package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertBands;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BandCompletenessTests {
    private static final long[] SEEDS = {817231L, 728193L, -97513L};

    private BandCompletenessTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void fallingPassagesWidenWithoutChangingHorizontalConnectors(GameTestHelper helper) {
        var zero = DensityFunctions.constant(0);
        int fallingColumns = 0, samples = 0;
        for (double bend : new double[]{-.65, -.25, 0, .25, .65}) {
            var detail = DensityFunctions.constant(bend);
            var nether = new DimensionTerrain("nether", "density", zero, detail, zero, zero, zero, zero);
            var desert = new DimensionTerrain("desert", "density", DensityFunctions.constant(1), zero, zero, zero, zero, zero, detail, zero);
            int centre = (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2 - bend * 18);
            for (var terrain : new DimensionTerrain[]{nether, desert}) {
                // Mid-slope elevations are enclosed in the mining bank and
                // cannot borrow width from any large gallery or its connector.
                int x = terrain == nether ? 82 : 252;
                int y = terrain == nether ? 64 : -11;
                int width = 0;
                for (int z = centre - 16; z <= centre + 16; z++) {
                    if (terrain.compute(new DensityFunction.SinglePointContext(x, y, z)) < 0) width++;
                }
                helper.assertTrue(width >= 14 && width <= 16,
                        "An isolated falling passage must be approximately fifteen blocks wide, formerly ten: " + width);
                fallingColumns += width;
                samples++;
            }
        }
        double meanWidth = fallingColumns / (double) samples;
        helper.assertTrue(meanWidth >= 14.5 && meanWidth <= 15.5,
                "Sub-block offsets must average approximately fifty percent wider than the former ten-block passage: " + meanWidth);

        var nether = new DimensionTerrain("nether", "density", zero, zero, zero, zero, zero, zero);
        var desert = new DimensionTerrain("desert", "density", DensityFunctions.constant(1), zero, zero, zero, zero, zero);
        int netherConnector = 0, desertConnector = 0, outletWidth = 0;
        var outlet = desert.desertOutlet(0, 0);
        int distance = 86;
        double heading = outlet.headingAt(distance);
        for (int dx = -30; dx <= 30; dx++) {
            if (nether.compute(new DensityFunction.SinglePointContext(dx, 23, 152)) < 0) netherConnector++;
            if (desert.compute(new DensityFunction.SinglePointContext(232 + dx, -21, 152)) < 0) desertConnector++;
            int outletX = (int) Math.round(outlet.xAt(distance) + dx * Math.cos(heading));
            int outletZ = (int) Math.round(outlet.zAt(distance) - dx * Math.sin(heading));
            if (desert.compute(new DensityFunction.SinglePointContext(outletX, 55, outletZ)) < 0) outletWidth++;
        }
        helper.assertTrue(netherConnector == 15 && desertConnector == 17,
                "Existing lateral connectors must retain their former footprints: " + netherConnector + "/" + desertConnector);
        helper.assertTrue(outletWidth >= 14 && outletWidth <= 16,
                "The curved desert outlet must retain its fifteen-block width perpendicular to its heading: " + outletWidth);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void actualInterpolatedNetherPassagesRetainTheWidenedCrossSection(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int widths = 0, formerWidths = 0, samples = 0;
        for (long seed : SEEDS) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            var rawDensity = new DimensionTerrain("nether", "density", terrain.land(), terrain.detail(),
                    terrain.river(), terrain.lake(), terrain.caveA(), terrain.caveB(), terrain.rampDetail(), terrain.rampBasin());
            for (int stripe : new int[]{-1, 0, 1}) {
                double x = 82, z = (stripe + .5) * DimensionTerrain.STAIR_BAND_SPACING;
                for (int iteration = 0; iteration < 8; iteration++) {
                    var point = new DensityFunction.SinglePointContext((int) Math.floor(x), 0, (int) Math.floor(z));
                    x = 82 - terrain.rampBasin().compute(point) * 32;
                    z = (stripe + .5) * DimensionTerrain.STAIR_BAND_SPACING - terrain.rampDetail().compute(point) * 18;
                }
                int width = 0, formerWidth = 0;
                for (int blockZ = (int) Math.round(z) - 16; blockZ <= (int) Math.round(z) + 16; blockZ++) {
                    int blockX = (int) Math.round(x);
                    var column = generator.getBaseColumn(blockX, blockZ, level, random);
                    boolean open = column.getBlock(64).isAir() && column.getBlock(65).isAir();
                    boolean formerlyOpen = formerRampDensity(rawDensity, blockX, 64, blockZ) < 0
                            && formerRampDensity(rawDensity, blockX, 65, blockZ) < 0;
                    if (open) width++;
                    if (formerlyOpen) formerWidth++;
                    helper.assertTrue(!formerlyOpen || open, "Widening must retain the former interpolated passage");
                }
                // The seeded displacement also stretches/compresses the local Z
                // coordinate. Compare the same warped ten-block baseline, rather
                // than assuming that every physical cross section is 12..18 wide.
                helper.assertTrue(formerWidth > 0 && width > formerWidth && width < 30,
                        "Every seeded passage must widen without opening its whole mining bank: seed=" + seed
                                + ", stripe=" + stripe + ", previous=" + formerWidth + ", width=" + width);
                AOEMod.LOGGER.info("Interpolated Nether passage seed={} stripe={} oldWidth={} newWidth={}", seed, stripe, formerWidth, width);
                widths += width;
                formerWidths += formerWidth;
                samples++;
            }
        }
        double average = widths / (double) samples;
        helper.assertTrue(average >= 13.5 && average <= 16.5,
                "Actual seeded passage widths must remain approximately fifteen blocks after interpolation: " + average);
        double widening = widths / (double) formerWidths;
        helper.assertTrue(widening >= 1.4 && widening <= 1.6,
                "Matched seeded/interpolated widths must retain the approved fifty-percent increase: " + widening);
        helper.succeed();
    }

    /** Previous ten-block ramp clipped inside the same bank, with the generator's 4x4x4 interpolation. */
    private static double formerRampDensity(DimensionTerrain terrain, int x, int y, int z) {
        int bx = Math.floorDiv(x, 4) * 4, by = Math.floorDiv(y, 4) * 4, bz = Math.floorDiv(z, 4) * 4;
        double tx = (x - bx) / 4.0, ty = (y - by) / 4.0, tz = (z - bz) / 4.0, value = 0;
        for (int dx = 0; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = 0; dz <= 1; dz++) {
            var point = new DensityFunction.SinglePointContext(bx + dx * 4, by + dy * 4, bz + dz * 4);
            double corridor = DimensionTerrain.netherRampDistance(point.blockZ(), terrain.detail().compute(
                    new DensityFunction.SinglePointContext(point.blockX(), 0, point.blockZ())));
            double narrow = Math.max(terrain.compute(point), (corridor - 5) / 4);
            value += narrow * (dx == 0 ? 1 - tx : tx) * (dy == 0 ? 1 - ty : ty) * (dz == 0 ? 1 - tz : tz);
        }
        return value;
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void everySnowyTransectReachesLakeDappledAndSnowyForest(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : SEEDS) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            for (int x : new int[]{-8192, -748, 0, 1652, 8192}) {
                for (int startZ : new int[]{-3840, 0, 3840}) {
                    Set<String> habitats = new HashSet<>();
                    int lake = 0, dappled = 0, snowy = 0;
                    for (int z = startZ; z < startZ + 1920; z += 4) {
                        String biome = generator.getBiomeSource().getNoiseBiome(x / 4, 32, z / 4, random.sampler())
                                .unwrapKey().orElseThrow().location().getPath();
                        habitats.add(biome);
                        if (biome.equals("mountain_lake") || biome.equals("cold_ocean")) lake++;
                        else if (biome.equals("dappled_forest")) dappled++;
                        else if (biome.equals("snowy_forest")) snowy++;
                    }
                    helper.assertTrue(lake > 24 && dappled > 48 && snowy > 80,
                            "A complete snowy band must include substantial lake, dappled and snowy sections: seed=" + seed
                                    + ", transect=" + x + "," + startZ + ", counts=" + lake + "/" + dappled + "/" + snowy
                                    + ", habitats=" + habitats);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void everyDesertTransectReachesLakesSandRedSandAndTerracotta(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : SEEDS) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            for (int x : new int[]{-8192, -748, 0, 1652, 8192}) {
                for (int startZ : new int[]{-3072, 0, 3072}) {
                    int lakes = 0, sand = 0, red = 0, terracotta = 0;
                    for (int z = startZ; z < startZ + DesertBands.PERIOD; z += 4) {
                        double contour = random.router().temperature().compute(new DensityFunction.SinglePointContext(x, 0, z));
                        boolean lake = generator.getBiomeSource().getNoiseBiome(x / 4, 24, z / 4, random.sampler())
                                .unwrapKey().orElseThrow().location().getPath().equals("desert_lake");
                        if (lake) lakes++;
                        else if (contour < DesertHeight.RED_SAND_START_NOISE) sand++;
                        else if (contour < DesertHeight.TERRACOTTA_START_NOISE) red++;
                        else terracotta++;
                    }
                    helper.assertTrue(lakes >= 20 && sand >= 10 && red >= 4 && terracotta >= 20,
                            "No complete desert band may lose a region: seed=" + seed + ", transect=" + x + "," + startZ
                                    + ", lake/sand/red/terracotta=" + lakes + "/" + sand + "/" + red + "/" + terracotta);
                    helper.assertTrue(red < sand && red < terracotta,
                            "Red sand must remain narrower than the neighboring sand and terracotta regions");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void actualDesertWaterHasLakeBiomeAndCaveMouthsKeepItsRoof(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : SEEDS) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            int wet = 0, dry = 0;
            for (int x : new int[]{-620, -4, 308, 924}) {
                for (int z = -1536; z < 1536; z += 64) {
                    var column = generator.getBaseColumn(x, z, level, random);
                    boolean water = column.getBlock(DesertHeight.SEA_LEVEL - 1).is(Blocks.WATER);
                    boolean lake = generator.getBiomeSource().getNoiseBiome(x / 4, 24, z / 4, random.sampler())
                            .unwrapKey().orElseThrow().location().getPath().equals("desert_lake");
                    helper.assertTrue(lake == water, "Desert biome locate samples must follow actual surface water: seed="
                            + seed + ", column=" + x + "," + z + ", lake=" + lake + ", water=" + water);
                    if (water) wet++; else dry++;
                }
            }
            helper.assertTrue(wet > 0 && dry > 0, "Water alignment samples must exercise both lake and dry terrain");

            DimensionTerrain terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            int protectedMouths = 0, openLowerPassages = 0;
            outer:
            for (int cycle = -1; cycle <= 1; cycle++) for (int stripe = -8; stripe <= 8; stripe++) {
                var outlet = terrain.desertOutlet(cycle, stripe);
                for (int distance : new int[]{64, 76, 88, 100}) for (int dx : new int[]{-6, 0, 6}) {
                    double heading = outlet.headingAt(distance);
                    int x = (int) Math.floor(outlet.xAt(distance) + dx * Math.cos(heading));
                    int z = (int) Math.floor(outlet.zAt(distance) - dx * Math.sin(heading));
                    var point = new DensityFunction.SinglePointContext(x, 0, z);
                    double height = DesertHeight.height(terrain.land().compute(point), terrain.detail().compute(point));
                    if (!DimensionTerrain.isSurfaceWater(height, DesertHeight.SEA_LEVEL)) continue;
                    var column = generator.getBaseColumn(x, z, level, random);
                    for (int y = 46; y <= 49; y++) {
                        helper.assertTrue(column.getBlock(y).blocksMotion() && column.getBlock(y).getFluidState().isEmpty(),
                                "A lake cave mouth must retain its solid protective roof and lake bed at seed=" + seed
                                        + ", column=" + x + "," + y + "," + z + ": " + column.getBlock(y));
                    }
                    helper.assertTrue(column.getBlock(53).is(Blocks.WATER), "Protecting the cave roof must retain surface lake water");
                    for (int y = 28; y < 46; y++) if (column.getBlock(y).isAir()) {
                        openLowerPassages++;
                        break;
                    }
                    if (++protectedMouths == 24) break outer;
                }
            }
            helper.assertTrue(protectedMouths >= 12 && openLowerPassages > 0,
                    "Roof audit must exercise actual wet mouths and surviving lower passages: seed=" + seed
                            + ", protected=" + protectedMouths + ", lower passages=" + openLowerPassages);
        }
        helper.succeed();
    }
}
