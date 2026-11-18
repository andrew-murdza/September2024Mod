package net.amurdza.examplemod.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Verifies the loaded density graph, not just the climate value sent to biome selection. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsTerrainTests {
    private PlainsTerrainTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void plainsUsesFullDepthVanillaDensityAndCarversWithFluidChanges(GameTestHelper helper) {
        ServerLevel level = plains(helper);
        var registry = level.registryAccess();
        var settings = ((NoiseBasedChunkGenerator) level.getChunkSource().getGenerator()).generatorSettings().value();
        // Hybrid Aquatic rewrites the live minecraft:overworld router and its
        // offset with ocean trenches during server startup. Bootstrap a separate
        // vanilla registry snapshot so those additions are not our baseline.
        var vanillaLookup = VanillaRegistries.createLookup();
        var vanilla = vanillaLookup.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        var vanillaOps = RegistryOps.create(JsonOps.INSTANCE, vanillaLookup);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registry);
        JsonObject actual = NoiseRouter.CODEC.encodeStart(ops, settings.noiseRouter().mapAll(PlainsTerrainTests::unwrap)).getOrThrow(false, message -> { }).getAsJsonObject();
        JsonObject expected = NoiseRouter.CODEC.encodeStart(vanillaOps, vanilla.noiseRouter().mapAll(PlainsTerrainTests::unwrap)).getOrThrow(false, message -> { }).getAsJsonObject();
        // The no-lava mining rule is independent of vanilla cave geometry.
        actual.add("lava", expected.get("lava"));
        // Isolated underground aquifers are intentionally disabled; connected
        // surface water is then checked by the decoration connectivity pass.
        expected.addProperty("fluid_level_floodedness", -1.0);
        assertParity(helper, "noise-router", vanillaReferences(actual), expected);
        helper.assertTrue(settings.noiseSettings().equals(vanilla.noiseSettings()) && settings.isAquifersEnabled(),
                "Plains must use vanilla noise cell sizes and aquifer barriers");
        var functions = registry.registryOrThrow(Registries.DENSITY_FUNCTION);
        var vanillaFunctions = vanillaLookup.lookupOrThrow(Registries.DENSITY_FUNCTION);
        for (String name : new String[]{"offset", "factor", "jaggedness", "depth", "sloped_cheese"}) {
            JsonElement regional = DensityFunction.DIRECT_CODEC.encodeStart(ops,
                    functions.get(new ResourceLocation("aoemod", "plains/" + name)).mapAll(PlainsTerrainTests::unwrap)).getOrThrow(false, message -> { });
            JsonElement original = DensityFunction.DIRECT_CODEC.encodeStart(vanillaOps,
                    vanillaFunctions.getOrThrow(ResourceKey.create(Registries.DENSITY_FUNCTION,
                            new ResourceLocation("minecraft", "overworld/" + name))).value().mapAll(PlainsTerrainTests::unwrap))
                    .getOrThrow(false, message -> { });
            assertParity(helper, "spline-" + name, vanillaReferences(regional), original);
        }
        var carvers = registry.registryOrThrow(Registries.CONFIGURED_CARVER);
        Set<String> active = new HashSet<>();
        var biome = registry.registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod", "plains"));
        biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).forEach(holder ->
                active.add(holder.unwrapKey().orElseThrow().location().toString()));
        for (String name : new String[]{"cave", "cave_extra_underground", "canyon"}) {
            String id = "aoemod:plains/" + name;
            helper.assertTrue(active.contains(id), "Missing Plains vanilla-shaped carver " + name);
            JsonObject regional = ConfiguredWorldCarver.DIRECT_CODEC.encodeStart(ops,
                    carvers.get(new ResourceLocation(id))).getOrThrow(false, message -> { }).getAsJsonObject();
            JsonObject original = ConfiguredWorldCarver.DIRECT_CODEC.encodeStart(vanillaOps,
                    vanillaLookup.lookupOrThrow(Registries.CONFIGURED_CARVER).getOrThrow(ResourceKey.create(
                            Registries.CONFIGURED_CARVER, new ResourceLocation("minecraft", name))).value())
                    .getOrThrow(false, message -> { }).getAsJsonObject();
            helper.assertTrue(regional.getAsJsonObject("config").getAsJsonObject("lava_level").get("absolute").getAsInt() < level.getMinBuildHeight(),
                    "Vanilla carvers must not reintroduce lava into Plains");
            regional.getAsJsonObject("config").add("lava_level", original.getAsJsonObject("config").get("lava_level"));
            assertParity(helper, "carver-" + name, regional, original);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void fullVanillaContinentsAndDeepCavesRemainSeeded(GameTestHelper helper) {
        ServerLevel level = plains(helper);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var settings = generator.generatorSettings().value();
        var vanillaLookup = VanillaRegistries.createLookup();
        var vanilla = vanillaLookup.lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
        double previous = Double.NaN;
        boolean changed = false;
        for (long seed : new long[]{12345L, 98765L}) {
            RandomState regional = RandomState.create(settings, noises, seed);
            // Environmental adds surface-only noise references during bootstrap;
            // resolve those from the loaded registry while retaining the pristine
            // vanilla density graph and its unchanged vanilla noise keys.
            RandomState original = RandomState.create(vanilla, noises, seed);
            int highCaveAir = 0, deepCaveAir = 0, widestCave = 0;
            double minContinent = Double.POSITIVE_INFINITY, maxContinent = Double.NEGATIVE_INFINITY;
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                int blockX = x * 173 + 101, blockZ = z * 227 - 109;
                var point = new DensityFunction.SinglePointContext(blockX, 0, blockZ);
                double continent = regional.router().continents().compute(point);
                double raw = original.router().continents().compute(point);
                helper.assertTrue(Math.abs(continent - raw) < 1.0E-9,
                        "Plains must restore the full seeded vanilla continentalness field");
                minContinent = Math.min(minContinent, continent);
                maxContinent = Math.max(maxContinent, continent);
                helper.assertTrue(Math.abs(regional.router().erosion().compute(point) - original.router().erosion().compute(point)) < 1.0E-9
                                && Math.abs(regional.router().ridges().compute(point) - original.router().ridges().compute(point)) < 1.0E-9,
                        "Erosion and ridges must retain vanilla shapes");
                for (int y : new int[]{-55, 0, 1, 5, 9, 10, 11, 32, 64, 96, 160}) {
                    var sample = new DensityFunction.SinglePointContext(blockX, y, blockZ);
                    double expectedDensity = original.router().finalDensity().compute(sample);
                    helper.assertTrue(Math.abs(regional.router().finalDensity().compute(sample) - expectedDensity) < 1.0E-9,
                            "Density must match vanilla throughout the full -64..320 world, without a Y0..10 solid transition");
                }
                var column = generator.getBaseColumn(blockX, blockZ, level, regional);
                int run = 0;
                for (int y = -55; y <= 40; y++) {
                    var block = column.getBlock(y);
                    helper.assertFalse(block.is(Blocks.LAVA), "Plains must remain lava-free at " + blockX + "," + y + "," + blockZ);
                    helper.assertFalse(block.is(Blocks.DEEPSLATE), "Lower Plains must retain stone at " + blockX + "," + y + "," + blockZ);
                    if (block.isAir()) {
                        if (y <= 0) deepCaveAir++; else highCaveAir++;
                        widestCave = Math.max(widestCave, ++run);
                    } else run = 0;
                }
            }
            helper.assertTrue(maxContinent - minContinent > 0.03, "Continental field lost vanilla variation");
            helper.assertTrue(highCaveAir > 0 && deepCaveAir > 0 && widestCave >= 6,
                    "Expected vanilla noise caves and chambers both above and below Y0");
            double sample = regional.router().finalDensity().compute(new DensityFunction.SinglePointContext(1147, 48, -2391));
            if (!Double.isNaN(previous)) changed |= Math.abs(sample - previous) > 1.0E-6;
            previous = sample;
            AOEMod.LOGGER.info("Plains vanilla terrain seed {}: C {}..{}, cave air above zero {}, below zero {}, widest sampled cave {}", seed,
                    minContinent, maxContinent, highCaveAir, deepCaveAir, widestCave);
        }
        helper.assertTrue(changed, "Plains density ignored seed changes");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void restoredDeepOceansHaveWaterToTheirFloor(GameTestHelper helper) {
        ServerLevel level = plains(helper);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
        int audited = 0;
        search: for (long seed : new long[]{12345L, 98765L}) {
            var random = RandomState.create(generator.generatorSettings().value(), noises, seed);
            for (int gx = -30; gx <= 30; gx++) for (int gz = -30; gz <= 30; gz++) {
                int x = gx * 197, z = gz * 211;
                if (random.router().continents().compute(new DensityFunction.SinglePointContext(x, 0, z)) >= -.4) continue;
                int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random) - 1;
                if (floor < 10 || floor >= 42) continue;
                var column = generator.getBaseColumn(x, z, level, random);
                for (int y = floor + 1; y < generator.getSeaLevel(); y++) helper.assertTrue(column.getBlock(y).is(Blocks.WATER),
                        "Restored ocean contains an artificial dry gap at " + x + "," + y + "," + z + " for seed " + seed);
                if (++audited == 8) break search;
            }
        }
        helper.assertTrue(audited == 8, "Expected eight deep-ocean columns below the former Y44 fluid cutoff; found " + audited);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedDeepPlainsHasCavesInStoneAboveBedrock(GameTestHelper helper) {
        ServerLevel level = plains(helper);
        int caveAir = 0;
        for (int[] coordinate : new int[][]{{17821, -19673}, {17825, -19669}, {-18357, 12681}}) {
            var chunk = level.getChunk(coordinate[0], coordinate[1], ChunkStatus.CARVERS, true);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = -63; y <= 0; y++) {
                var pos = new BlockPos(coordinate[0] * 16 + x, y, coordinate[1] * 16 + z);
                var block = chunk.getBlockState(pos);
                helper.assertFalse(block.is(Blocks.DEEPSLATE) || block.is(Blocks.LAVA),
                        "Deep Plains must contain stone caves without deepslate or lava at " + pos);
                if (block.isAir()) caveAir++;
                if (y == -63) helper.assertTrue(chunk.getBlockState(pos.below()).is(Blocks.BEDROCK),
                        "Plains must end at the Y-64 bedrock floor");
            }
        }
        helper.assertTrue(caveAir > 0, "The lower Plains world must contain caves after carvers run");
        helper.succeed();
    }

    private static void assertParity(GameTestHelper helper, String label, JsonElement actual, JsonElement expected) {
        if (actual.equals(expected)) return;
        String difference = firstDifference("$", actual, expected);
        try {
            Path directory = Path.of("reports", "plains-terrain-parity");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(label + "-actual.json"), actual.toString());
            Files.writeString(directory.resolve(label + "-expected.json"), expected.toString());
        } catch (IOException exception) {
            AOEMod.LOGGER.warn("Could not save terrain parity diagnostics", exception);
        }
        helper.fail("Plains vanilla " + label + " mismatch: " + difference);
    }

    private static String firstDifference(String path, JsonElement actual, JsonElement expected) {
        if (actual.equals(expected)) return "";
        if (actual.isJsonObject() && expected.isJsonObject()) {
            JsonObject left = actual.getAsJsonObject(), right = expected.getAsJsonObject();
            for (var entry : right.entrySet()) {
                String key = entry.getKey();
                if (!left.has(key)) return path + "." + key + " missing; expected " + summary(entry.getValue());
                if (!left.get(key).equals(entry.getValue())) return firstDifference(path + "." + key, left.get(key), entry.getValue());
            }
            for (var entry : left.entrySet()) if (!right.has(entry.getKey())) return path + "." + entry.getKey() + " unexpectedly present";
        }
        if (actual.isJsonArray() && expected.isJsonArray()) {
            JsonArray left = actual.getAsJsonArray(), right = expected.getAsJsonArray();
            if (left.size() != right.size()) return path + " length actual " + left.size() + ", expected " + right.size();
            for (int i = 0; i < left.size(); i++) if (!left.get(i).equals(right.get(i))) return firstDifference(path + "[" + i + "]", left.get(i), right.get(i));
        }
        return path + " actual " + summary(actual) + ", expected " + summary(expected);
    }

    private static String summary(JsonElement value) {
        String text = value.toString();
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }

    private static DensityFunction unwrap(DensityFunction function) {
        // mapAll recursively resolves holders, including direct holders inside
        // spline coordinates. HolderHolder intentionally has no direct codec.
        while (function instanceof DensityFunctions.HolderHolder holder) function = holder.function().value();
        return function;
    }

    private static JsonElement vanillaReferences(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            JsonObject result = new JsonObject();
            value.getAsJsonObject().entrySet().forEach(entry -> result.add(entry.getKey(), vanillaReferences(entry.getValue())));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(item -> result.add(vanillaReferences(item)));
            return result;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && value.getAsString().startsWith("aoemod:plains/")) {
            return new JsonPrimitive(value.getAsString().replace("aoemod:plains/", "minecraft:overworld/"));
        }
        return value;
    }

    private static ServerLevel plains(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
    }
}
