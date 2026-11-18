package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalPresentationTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalWaterColorsUseTheirRequestedBiomePalettes(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var reef = biomes.get(new ResourceLocation("aoemod:coral_reef"));
        var vanillaMangrove = biomes.get(new ResourceLocation("minecraft:mangrove_swamp"));
        for (String name : new String[]{"rainforest", "mangrove_swamp", "cherry_grove"}) {
            var biome = biomes.get(new ResourceLocation("aoemod", name));
            int expected = name.equals("cherry_grove") ? reef.getWaterColor() : 3832426;
            helper.assertTrue(biome.getWaterColor() == expected, "Water color differs in " + name);
            int expectedWaterFog = name.equals("cherry_grove") ? reef.getWaterFogColor() : vanillaMangrove.getWaterFogColor();
            helper.assertTrue(biome.getWaterFogColor() == expectedWaterFog, "Water fog differs in " + name);
            helper.assertTrue(biome.getFogColor() == reef.getFogColor(), "Air fog differs in " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void torchflowersReplaceMossAndCoralAsLightSources(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        var pos = new BlockPos(anchor.getX(), 220, anchor.getZ());
        helper.assertTrue(Blocks.MOSS_BLOCK.defaultBlockState().getLightEmission(level, pos) == 0,
                "Moss must not emit block light");
        for (String family : new String[]{"minecraft:tube", "minecraft:brain", "minecraft:bubble", "minecraft:fire",
                "minecraft:horn", "upgrade_aquatic:star", "upgrade_aquatic:petal", "upgrade_aquatic:finger"}) {
            var coral = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(family + "_coral_block"));
            helper.assertTrue(coral != null && coral.defaultBlockState().getLightEmission(level, pos) == 0,
                    "Coral must not emit block light: " + family);
        }
        helper.assertTrue(Blocks.TORCHFLOWER.defaultBlockState().getLightEmission(level, pos) == 15,
                "Torchflower must emit full block light");
        helper.assertTrue(Blocks.POPPY.defaultBlockState().getLightEmission(level, pos) == 0,
                "Torchflower light must not alter neighboring flower registrations");
        var outside = helper.getLevel();
        helper.assertTrue(Blocks.MOSS_BLOCK.defaultBlockState().getLightEmission(outside, pos) == 0
                        && Blocks.BRAIN_CORAL_BLOCK.defaultBlockState().getLightEmission(outside, pos) == 0,
                "Solid blocks must also remain unlit outside tropical");
        helper.assertTrue(Blocks.TORCHFLOWER.defaultBlockState().getLightEmission(outside, pos) == 0,
                "Torchflower must retain its previous unlit behavior outside tropical");
        level.setBlock(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        level.setBlock(pos, Blocks.TORCHFLOWER.defaultBlockState(), 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getBrightness(LightLayer.BLOCK, pos.above()) >= 14,
                    "Torchflower emission did not propagate to its neighbor");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
        });
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void chunkLightSourceDiscoveryUsesTheChunksDimension(GameTestHelper helper) {
        for (var level : new net.minecraft.server.level.ServerLevel[]{helper.getLevel(),
                helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL)}) {
            var chunk = new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(0, 0),
                    net.minecraft.world.level.chunk.UpgradeData.EMPTY, level,
                    level.registryAccess().registryOrThrow(Registries.BIOME), null);
            BlockPos flower = new BlockPos(8, 220, 8), moss = flower.east(2), coral = flower.east(4);
            chunk.setBlockState(flower, Blocks.TORCHFLOWER.defaultBlockState(), false);
            chunk.setBlockState(moss, Blocks.MOSS_BLOCK.defaultBlockState(), false);
            chunk.setBlockState(coral, Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(), false);
            java.util.Set<BlockPos> sources = new java.util.HashSet<>();
            chunk.findBlockLightSources((pos, state) -> sources.add(pos.immutable()));
            helper.assertTrue(sources.equals(GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                            ? java.util.Set.of(flower) : java.util.Set.of()),
                    "Chunk palette scans must retain only the actual light sources for their own dimension: " + sources);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void solidSoilsAndCoralsBlockSunlightWhileLeavesAndGlowingPlantsKeepTheirRules(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var level : new net.minecraft.server.level.ServerLevel[]{helper.getLevel(),
                helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL)}) {
            for (String id : new String[]{"minecraft:moss_block", "farmersdelight:rich_soil",
                    "farmersdelight:rich_soil_farmland", "minecraft:brain_coral_block",
                    "minecraft:dead_brain_coral_block", "upgrade_aquatic:star_coral_block"}) {
                var state = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).defaultBlockState();
                helper.assertTrue(state.getLightEmission() == 0 && state.getLightEmission(level, pos) == 0,
                        "No static or contextual glow may remain on " + id);
                // Farmland is 15/16 high. Vanilla blocks its downward skylight
                // with the full bottom face rather than the full-cube opacity hook.
                boolean opaque = !state.propagatesSkylightDown(level, pos) && state.getLightBlock(level, pos) == 15;
                boolean shaped = state.useShapeForLightOcclusion() && net.minecraft.world.level.block.Block.isShapeFullBlock(
                        state.getFaceOcclusionShape(level, pos, net.minecraft.core.Direction.DOWN));
                helper.assertTrue(opaque || shaped, "Solid soil/coral must obstruct downward sunlight: " + id);
            }
            helper.assertTrue(Blocks.OAK_LEAVES.defaultBlockState().getLightBlock(level, pos) == 0,
                    "Leaves retain their transparent sunlight rule");
            helper.assertTrue(Blocks.CAVE_VINES.defaultBlockState().setValue(
                    net.minecraft.world.level.block.CaveVines.BERRIES, true).getLightEmission(level, pos) == 15,
                    "Glow-berry vines retain full light emission");
        }
        helper.succeed();
    }
}
