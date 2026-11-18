package net.amurdza.examplemod.gametest;

import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.worldgen.DeepDarkVegetation;
import net.amurdza.examplemod.worldgen.feature.features.DeepDarkVeinFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeepDarkVegetationTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void chorusTreesHaveFlowersAndSurviveOnCatalysts(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel level = fixture(helper, blocks, false);
        BlockPos root = new BlockPos(8, -31, 8);
        helper.assertTrue(DeepDarkVegetation.placeTree(level, root, RandomSource.create(7601)), "A cave with room must grow a chorus tree");
        helper.assertTrue(level.getBlockState(root.below()).is(Blocks.SCULK_CATALYST), "The tree's sculk support must become a catalyst");
        helper.assertTrue(level.getBlockState(root).is(Blocks.CHORUS_PLANT)
                && level.getBlockState(root).canSurvive(level, root), "The chorus trunk must survive on its catalyst");
        int flowers = 0, stems = 0;
        for (var entry : blocks.entrySet()) {
            if (entry.getValue().is(Blocks.CHORUS_FLOWER)) {
                flowers++;
                helper.assertTrue(entry.getValue().canSurvive(level, entry.getKey()), "Every generated flower must survive on its tree");
            }
            if (entry.getValue().is(Blocks.CHORUS_PLANT)) stems++;
        }
        helper.assertTrue(flowers > 0 && stems >= 3, "Generation must produce a real stemmed tree with flowers");
        blocks.clear();
        blocks.put(root.above(3), Blocks.STONE.defaultBlockState());
        helper.assertFalse(DeepDarkVegetation.placeTree(level, root, RandomSource.create(7601)), "A cramped cave must reject the tree");
        helper.assertTrue(blocks.size() == 1 && level.getBlockState(root.below()).is(Blocks.SCULK),
                "A failed tree must not leave a catalyst or standalone flower");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void oneChorusColumnPerChunkRestoresTheHistoricalBudget(GameTestHelper helper) {
        for (int seed = 0; seed < 16; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel level = fixture(helper, blocks, false);
            DeepDarkVegetation.place(new FeaturePlaceContext<>(Optional.empty(), level,
                    level.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed),
                    BlockPos.ZERO, NoneFeatureConfiguration.INSTANCE));
            long catalysts = blocks.values().stream().filter(state -> state.is(Blocks.SCULK_CATALYST)).count();
            helper.assertTrue(catalysts == 1, "One open cave layer must receive exactly one chorus opportunity per chunk");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalChorusTreeKeepsFlowersAboveTheBiomeBoundary(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel level = fixture(helper, blocks, false, -3, 25, true);
        BlockPos root = new BlockPos(8, -2, 8);
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(level, root.above(5), Blocks.CHORUS_FLOWER.defaultBlockState()),
                "Fixture must enforce the real direct-placement restriction outside deep dark");
        helper.assertTrue(DeepDarkVegetation.placeTree(level, root, RandomSource.create(7601)),
                "A naturally generated chorus tree must grow across the biome boundary as a complete tree");
        helper.assertTrue(blocks.entrySet().stream().anyMatch(entry -> entry.getKey().getY() >= 0
                        && entry.getValue().is(Blocks.CHORUS_FLOWER)), "Upper flowers must not be silently rejected during worldgen");
        helper.assertTrue(level.getBlockState(root.below()).is(Blocks.SCULK_CATALYST), "The complete tree must retain its catalyst root");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sensorAndShriekerPatchesStaySeparate(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel level = fixture(helper, blocks, false);
        BlockPos anchor = new BlockPos(0, -31, 0);
        int sensors = DeepDarkVegetation.placePatch(level, anchor, Blocks.SCULK_SENSOR, RandomSource.create(19));
        helper.assertTrue(sensors == 5 && blocks.values().stream().allMatch(state -> state.is(Blocks.SCULK_SENSOR)),
                "A sensor patch must contain exactly five sensors");
        helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getX() >= -4 && pos.getX() <= 5
                        && pos.getZ() >= -4 && pos.getZ() <= 5), "Every sensor must stay inside the ten-square footprint");
        helper.assertTrue(DeepDarkVegetation.placePatch(level, anchor.offset(2, 0, 0), Blocks.SCULK_SHRIEKER,
                RandomSource.create(21)) == 0, "Shriekers must not merge into a neighbouring sensor patch");
        int shriekers = DeepDarkVegetation.placePatch(level, anchor.offset(12, 0, 0), Blocks.SCULK_SHRIEKER, RandomSource.create(21));
        helper.assertTrue(shriekers == 5, "A separated shrieker patch must generate exactly five roots");
        helper.assertTrue(blocks.values().stream().filter(state -> state.is(Blocks.SCULK_SHRIEKER))
                        .allMatch(state -> state.getValue(SculkShriekerBlock.CAN_SUMMON)),
                "Naturally generated shriekers must preserve vanilla warden summoning");
        RandomSource random = RandomSource.create(92038);
        int selected = 0;
        for (int i = 0; i < 20000; i++) if (DeepDarkVegetation.patchBlock(random) == Blocks.SCULK_SHRIEKER) selected++;
        helper.assertTrue(selected >= 2800 && selected <= 3200, "The whole-patch shrieker share must be 15%: " + selected);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void sculkPatchPassRunsInOneTwentiethOfChunks(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel level = fixture(helper, blocks, false);
        int selected = 0;
        for (int seed = 0; seed < 1000; seed++) {
            blocks.clear();
            DeepDarkVegetation.place(new FeaturePlaceContext<>(Optional.empty(), level,
                    level.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed),
                    new BlockPos((seed % 50) * 16, 0, (seed / 50) * 16), NoneFeatureConfiguration.INSTANCE));
            long sculkPlants = blocks.values().stream().filter(state -> state.is(Blocks.SCULK_SENSOR)
                    || state.is(Blocks.SCULK_SHRIEKER)).count();
            helper.assertTrue(sculkPlants == 0 || sculkPlants == 5, "A chunk can receive one complete five-plant patch");
            if (sculkPlants == 5) selected++;
        }
        helper.assertTrue(selected == 150,
                "The actual vegetation pass must produce exactly one hundred fifty evenly spaced patches per thousand chunks: " + selected);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void scheduledPatchesHaveBoundedLongitudinalGaps(GameTestHelper helper) {
        for (long seed : new long[]{0, 12345, -99118}) for (int row = -8; row <= 8; row++) {
            BlockPos previous = null;
            for (int tile = -100; tile <= 100; tile++) for (int patch = 0; patch < 3; patch++) {
                BlockPos anchor = DeepDarkVegetation.patchAnchor(seed, tile, row, patch);
                helper.assertTrue(Math.floorDiv(anchor.getX(), 80) == tile && Math.floorDiv(anchor.getZ(), 64) == row,
                        "Every five-by-four chunk tile owns three distinct patches");
                if (previous != null) helper.assertTrue(anchor.getX() - previous.getX() >= 16
                                && anchor.getX() - previous.getX() <= 32,
                        "Longitudinal spacing must remain bounded instead of allowing unbounded random gaps");
                previous = anchor;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void veinsKeepTheirFrequencyWithFivefoldConnectedPatches(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var veins = registry.get(new ResourceLocation("aoemod:deep_dark/sculk_vein"));
        var lichen = registry.get(new ResourceLocation("minecraft:glow_lichen"));
        var config = (MultifaceGrowthConfiguration) veins.feature().value().config();
        helper.assertTrue(veins.feature().value().feature() instanceof DeepDarkVeinFeature, "Veins must use the actual larger-patch feature");
        helper.assertTrue(veins.placement().size() == lichen.placement().size() + 1,
                "Veins must repeat the complete vanilla placement chain three times");
        var multiplier = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, veins.placement().get(0)).result().orElseThrow().getAsJsonObject();
        helper.assertTrue(multiplier.get("type").getAsString().equals("minecraft:count") && multiplier.get("count").getAsInt() == 3,
                "A separate count-three modifier must triple opportunities without exceeding CountPlacement's 256 limit");
        for (int i = 0; i < lichen.placement().size(); i++) {
            var actual = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, veins.placement().get(i + 1)).result().orElseThrow();
            var vanilla = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, lichen.placement().get(i)).result().orElseThrow();
            helper.assertTrue(actual.equals(vanilla), "Every placement modifier after the multiplier must match vanilla lichen");
        }
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, true);
        for (int seed = 0; seed < 32; seed++) {
            blocks.clear();
            int count = DeepDarkVeinFeature.placePatch(world, new BlockPos(0, -20, 0), config,
                    RandomSource.create(seed * 7919L), List.of(Direction.WEST));
            helper.assertTrue(count == 10 || count == 20, "Patches must have five times the previous two/four-block footprint");
            helper.assertTrue(blocks.size() == count, "Patch size must count distinct blocks");
            for (var entry : blocks.entrySet()) {
                helper.assertTrue(entry.getValue().is(Blocks.SCULK_VEIN)
                        && !entry.getValue().getValue(MultifaceBlock.getFaceProperty(Direction.DOWN)), "Veins must attach only to walls or ceilings");
                helper.assertTrue(blocks.keySet().stream().anyMatch(other -> other.distManhattan(entry.getKey()) == 1),
                        "Every vein block must touch the rest of its patch");
            }
        }
        var biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:deep_dark"));
        var vegetation = biome.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal());
        int floraIndex = -1, veinIndex = -1, veinCount = 0;
        for (int i = 0; i < vegetation.size(); i++) {
            String name = vegetation.get(i).unwrapKey().map(key -> key.location().toString()).orElse("");
            if (name.equals("aoemod:documented_flora")) floraIndex = i;
            if (vegetation.get(i).value() == veins) { veinIndex = i; veinCount++; }
        }
        helper.assertTrue(veinCount == 1 && floraIndex >= 0 && veinIndex > floraIndex,
                "The registered vein feature must remain exactly once after the cave-coating flora pass");
        for (var step : biome.getGenerationSettings().features()) for (var feature : step) {
            String name = feature.unwrapKey().map(key -> key.location().toString()).orElse("");
            helper.assertFalse(name.contains("sculk_patch") || name.contains("glow_lichen") || name.equals("minecraft:sculk_vein"),
                    "Legacy wall-sculk or lichen decorator remains: " + name);
        }
        helper.assertTrue(biome.getGenerationSettings().features().stream().flatMap(holders -> holders.stream())
                        .noneMatch(holder -> holder.value() == lichen), "Glow lichen must not generate in deep dark");
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, boolean wall) {
        return fixture(helper, blocks, wall, -32, -4, false);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, boolean wall,
                                         int floorY, int roofY, boolean biomeBoundary) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var biome = desert.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:deep_dark")));
        var upperBiome = desert.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:desert")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                wall && pos.getX() == -1 ? Blocks.STONE.defaultBlockState()
                        : pos.getY() == floorY ? Blocks.SCULK.defaultBlockState()
                        : pos.getY() < floorY || pos.getY() >= roofY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        Map<Long, ProtoChunk> chunks = new HashMap<>();
        java.util.function.BiFunction<Integer, Integer, ProtoChunk> chunkAt = (x, z) -> chunks.computeIfAbsent(ChunkPos.asLong(x, z), ignored ->
                new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, desert, desert.registryAccess().registryOrThrow(Registries.BIOME), null) {
                    @Override public BlockState getBlockState(BlockPos pos) { return stateAt.apply(pos); }
                    @Override public BlockState setBlockState(BlockPos pos, BlockState next, boolean moving) {
                        BlockState previous = stateAt.apply(pos); blocks.put(pos.immutable(), next); return previous;
                    }
                });
        return (WorldGenLevel) Proxy.newProxyInstance(DeepDarkVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return desert;
                        case "getBiome": return biomeBoundary && ((BlockPos) args[0]).getY() >= 0 ? upperBiome : biome;
                        case "getMinBuildHeight": return -64;
                        case "getMaxBuildHeight": return 64;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "levelEvent", "gameEvent", "scheduleTick": return null;
                        case "getBlockTicks", "getFluidTicks": return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList();
                        case "getChunk": {
                            if (args[0] instanceof BlockPos pos) return chunkAt.apply(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
                            return chunkAt.apply((Integer) args[0], (Integer) args[1]);
                        }
                        case "setBlock":
                            if (!BiomePlacementRestrictions.isAllowed((WorldGenLevel) proxy, (BlockPos) args[0], (BlockState) args[1])) return false;
                            blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            return true;
                        default:
                            try { return method.invoke(desert, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
