package net.amurdza.examplemod.gametest;

import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaSeafloor;
import net.amurdza.examplemod.worldgen.feature.configs.RandomSelectionFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaAquaticDensityTests {
    private static final int SAMPLE_SIDE = 128;

    private SavannaAquaticDensityTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void loadedSavannaWeightsQuarterSpongesAndHalvePickles(GameTestHelper helper) {
        var deep = selectorWeights(helper, "water_plants_full");
        var shallow = selectorWeights(helper, "water_plants_shallow_full");
        var special = providerWeights(helper, "water_plants");
        var shallowSpecial = providerWeights(helper, "water_plants_shallow");
        helper.assertTrue(deep.size() == 3 && shallow.size() == 3 && special.size() == 2 && shallowSpecial.size() == 1,
                "Savanna must retain one weighted choice per valid underwater column");
        double deepSpecial = deep.getOrDefault("aoemod:savanna/water/water_plants", 0.0);
        double shallowChance = shallow.getOrDefault("aoemod:savanna/water/water_plants_shallow", 0.0);
        near(helper, deepSpecial * special.getOrDefault("minecraft:wet_sponge", 0.0), .0125 / 4,
                "Deep sponge probability must be one quarter of the previous 1.25%");
        near(helper, deepSpecial * special.getOrDefault("minecraft:sea_pickle", 0.0), .0625 / 2,
                "Deep sea pickle probability must be half of the previous 6.25%");
        near(helper, shallowChance * shallowSpecial.getOrDefault("minecraft:sea_pickle", 0.0), .075 / 2,
                "Shallow sea pickle probability must be half of the previous 7.5%");
        near(helper, deep.getOrDefault("aoemod:water/kelp", 0.0), .0375, "Deep kelp probability must stay unchanged");
        near(helper, shallow.getOrDefault("aoemod:water/kelp", 0.0), .0375, "Shallow kelp probability must stay unchanged");
        near(helper, deep.getOrDefault("aoemod:savanna/water/seagrass", 0.0), .928125,
                "All freed deep selections must become seagrass");
        near(helper, shallow.getOrDefault("aoemod:water/short_seagrass", 0.0), .925,
                "All freed shallow selections must become short seagrass");
        var grass = (ProbabilityFeatureConfiguration) configured(helper, "seagrass").config();
        near(helper, grass.probability, .3, "The existing tall fraction of deep seagrass must be preserved");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void seededPaletteKeepsEveryColumnPlantedAtReducedRates(GameTestHelper helper) {
        for (int depth : new int[]{1, 7}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, depth);
            var feature = configured(helper, depth == 1 ? "water_plants_shallow_full" : "water_plants_full");
            RandomSource random = RandomSource.create(79463);
            int rootY = world.getLevel().getChunkSource().getGenerator().getSeaLevel() - depth;
            for (int x = 0; x < SAMPLE_SIDE; x++) for (int z = 0; z < SAMPLE_SIDE; z++) {
                feature.place(world, world.getLevel().getChunkSource().getGenerator(), random, new BlockPos(x, rootY, z));
            }
            // Run the same end-of-chunk repair as natural decoration: shallow
            // kelp choices otherwise cannot fit their required extra water cell.
            if (depth == 1) for (int x = 0; x < SAMPLE_SIDE; x += 16) for (int z = 0; z < SAMPLE_SIDE; z += 16) {
                SavannaSeafloor.fillShallowGaps(world, x, z);
            }
            int grass = 0, tall = 0, kelp = 0, pickles = 0, sponges = 0;
            for (int x = 0; x < SAMPLE_SIDE; x++) for (int z = 0; z < SAMPLE_SIDE; z++) {
                BlockPos pos = new BlockPos(x, rootY, z);
                BlockState state = world.getBlockState(pos);
                if (state.is(Blocks.SEAGRASS)) grass++;
                if (state.is(Blocks.TALL_SEAGRASS)) { grass++; tall++; }
                if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) kelp++;
                if (state.is(Blocks.SEA_PICKLE)) pickles++;
                if (state.is(Blocks.WET_SPONGE)) sponges++;
                if (depth == 1) helper.assertTrue(state.getFluidState().isSource()
                                && state.getFluidState().is(FluidTags.WATER) && world.isEmptyBlock(pos.above()),
                        "Shallow replacements must retain the existing water surface");
            }
            int samples = SAMPLE_SIDE * SAMPLE_SIDE;
            helper.assertTrue(grass + kelp + pickles + sponges == samples,
                    "Every valid floor column must contain one palette selection; depth=" + depth);
            distribution(helper, grass, samples, depth == 1 ? .925 : .928125, "seagrass", depth);
            distribution(helper, kelp, samples, .0375, "kelp", depth);
            distribution(helper, pickles, samples, depth == 1 ? .0375 : .03125, "sea pickles", depth);
            if (depth == 1) helper.assertTrue(sponges == 0 && tall == 0, "One-deep water must contain no sponges or tall seagrass");
            else {
                distribution(helper, sponges, samples, .003125, "wet sponges", depth);
                distribution(helper, tall, grass, .3, "tall fraction of seagrass", depth);
            }
            AOEMod.LOGGER.info("Savanna aquatic density at depth {}: {} seagrass ({} tall), {} kelp, {} pickles, {} sponges / {} roots, zero bare cells",
                    depth, grass, tall, kelp, pickles, sponges, samples);
        }
        helper.succeed();
    }

    private static ConfiguredFeature<?, ?> configured(GameTestHelper helper, String name) {
        var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:savanna/water/" + name));
        if (feature == null) throw new IllegalStateException("Missing Savanna aquatic feature " + name);
        return feature;
    }

    private static Map<String, Double> selectorWeights(GameTestHelper helper, String name) {
        var config = (RandomSelectionFeatureConfig) configured(helper, name).config();
        double total = config.features().stream().mapToDouble(entry -> entry.chance()).sum();
        near(helper, total, 1, "The weighted floor must retain total probability one");
        Map<String, Double> result = new HashMap<>();
        for (var entry : config.features()) {
            result.merge(entry.feature().unwrapKey().orElseThrow().location().toString(), entry.chance() / total, Double::sum);
        }
        return result;
    }

    private static Map<String, Double> providerWeights(GameTestHelper helper, String name) {
        var config = (SimpleBlockConfiguration) configured(helper, name).config();
        var json = BlockStateProvider.CODEC.encodeStart(JsonOps.INSTANCE, config.toPlace()).result().orElseThrow().getAsJsonObject();
        var entries = json.getAsJsonArray("entries");
        double total = 0;
        for (var entry : entries) total += entry.getAsJsonObject().get("weight").getAsDouble();
        Map<String, Double> result = new HashMap<>();
        for (var entry : entries) {
            var object = entry.getAsJsonObject();
            result.merge(object.getAsJsonObject("data").get("Name").getAsString(), object.get("weight").getAsDouble() / total, Double::sum);
        }
        return result;
    }

    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-7, message + "; actual=" + actual + ", expected=" + expected);
    }

    private static void distribution(GameTestHelper helper, int actual, int total, double probability, String name, int depth) {
        double expected = total * probability;
        double tolerance = 5 * Math.sqrt(total * probability * (1 - probability)) + 2;
        helper.assertTrue(Math.abs(actual - expected) <= tolerance,
                "Seeded " + name + " selections at depth " + depth + " must match the loaded weights; actual=" + actual + ", expected=" + expected);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, int depth) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        int seaLevel = level.getChunkSource().getGenerator().getSeaLevel();
        int floor = seaLevel - 1 - depth;
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= floor ? Blocks.MUD.defaultBlockState()
                        : pos.getY() < seaLevel ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(SavannaAquaticDensityTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getSeed": return 79463L;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : seaLevel;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                        case "canSeeSky": return true;
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try { return method.invoke(level, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
