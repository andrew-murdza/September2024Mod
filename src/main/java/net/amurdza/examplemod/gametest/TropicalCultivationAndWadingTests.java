package net.amurdza.examplemod.gametest;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.event_handlers.CustomBlockDrops;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalCultivationAndWadingTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void cultivatedTropicalCornRequiresFarmlandThroughEverySection(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos pos = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 60000, 220, 60000);
        var corn = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hearthandharvest:corn_stalk"));
        for (int y = -1; y <= 5; y++) level.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 18);
        var outside = helper.getLevel();
        var outsidePos = helper.absolutePos(new BlockPos(2, 3, 2));
        outside.setBlock(outsidePos.below(), Blocks.DIRT.defaultBlockState(), 18);
        outside.setBlock(outsidePos, Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(pos.east(3), Blocks.GLOWSTONE.defaultBlockState(), 2);
        outside.setBlock(outsidePos.east(3), Blocks.GLOWSTONE.defaultBlockState(), 2);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                CornStalkBlock.hasSufficientLight(level, pos) && CornStalkBlock.hasSufficientLight(outside, outsidePos),
                "The native corn light requirement needs propagated fixture lighting")).thenExecute(() -> {
            var bottom = corn.defaultBlockState().setValue(CornStalkBlock.AGE, 3)
                    .setValue(CornStalkBlock.SECTION, CornStalkBlock.CornSection.BOTTOM);
            for (var soil : new net.minecraft.world.level.block.Block[]{Blocks.MOSS_BLOCK, Blocks.DIRT,
                    Blocks.GRASS_BLOCK, Blocks.FARMLAND,
                    vectorwing.farmersdelight.common.registry.ModBlocks.RICH_SOIL_FARMLAND.get()}) {
                // The soil predicate is independent of the older dimension-origin
                // restrictions: vanilla farmland's original dimension restrictions stay unchanged.
                level.getChunkAt(pos).setBlockState(pos.below(), soil.defaultBlockState(), false);
                boolean cultivated = soil instanceof net.minecraft.world.level.block.FarmBlock;
                helper.assertTrue(bottom.canSurvive(level, pos) == cultivated, "Incorrect corn soil: " + soil);
                level.setBlock(pos, bottom, 18);
                var middle = bottom.setValue(CornStalkBlock.SECTION, CornStalkBlock.CornSection.MIDDLE);
                level.setBlock(pos.above(), middle, 18);
                var top = bottom.setValue(CornStalkBlock.SECTION, CornStalkBlock.CornSection.TOP);
                helper.assertTrue(middle.canSurvive(level, pos.above()) == cultivated
                                && top.canSurvive(level, pos.above(2)) == cultivated,
                        "Upper corn sections must retain the same cultivated root requirement");
            }
            helper.assertFalse(bottom.canSurvive(outside, outsidePos), "Corn outside tropical must also require farmland");
        }).thenSucceed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalGrassProvidesCornKernelsWithoutChangingShearsOrOtherDimensions(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var kernels = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hearthandharvest:corn_kernels"));
        var pos = helper.absolutePos(BlockPos.ZERO);
        int total = 0;
        var random = RandomSource.create(715913);
        for (int i = 0; i < 4096; i++) {
            var drops = CustomBlockDrops.configuredDrops(level, pos, Blocks.GRASS.defaultBlockState(), ItemStack.EMPTY, random);
            total += drops.stream().filter(s -> s.is(kernels)).mapToInt(ItemStack::getCount).sum();
        }
        helper.assertTrue(total >= 150 && total <= 260, "Corn-kernel seed yield is not near five percent: " + total);
        helper.assertTrue(CustomBlockDrops.configuredDrops(level, pos, Blocks.GRASS.defaultBlockState(),
                new ItemStack(Items.SHEARS), random) == null, "Shears must still harvest the grass itself");
        for (var outside : new net.minecraft.server.level.ServerLevel[]{helper.getLevel(),
                helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS)}) {
            helper.assertTrue(BlockConfig.blockDropEntries(outside, pos, Blocks.GRASS.defaultBlockState()).stream()
                    .noneMatch(e -> e.item() == kernels), "Corn-kernel seeds leaked outside tropical");
        }
        helper.assertTrue(BlockConfig.blockDropEntries(level, pos, Blocks.FERN.defaultBlockState()).stream()
                .anyMatch(e -> e.item() == kernels), "Tropical ferns must also supply corn kernels");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mangroveLandFaunaCanStandOnLandOrWadeInExactlyOneWaterBlock(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var chunk = level.getChunk(4090 + Math.floorMod(helper.absolutePos(BlockPos.ZERO).getX(), 19), 4090);
        Holder<Biome> mangrove = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:mangrove_swamp")));
        var region = new WorldGenRegion(level, List.of(chunk), ChunkStatus.SPAWN, -1) {
            @Override public Holder<Biome> getBiome(BlockPos pos) { return mangrove; }
        };
        BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + 8, 220, chunk.getPos().getMinBlockZ() + 8);
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -1; y <= 6; y++) {
            chunk.setBlockState(pos.offset(x, y, z), (y == -1 ? Blocks.MOSS_BLOCK : Blocks.AIR).defaultBlockState(), false);
        }
        var waders = DocumentedFauna.forBiome("mangrove_swamp").stream()
                .filter(s -> s.habitat() == DocumentedFauna.Habitat.LAND).toList();
        for (var species : waders) helper.assertTrue(DocumentedFauna.hasSpace(region, pos, species), "Dry mangrove soil rejected");
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            chunk.setBlockState(pos.offset(x, 0, z), Blocks.WATER.defaultBlockState(), false);
        }
        for (var species : waders) {
            helper.assertTrue(DocumentedFauna.hasSpace(region, pos, species), "One-deep water rejected: " + species.id());
            helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, region, pos.above(), species.type()),
                    "Heightmap surface candidate must reach the shallow bed: " + species.id());
            Mob mob = (Mob) species.type().create(level);
            mob.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 0, 0);
            var event = new MobSpawnEvent.PositionCheck(mob, region, MobSpawnType.CHUNK_GENERATION, null);
            DocumentedNaturalSpawns.position(event);
            helper.assertTrue(event.getResult() == Event.Result.ALLOW && mob.getY() == pos.getY(),
                    "The actual mob must spawn down on the bed rather than float above the water: " + species.id());
            mob.discard();
        }
        chunk.setBlockState(pos.above(), Blocks.WATER.defaultBlockState(), false);
        for (var species : waders) helper.assertFalse(DocumentedFauna.hasSpace(region, pos, species), "Two-deep water accepted");
        var outside = helper.getLevel();
        outside.setBlock(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 18);
        outside.setBlock(pos, Blocks.WATER.defaultBlockState(), 18);
        helper.assertFalse(DocumentedFauna.hasSpace(outside, pos, waders.get(0)), "Shallow-water habitat leaked outside tropical");
        helper.succeed();
    }
}
