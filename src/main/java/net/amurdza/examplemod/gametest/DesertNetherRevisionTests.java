package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.GlowstoneFeature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertNetherRevisionTests {
    private DesertNetherRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void loweredDesertWaterPreservesFourDeepBasinsAndUpperMaterialRegions(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        helper.assertTrue(generator.getSeaLevel() == 60, "Desert sea level must be four blocks lower");
        int totalNormal = 0, totalWater = 0, totalOldWater = 0, upperChecks = 0;
        for (long seed : new long[]{0, 12345, 98765, -8152788754161584091L}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            int normal = 0, wet = 0, oldWet = 0, exposed = 0;
            BlockPos deepBed = null;
            for (int x = -8192; x < 8192; x += 32) for (int z = -8192; z < 8192; z += 32) {
                var point = new DensityFunction.SinglePointContext(x, 0, z);
                double height = random.router().initialDensityWithoutJaggedness().compute(point);
                double land = random.router().temperature().compute(point);
                double detail = random.router().vegetation().compute(point);
                double original = originalDesertHeight(land, detail);
                helper.assertTrue(height >= 55, "Groundwater must never be more than four blocks deep");
                helper.assertTrue(Math.abs(height - DesertHeight.height(land, detail)) < 1e-10,
                        "Router and desert profile disagree");
                if (height <= 80.5) normal++;
                if (height <= 58.5) wet++;
                if (original <= 62.5) oldWet++;
                if (original <= 62.5 && height > 58.5) exposed++;
                if (original > 59) helper.assertTrue(height == original, "Existing slopes above the old floor must not move");
                int oldTop = (int) Math.ceil(original + .5) - 1;
                int newTop = (int) Math.ceil(height + .5) - 1;
                helper.assertTrue((oldTop <= 80) == (newTop <= 80)
                                && (oldTop >= 87) == (newTop >= 87),
                        "Sand/red-sand/terracotta regions changed at " + x + "," + z);
                if (oldTop > 80) {
                    helper.assertTrue(height == original && oldTop == newTop,
                            "Red sand and terracotta must retain their exact former surface heights");
                    upperChecks++;
                }
                if (height == 55 && DesertHeight.contour(land, detail) < 52 && deepBed == null) deepBed = new BlockPos(x, 55, z);
            }
            helper.assertTrue(wet > 0 && wet < oldWet && exposed > 0,
                    "Lower water must expose ordinary sand while retaining lakes: old=" + oldWet + ", new=" + wet);
            totalNormal += normal;
            totalWater += wet;
            totalOldWater += oldWet;
            helper.assertTrue(deepBed != null, "The basin needs a maximum-depth central floor");
            var column = generator.getBaseColumn(deepBed.getX(), deepBed.getZ(), level, random);
            helper.assertTrue(!column.getBlock(55).isAir() && !column.getBlock(55).is(Blocks.WATER),
                    "Maximum-depth lake bed must be solid at Y55");
            for (int y = 56; y <= 59; y++) helper.assertTrue(column.getBlock(y).is(Blocks.WATER),
                    "Actual interpolated generator must contain four water blocks at Y56..59");
            helper.assertTrue(column.getBlock(60).isAir(), "Water surface must stop below Y60");
        }
        double previous = 55;
        for (int i = -1000; i <= -25; i++) {
            double land = i / 1000.0;
            double h = DesertHeight.height(land, .8);
            helper.assertTrue(h >= previous, "The basin must descend monotonically toward its noise-field center");
            helper.assertTrue(h == DesertHeight.height(land, -.8), "Detail noise must not create lowland islands");
            previous = h;
        }
        helper.assertTrue(upperChecks > 1000, "Insufficient sampled red-sand and terracotta coordinates");
        AOEMod.LOGGER.info("Lowered desert water: {} -> {} of {} ordinary sand columns; {} exact upper-region checks",
                totalOldWater, totalWater, totalNormal, upperChecks);
        helper.succeed();
    }

    /** Independent copy of the previous profile locks both material contours and shoreline behavior. */
    private static double originalDesertHeight(double land, double detail) {
        double ridge = 72 + Math.pow(Math.max(0, land + .06), 1.5) * 145;
        double shallowStart = Math.pow(1.0 / 145, 2.0 / 3) - .06;
        if (ridge <= 73) return Math.max(59, 73 + (land - shallowStart) * 50);
        double t = Math.max(0, Math.min(1, ridge - 73));
        return ridge + detail * 5 * t * t * (3 - 2 * t);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void displayedDesertAndNetherBiomeBoundariesDependOnlyOnY(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var deep = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("deep_dark")));
        var desert = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("desert")));
        for (var dimension : java.util.List.of(GameplayDimensions.DESERT, GameplayDimensions.NETHER)) {
            var level = server.getLevel(dimension);
            BiomeManager.NoiseBiomeSource source = (x, y, z) -> y < 0 ? deep : desert;
            for (long seed : new long[]{0, 12345, 98765}) {
                var manager = new BiomeManager(level, seed).withDifferentSource(source);
                for (int x = -32; x < 32; x++) for (int z = -32; z < 32; z++) {
                    for (int y : new int[]{-3, -2, -1, 0, 1, 2, 3}) {
                        helper.assertTrue(manager.getBiome(new BlockPos(x, y, z)).equals(y < 0 ? deep : desert),
                                "Vanilla biome zoom must not displace the vertical boundary at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void basaltFeaturesHaveNoMoundsAndGeneratedLavaBedsRestoreTheirPlants(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID("basalt_deltas"));
        for (var step : biome.getGenerationSettings().features()) for (var holder : step) {
            String id = holder.unwrapKey().orElseThrow().location().toString();
            var feature = holder.value().feature().value().feature();
            helper.assertTrue(feature instanceof OreFeature || feature instanceof ScatteredOreFeature
                            || feature instanceof GlowstoneFeature || id.equals("aoemod:documented_flora"),
                    "A feature can still add unwanted rock/fire/mushrooms: " + id);
        }
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var selected = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        int candidates = 0, centerLavaBeds = 0;
        // A fixed 16x16-chunk window can lie entirely on the same dry bank or
        // high lake-noise contour. Spread the deterministic terrain-only search
        // over 64x64 sites, with 512 blocks between sites. The permutation visits
        // different X and Z regions immediately instead of exhausting one row.
        for (int candidate = 0; candidate < 4096 && selected.size() < 6; candidate++) {
            int gridX = candidate % 64, gridZ = (gridX * 17 + candidate / 64) % 64;
            int cx = 81000 + gridX * 32, cz = -81000 + gridZ * 32;
            candidates++;
            if (!baseColumnHasLavaBed(level, generator, random, cx * 16 + 8, cz * 16 + 8)) continue;
            centerLavaBeds++;
            int suitable = 1;
            for (int dx : new int[]{2, 13}) for (int dz : new int[]{2, 13}) {
                if (baseColumnHasLavaBed(level, generator, random, cx * 16 + dx, cz * 16 + dz)) suitable++;
            }
            if (suitable == 5) selected.add(new net.minecraft.world.level.ChunkPos(cx, cz));
        }
        String selection = "seed=" + level.getSeed() + ", candidates=" + candidates
                + ", center lava beds=" + centerLavaBeds + ", selected=" + selected;
        AOEMod.LOGGER.info("Nether lava-bed terrain selection: {}", selection);
        helper.assertTrue(selected.size() == 6, "The bounded terrain search must find six suitable lava-bed chunks: " + selection);
        // Finish generation only after selection. Plant counts must never decide
        // which terrain enters the sample, nor trigger manual feature placement.
        for (var pos : selected) level.getChunk(pos.x, pos.z);
        int plants = 0, lavaColumns = 0;
        var results = new java.util.ArrayList<String>();
        for (var pos : selected) {
            var chunk = level.getChunk(pos.x, pos.z);
            int chunkPlants = 0, chunkLavaColumns = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                boolean lava = false;
                for (int y = 2; y < 12; y++) {
                    var state = chunk.getBlockState(new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                    lava |= state.is(Blocks.LAVA);
                    var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (id != null && id.getNamespace().equals("netherdepthsupgrade")
                            && (id.getPath().contains("crimson_seagrass") || id.getPath().contains("crimson_kelp"))) chunkPlants++;
                }
                if (lava) chunkLavaColumns++;
            }
            results.add(pos + " lava columns=" + chunkLavaColumns + ", plants=" + chunkPlants);
            helper.assertTrue(chunkLavaColumns > 0, "A terrain-selected chunk lost its actual generated lava: " + selection + "; " + results);
            plants += chunkPlants;
            lavaColumns += chunkLavaColumns;
        }
        String result = selection + ", sampled=" + selected.size() + ", generated lava columns=" + lavaColumns
                + ", generated plants=" + plants + ", chunks=" + results;
        AOEMod.LOGGER.info("Nether generated lava-bed audit: {}", result);
        helper.assertTrue(plants > 10, "The real lava-bed pass must restore the old seagrass/kelp mixture: " + result);
        helper.succeed();
    }

    private static boolean baseColumnHasLavaBed(net.minecraft.server.level.ServerLevel level,
                                               NoiseBasedChunkGenerator generator, RandomState random, int x, int z) {
        // Use Minecraft's actual interpolated column, including the fluid picker
        // and mining banks, rather than testing the underlying noise value.
        var column = generator.getBaseColumn(x, z, level, random);
        if (!column.getBlock(9).is(Blocks.LAVA)) return false;
        for (int y = 1; y < 11; y++) {
            if (column.getBlock(y).is(Blocks.LAVA) && column.getBlock(y + 1).is(Blocks.LAVA)
                    && column.getBlock(y - 1).isFaceSturdy(level, new BlockPos(x, y - 1, z), net.minecraft.core.Direction.UP)) return true;
        }
        return false;
    }
}
