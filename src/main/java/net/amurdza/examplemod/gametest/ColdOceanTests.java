package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.TreeGrowthPlacementRules;
import net.amurdza.examplemod.worldgen.DimensionBiomeSource;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ColdOceanTests {
    private static final com.google.gson.JsonPrimitive SNOWY_EMERALD_BONUS =
            new com.google.gson.JsonPrimitive("aoemod:dimension_ores/snowy/emerald_stone_bonus");

    @GameTest(template = "plant_gameplay_empty")
    public static void coldOceanNamesNewBasinsAndPreservesLegacyGenerators(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var forest = registry.getHolderOrThrow(key("snowy_forest"));
        var caves = registry.getHolderOrThrow(key("mushroom_caves"));
        var lake = registry.getHolderOrThrow(key("cold_ocean"));
        var dappled = registry.getHolderOrThrow(key("dappled_forest"));
        var source = new DimensionBiomeSource("snowy", List.of(forest, caves, lake, dappled));
        var water = sampler(0.05);
        helper.assertTrue(source.getNoiseBiome(0, 24, 0, water).equals(lake), "Low absolute weirdness must select canonical Cold Ocean");
        helper.assertTrue(source.getNoiseBiome(0, 0, 0, water).equals(lake), "Lake water and floor must share the surface habitat");
        helper.assertTrue(source.getNoiseBiome(0, -1, 0, water).equals(caves)
                        && source.getNoiseBiome(0, -8, 0, water).equals(caves)
                        && source.getNoiseBiome(0, -9, 0, water).equals(forest),
                "Renaming the lake must not change the underground biome bands");
        for (int sign : new int[]{-1, 1}) {
            helper.assertTrue(source.getNoiseBiome(0, 24, 0, sampler(sign * 0.6)).equals(forest), "High weirdness retains snowy taiga");
            helper.assertTrue(source.getNoiseBiome(0, 24, 0, sampler(sign * 0.35)).equals(dappled), "Intermediate weirdness selects dappled forest");
            helper.assertTrue(source.getNoiseBiome(0, 24, 0, sampler(sign * 0.4)).equals(dappled),
                    "The exact absolute weirdness boundary 0.4 still belongs to dappled forest");
            helper.assertTrue(source.getNoiseBiome(0, 24, 0, sampler(sign * 0.401)).equals(forest),
                    "Snowy taiga starts strictly above absolute weirdness 0.4");
        }
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,
                helper.getLevel().registryAccess());
        var saved = GsonHelper.parse("{\"dimension\":\"snowy\",\"biomes\":[\"aoemod:snowy_forest\",\"aoemod:mushroom_caves\",\"aoemod:mountain_lake\",\"aoemod:dappled_forest\"]}");
        var upgraded = DimensionBiomeSource.CODEC.parse(ops, saved).result().orElseThrow();
        helper.assertTrue(upgraded.getNoiseBiome(0, 24, 0, water).equals(lake),
                "Decoding saved Mountain Lake generators must select Cold Ocean for new chunks");
        helper.assertTrue(registry.containsKey(new ResourceLocation("aoemod", "mountain_lake")),
                "The legacy registry alias must remain available for already-saved chunks");
        var encoded = DimensionBiomeSource.CODEC.encodeStart(ops, upgraded).result().orElseThrow();
        helper.assertTrue(encoded.getAsJsonObject().getAsJsonArray("biomes").size() == 4,
                "The upgraded biome list must survive saving again");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void lakeClimateAndDappledColorsRetainAuthoredFaunaAndOres(GameTestHelper helper) throws Exception {
        try (var forest = new InputStreamReader(ColdOceanTests.class.getResourceAsStream(
                "/data/aoemod/worldgen/biome/snowy_forest.json"), StandardCharsets.UTF_8);
             var ocean = new InputStreamReader(ColdOceanTests.class.getResourceAsStream(
                     "/data/aoemod/worldgen/biome/cold_ocean.json"), StandardCharsets.UTF_8);
             var lake = new InputStreamReader(ColdOceanTests.class.getResourceAsStream(
                     "/data/aoemod/worldgen/biome/mountain_lake.json"), StandardCharsets.UTF_8)) {
            var forestData = GsonHelper.parse(forest);
            var oceanData = GsonHelper.parse(ocean);
            var lakeData = GsonHelper.parse(lake);
            for (String field : List.of("downfall", "has_precipitation")) {
                helper.assertTrue(forestData.get(field).equals(oceanData.get(field)),
                        "Cold ocean retains its authored precipitation amount, colors and music: " + field);
            }
            helper.assertTrue(oceanData.get("temperature").getAsFloat() == 0.5F,
                    "The lake must stay warm enough for rain throughout the dimension height");
            var forestOres = forestData.getAsJsonArray("features").get(6).getAsJsonArray();
            helper.assertTrue(forestOres.contains(SNOWY_EMERALD_BONUS), "The current forest must include its emerald bonus");
            helper.assertTrue(oceanData.equals(lakeData),
                    "Canonical Cold Ocean and the Mountain Lake save alias must preserve identical current content");
            helper.assertTrue(forestOres.equals(lakeData.getAsJsonArray("features").get(6)),
                    "Mountain Lake must retain the complete current snowy ore pass, including the emerald bonus");
        }
        Set<DocumentedFauna.Species> expected = new HashSet<>();
        for (var species : DocumentedFauna.forBiome("snowy_forest")) {
            if (Set.of("minecraft:goat", "biodiverse:reindeer", "minecraft:cow").contains(species.id().toString())) continue;
            expected.add(species);
        }
        helper.assertTrue(expected.equals(new HashSet<>(DocumentedFauna.forBiome("cold_ocean"))),
                "Ocean fauna uses the same requested aquatic roster while honoring exclusive land-animal habitats");
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var forest = registry.get(key("snowy_forest"));
        var ocean = registry.get(key("cold_ocean"));
        var lake = registry.get(key("mountain_lake"));
        var dappled = registry.get(key("dappled_forest"));
        helper.assertTrue(dappled.getWaterColor() == ocean.getWaterColor()
                        && dappled.getWaterFogColor() == ocean.getWaterFogColor()
                        && dappled.getFogColor() == ocean.getFogColor(),
                "Dappled water, underwater fog and atmospheric fog must match Cold Ocean");
        for (var basin : List.of(ocean, lake)) {
            helper.assertFalse(basin.getGenerationSettings().getCarvers(
                            net.minecraft.world.level.levelgen.GenerationStep.Carving.AIR).iterator().hasNext(),
                    "Lake biomes must not originate carver tunnels");
            for (int y : new int[]{-96, -32, 0, 63, 160, 319}) {
                for (int coordinate : new int[]{-8191, -1, 0, 4097}) {
                    BlockPos pos = new BlockPos(coordinate, y, -coordinate);
                    helper.assertTrue(basin.getPrecipitationAt(pos) == Biome.Precipitation.RAIN
                                    && !basin.shouldSnow(helper.getLevel(), pos)
                                    && !basin.shouldFreeze(helper.getLevel(), pos, false),
                            "Both lake IDs must rain without snow or ice, including high terrain: " + pos);
                }
            }
        }
        var forestOres = features(helper, forest).get(6);
        helper.assertTrue(forestOres.equals(features(helper, ocean).get(6)),
                "Canonical Cold Ocean must preserve the complete current ore passes, including the emerald bonus");
        helper.assertTrue(forestOres.equals(features(helper, lake).get(6)),
                "The final Mountain Lake ore pass must match the current forest, including the emerald bonus");
        helper.assertTrue(features(helper, ocean).get(9).equals(List.of(new com.google.gson.JsonPrimitive("aoemod:documented_flora"))),
                "Cold ocean must retain its aquatic flora without adding land trees or berry patches");
        Set<String> expectedSpawns = new HashSet<>();
        for (String spawn : spawns(forest)) {
            // The newer hostile rosters are forest-only; both lake aliases are quiet.
            if (spawn.startsWith(MobCategory.MONSTER + ":")) continue;
            if (spawn.contains(":minecraft:goat:") || spawn.contains(":biodiverse:reindeer:") || spawn.contains(":minecraft:cow:")) continue;
            expectedSpawns.add(spawn);
        }
        helper.assertTrue(expectedSpawns.equals(spawns(ocean)) && expectedSpawns.equals(spawns(lake)),
                "Both lake spawn tables must preserve the retained fauna, counts and weights without forest monsters");
        for (String biome : List.of("coral_reef")) helper.assertTrue(
                DocumentedFauna.forBiome(biome).stream().anyMatch(species -> species.id().toString().equals("aoemod:nautilus")),
                "Nautilus must remain in " + biome);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void dryLakeBanksNeverReceiveForcedWorldgenSnow(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        for (String biome : List.of("cold_ocean", "mountain_lake")) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = floraWorld(helper, biome, blocks, 0, 42);
            new DocumentedFloraFeature().place(new FeaturePlaceContext<>(Optional.empty(), world,
                    level.getChunkSource().getGenerator(), RandomSource.create(42), BlockPos.ZERO,
                    NoneFeatureConfiguration.INSTANCE));
            helper.assertTrue(blocks.values().stream().noneMatch(state -> state.is(Blocks.SNOW)
                            || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE)
                            || state.is(snownee.snow.CoreModule.TILE_BLOCK.get())),
                    "The complete flora pass must leave dry lake banks snow-free: " + biome);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void coldOceanKeepsAquaticFloraWithoutForestPlants(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        int seaLevel = level.getChunkSource().getGenerator().getSeaLevel();
        BlockPos root = new BlockPos(8, seaLevel, 8);
        for (int depth : new int[]{1, 8, 32}) for (int seed = 0; seed < 4; seed++) {
            Map<BlockPos, BlockState> forestBlocks = new HashMap<>();
            Map<BlockPos, BlockState> oceanBlocks = new HashMap<>();
            var forest = floraWorld(helper, "snowy_forest", forestBlocks, depth, seed);
            var ocean = floraWorld(helper, "cold_ocean", oceanBlocks, depth, seed);
            var feature = new DocumentedFloraFeature();
            feature.place(new FeaturePlaceContext<>(Optional.empty(), forest, level.getChunkSource().getGenerator(),
                    RandomSource.create(seed), BlockPos.ZERO, NoneFeatureConfiguration.INSTANCE));
            feature.place(new FeaturePlaceContext<>(Optional.empty(), ocean, level.getChunkSource().getGenerator(),
                    RandomSource.create(seed), BlockPos.ZERO, NoneFeatureConfiguration.INSTANCE));
            helper.assertTrue(!forestBlocks.isEmpty() && forestBlocks.equals(oceanBlocks),
                    "Forest lakes and legacy Cold Ocean must use identical depth-sensitive aquatic placement for seed " + seed + ", depth " + depth);
            int roots = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                BlockState state = ocean.getBlockState(new BlockPos(x, seaLevel - depth, z));
                if (depth >= 25) {
                    String id = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                    helper.assertTrue(state.is(Blocks.WATER) || Set.of("alexscaves:ping_pong_sponge", "betterend:charnia_light_blue", "betterend:charnia_cyan",
                                    "alexscaves:bone_worms").contains(id),
                            "Deep beds retain their sparse original plant palette: " + state);
                    if (!state.is(Blocks.WATER)) roots++;
                } else {
                    helper.assertTrue(state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)
                                || state.is(Blocks.SEA_PICKLE) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)
                                || depth > 1 && state.is(Blocks.WET_SPONGE),
                        "Every exposed ocean floor column must receive its aquatic palette: " + state);
                    roots++;
                }
            }
            helper.assertTrue((depth >= 25 ? roots > 0 && roots <= 40 : roots == 256) && oceanBlocks.keySet().stream().allMatch(pos ->
                            pos.getY() >= seaLevel - depth && pos.getY() < seaLevel),
                    "Aquatic decoration must preserve the bed and stay below the existing water surface");
        }
        var dryOcean = floraWorld(helper, "cold_ocean", new HashMap<>(), 0, 42);
        for (String species : List.of("minecraft:sweet_berry_bush", "aoemod:blue_berry_bush")) {
            helper.assertTrue(net.amurdza.examplemod.worldgen.SnowyVegetation.placeBerryPatch(dryOcean,
                            new net.amurdza.examplemod.worldgen.SnowyVegetation.BerryPatch(2, 2, species, 42)) == 0,
                    "Snowy taiga berry patches must not grow in cold ocean, even on a dry edge");
        }
        var poplar = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:poplar_sapling"));
        var leaves = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:orange_poplar_leaves"));
        helper.assertFalse(DocumentedPlacementRules.isBiomeAllowed(dryOcean, root, poplar.defaultBlockState(), false),
                "Poplar planting belongs exclusively to dappled forest");
        helper.assertFalse(TreeGrowthPlacementRules.isAllowed(dryOcean, root, root.above(3), leaves.defaultBlockState()),
                "Poplar tree growth must not leak into cold ocean");
        helper.succeed();
    }

    private static WorldGenLevel floraWorld(GameTestHelper helper, String biome, Map<BlockPos, BlockState> blocks, int depth, long seed) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        int seaLevel = level.getChunkSource().getGenerator().getSeaLevel();
        int floor = seaLevel - depth - 1;
        var holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key(biome));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= floor ? (depth == 0 ? Blocks.GRASS_BLOCK : Blocks.MUD).defaultBlockState()
                        : pos.getY() < seaLevel ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(ColdOceanTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBiome": return holder;
                        case "getSeed": return seed;
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : seaLevel;
                        case "getHeightmapPos": return new BlockPos(((BlockPos) args[1]).getX(), seaLevel, ((BlockPos) args[1]).getZ());
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }

    private static Set<String> spawns(Biome biome) {
        Set<String> result = new HashSet<>();
        for (MobCategory category : MobCategory.values()) for (var entry : biome.getMobSettings().getMobs(category).unwrap()) {
            result.add(category + ":" + ForgeRegistries.ENTITY_TYPES.getKey(entry.type) + ":" + entry.getWeight().asInt()
                    + ":" + entry.minCount + ":" + entry.maxCount);
        }
        return result;
    }

    private static List<List<com.google.gson.JsonElement>> features(GameTestHelper helper, Biome biome) {
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,
                helper.getLevel().registryAccess());
        return biome.getGenerationSettings().features().stream().map(step -> step.stream()
                .map(feature -> feature.unwrapKey().<com.google.gson.JsonElement>map(
                        key -> new com.google.gson.JsonPrimitive(key.location().toString())).orElseGet(() -> {
                    // Dependencies can add inline holders; compare their entire configured
                    // feature and placement modifiers instead of requiring a registry key.
                    var encoded = net.minecraft.world.level.levelgen.placement.PlacedFeature.DIRECT_CODEC
                            .encodeStart(ops, feature.value());
                    return encoded.result().orElseThrow(() -> new IllegalStateException(
                            "Cannot compare inline placed feature: " + encoded.error().orElseThrow().message()));
                })).toList()).toList();
    }

    private static Climate.Sampler sampler(double weirdness) {
        var zero = DensityFunctions.zero();
        return new Climate.Sampler(zero, zero, DensityFunctions.constant(0.4), DensityFunctions.constant(-0.15),
                zero, DensityFunctions.constant(weirdness), List.of());
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", path));
    }
}
