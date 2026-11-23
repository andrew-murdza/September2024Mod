package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.TreeGrowthPlacementRules;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.SurfaceHabitatColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyBlockUnionTests {
    private static final List<String> BIOMES = List.of("snowy_forest", "dappled_forest", "cold_ocean",
            "mountain_lake", "mushroom_caves");

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void everySnowyBiomeAcceptsExactlyTheUnionOfItsOriginalBlockLists(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var position = new BlockPos(0, 100, 0);
        var generation = BIOMES.stream().map(name -> habitat(snowy, name, true)).toList();
        var runtime = BIOMES.stream().map(name -> habitat(snowy, name, false)).toList();
        int accepted = 0, rejected = 0;
        for (var block : ForgeRegistries.BLOCKS.getValues()) {
            var state = block.defaultBlockState();
            boolean union = generation.stream().anyMatch(world -> BiomePlacementRestrictions.isAllowed(world, position, state));
            for (int index = 0; index < runtime.size(); index++) {
                helper.assertTrue(BiomePlacementRestrictions.isAllowed(runtime.get(index), position, state) == union,
                        "The Snowy union differs in " + BIOMES.get(index) + " for " + ForgeRegistries.BLOCKS.getKey(block));
                if (union) helper.assertTrue(BiomePlacementRestrictions.placementFailureMessage(runtime.get(index), position, state).isEmpty(),
                        "Allowed blocks must not retain old biome-denial messages");
            }
            if (union) accepted++; else rejected++;
        }
        helper.assertTrue(accepted > 100 && rejected > 100, "The union must preserve a substantial allowed list and foreign-material exclusions");
        for (var world : runtime) {
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(world, position, Blocks.SPRUCE_SAPLING.defaultBlockState())
                            && BiomePlacementRestrictions.isAllowed(world, position, Blocks.MYCELIUM.defaultBlockState()),
                    "The two documented examples must be available in every Snowy biome");
            helper.assertFalse(BiomePlacementRestrictions.isAllowed(world, position, Blocks.MOSS_BLOCK.defaultBlockState())
                            || BiomePlacementRestrictions.isAllowed(world, position, Blocks.CRIMSON_NYLIUM.defaultBlockState()),
                    "Sharing Snowy habitats must not admit tropical moss or Nether nylium");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyNaturalFeaturesAndMobPoolsRemainBiomeScoped(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var position = new BlockPos(0, 100, 0);
        var cave = (WorldGenLevel) habitat(snowy, "mushroom_caves", true);
        var forest = (WorldGenLevel) habitat(snowy, "dappled_forest", true);
        var buffered = new GrowthPlacementBuffer(new GrowthPlacementBuffer(cave));
        helper.assertTrue(DocumentedPlacementRules.isNaturalGeneration(buffered), "Nested natural feature buffers must retain their source context");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(buffered, position, Blocks.SPRUCE_SAPLING.defaultBlockState())
                        || TreeGrowthPlacementRules.isAllowed(buffered, position, position.above(), Blocks.SPRUCE_LOG.defaultBlockState()),
                "Natural cave generation must not acquire surface spruce through the placement union");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(forest, position, Blocks.MYCELIUM.defaultBlockState()),
                "Natural dappled terrain must not acquire cave mycelium");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(cave, position, Blocks.MYCELIUM.defaultBlockState()),
                "Natural cave mycelium must remain available");
        var biomes = snowy.registryAccess().registryOrThrow(Registries.BIOME);
        var caveBiome = biomes.get(AOEMod.makeID("mushroom_caves"));
        var forestBiome = biomes.get(AOEMod.makeID("snowy_forest"));
        helper.assertTrue(caveBiome.getMobSettings().getMobs(ModMobCategories.MUSHROOM_CAVE).unwrap().stream()
                        .anyMatch(entry -> entry.type == EntityType.MOOSHROOM && entry.minCount == 2 && entry.maxCount == 4)
                        && caveBiome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                        .noneMatch(entry -> entry.type == EntityType.MOOSHROOM)
                        && Arrays.stream(MobCategory.values()).flatMap(category -> forestBiome.getMobSettings().getMobs(category).unwrap().stream())
                        .noneMatch(entry -> entry.type == EntityType.MOOSHROOM),
                "Cave mooshrooms must retain their independent spawn budget and remain absent from every forest mob pool");
        helper.assertFalse(caveBiome.getGenerationSettings().features().equals(forestBiome.getGenerationSettings().features()),
                "Forest and mushroom caves must retain distinct natural feature lists");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void placedMyceliumAndPlantedSpruceWorkOutsideTheirFormerSnowyHabitats(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var surface = new BlockPos(2384, 240, 2384);
        var origin = surface.atY(-24);
        var column = SurfaceHabitatColumns.data(snowy.getChunkAt(surface));
        byte[] originalHabitats = column.aoemod$getHabitats();
        helper.assertTrue(originalHabitats != null, "The loaded surface fixture must have its persisted habitat columns");
        byte[] fixtureHabitats = originalHabitats.clone();
        fixtureHabitats[(surface.getZ() & 15) * 16 + (surface.getX() & 15)] = (byte) SurfaceHabitatColumns.HABITATS.indexOf(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID("dappled_forest")));
        var originalSurface = snowy.getBlockState(surface);
        var originalAbove = snowy.getBlockState(surface.above());
        try {
            // /fillbiome does not override Snowy's saved surface-column classifier.
            // Set and restore the fixture's authoritative column, then use the real underground cave band.
            column.aoemod$setHabitats(fixtureHabitats);
            helper.assertTrue(GameplayDimensions.biomeIs(snowy, surface, "dappled_forest"),
                    "The live surface placement must use the dappled habitat column");
            snowy.setBlock(surface.above(), Blocks.AIR.defaultBlockState(), 18);
            helper.assertTrue(snowy.setBlock(surface, Blocks.MYCELIUM.defaultBlockState(), 18)
                            && snowy.getBlockState(surface).is(Blocks.MYCELIUM),
                    "Direct block placement must accept mycelium in dappled forest");
            helper.assertTrue(GameplayDimensions.biomeIs(snowy, origin, "mushroom_caves"),
                    "The planted spruce must use the real underground mushroom-cave band");
            for (var pos : BlockPos.betweenClosed(origin.offset(-6, -1, -6), origin.offset(6, 20, 6)))
                snowy.setBlock(pos, pos.getY() < origin.getY() ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
            var sapling = Blocks.SPRUCE_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
            helper.assertTrue(snowy.setBlock(origin, sapling, 18), "Direct placement must accept spruce saplings in mushroom caves");
            ((SaplingBlock) Blocks.SPRUCE_SAPLING).advanceTree(snowy, origin, sapling, RandomSource.create(31851));
            helper.assertTrue(snowy.getBlockState(origin).is(Blocks.SPRUCE_LOG),
                    "An allowed player-planted cave spruce must successfully grow its trunk");
            int leaves = 0;
            for (var pos : BlockPos.betweenClosed(origin.offset(-6, 0, -6), origin.offset(6, 20, 6)))
                if (snowy.getBlockState(pos).is(Blocks.SPRUCE_LEAVES)) leaves++;
            helper.assertTrue(leaves > 10, "The planted cave spruce must commit a complete crown");
        } finally {
            column.aoemod$setHabitats(originalHabitats);
            snowy.setBlock(surface, originalSurface, 18);
            snowy.setBlock(surface.above(), originalAbove, 18);
            for (var pos : BlockPos.betweenClosed(origin.offset(-6, -1, -6), origin.offset(6, 20, 6)))
                snowy.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        }
        helper.succeed();
    }

    private static ServerLevelAccessor habitat(ServerLevel backing, String biome, boolean generation) {
        var holder = backing.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(biome)));
        return (ServerLevelAccessor) Proxy.newProxyInstance(SnowyBlockUnionTests.class.getClassLoader(),
                new Class<?>[]{generation ? WorldGenLevel.class : ServerLevelAccessor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return backing;
                    if (method.getName().equals("getBiome")) return holder;
                    try { return method.invoke(backing, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
