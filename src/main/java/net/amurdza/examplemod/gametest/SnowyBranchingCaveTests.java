package net.amurdza.examplemod.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Compares seeded cave shape after the same 4x8x4 interpolation used by the generator. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyBranchingCaveTests {
    private static final int WIDTH = 96, HEIGHT = 64;
    private static final String VANILLA_CHEESE = """
            {"type":"minecraft:add","argument1":{"type":"minecraft:mul","argument1":4.0,
            "argument2":{"type":"minecraft:square","argument":{"type":"minecraft:noise",
            "noise":"minecraft:cave_layer","xz_scale":1.0,"y_scale":8.0}}},
            "argument2":{"type":"minecraft:clamp","min":-1.0,"max":1.0,
            "input":{"type":"minecraft:add","argument1":0.27,"argument2":{"type":"minecraft:noise",
            "noise":"minecraft:cave_cheese","xz_scale":1.0,"y_scale":0.6666666666666666}}}}
            """;

    private SnowyBranchingCaveTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void symmetricVanillaCheeseAddsBothTailsWithoutTunnelNoise(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var settings = generator.generatorSettings().value();
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        DensityFunction oldGraph = DensityFunction.DIRECT_CODEC.parse(ops, JsonParser.parseString(VANILLA_CHEESE))
                .getOrThrow(false, message -> { throw new IllegalStateException(message); });
        DensityFunction newGraph = level.registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION)
                .get(new ResourceLocation(AOEMod.MOD_ID, "snowy/caves/cheese"));
        String encoded = DensityFunction.DIRECT_CODEC.encodeStart(ops, newGraph).getOrThrow(false, message -> { }).toString();
        helper.assertTrue(encoded.contains("minecraft:abs") && encoded.contains("minecraft:cave_cheese")
                        && !encoded.contains("spaghetti") && !encoded.contains("noodle") && !encoded.contains("entrance"),
                "The active chamber field must contain only symmetric vanilla cheese and layer noise");
        long oldAir = 0, newAir = 0, total = 0;
        for (long seed : new long[]{12345L, 98765L, -8152788754161584091L}) {
            var before = seeded(level, settings, oldGraph, seed);
            var after = seeded(level, settings, newGraph, seed);
            for (int x = -4096; x <= 4096; x += 48) for (int z = -4096; z <= 4096; z += 48)
                for (int y = -80; y <= 32; y += 8) {
                    var point = new DensityFunction.SinglePointContext(x, y, z);
                    double previous = before.compute(point), revised = after.compute(point);
                    helper.assertTrue(previous >= 0 || revised < 0,
                            "Symmetric cheese must retain every original one-sided vanilla chamber");
                    if (previous < 0) oldAir++;
                    if (revised < 0) newAir++;
                    total++;
                }
            if (seed == 12345L) picture(helper, level, generator, seed,
                    new Grid(before, -96, -64, -96), new Grid(after, -96, -64, -96));
        }
        double ratio = newAir / (double) oldAir;
        helper.assertTrue(oldAir > 1000 && ratio > 1.8 && ratio < 2.2,
                "Both equally likely cheese tails must roughly double vanilla chamber volume: " + ratio);
        helper.assertTrue(newAir < total / 2, "Cheese chambers must retain substantial surrounding rock");
        AOEMod.LOGGER.info("Symmetric vanilla cheese: originalAir={} revisedAir={} samples={} ratio={}", oldAir, newAir, total, ratio);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void actualColumnsRetainDeepCavesAndProtectedLakeBeds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int lakeColumns = 0, caveAir = 0, solid = 0, repairedBedHoles = 0;
        for (long seed : new long[]{12345L, 98765L, -8152788754161584091L}) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            SnowyTerrainProfile[] profile = new SnowyTerrainProfile[1];
            random.router().finalDensity().mapAll(function -> {
                if (function instanceof SnowyTerrainProfile snowy && !snowy.surfaceOnly()) profile[0] = snowy;
                return function;
            });
            helper.assertTrue(profile[0] != null, "Inspect the loaded snowy terrain profile");
            for (int x = -1024; x <= 1024; x += 128) for (int z = -1024; z <= 1024; z += 128) {
                double ridge = Math.abs(profile[0].ridges().compute(new DensityFunction.SinglePointContext(x, 0, z)));
                var column = generator.getBaseColumn(x, z, level, random);
                if (ridge < MountainLakeOffset.LAKE_WEIRDNESS - .02) {
                    lakeColumns++;
                    int bed = net.amurdza.examplemod.worldgen.SnowyOceanFloor.floor(random, x, z);
                    for (int y = bed - 3; y <= bed; y++) if (column.getBlock(y).isAir()) repairedBedHoles++;
                    finishSampledColumn(level, random, x, z, column);
                    for (int y = bed - 3; y <= bed; y++) helper.assertTrue(column.getBlock(y).isSolid(),
                            "Cheese chambers must not carve a protected lake bed at " + x + "," + y + "," + z + " seed=" + seed);
                } else if (ridge > MountainLakeOffset.LAKE_WEIRDNESS + .02) {
                    for (int y = -80; y <= 24; y += 2) {
                        if (column.getBlock(y).isAir()) caveAir++;
                        else if (column.getBlock(y).isSolid()) solid++;
                    }
                }
            }
        }
        helper.assertTrue(lakeColumns > 20 && caveAir > 100 && solid > caveAir,
                "Actual generated columns need protected lakes and substantial caves within surrounding rock");
        helper.assertTrue(repairedBedHoles > 0,
                "The sampled columns must exercise the post-interpolation bed repair, not only already-solid beds");
        AOEMod.LOGGER.info("Snowy actual columns: protectedLakes={} caveAirSamples={} rockSamples={}", lakeColumns, caveAir, solid);
        helper.succeed();
    }

    private static void finishSampledColumn(ServerLevel level, RandomState random, int x, int z,
                                            net.minecraft.world.level.NoiseColumn column) {
        var chunk = new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4),
                net.minecraft.world.level.chunk.UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++)
            chunk.setBlockState(pos.set(x, y, z), column.getBlock(y), false);
        var rock = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        net.amurdza.examplemod.worldgen.SnowyOceanFloor.restoreCoverAndWater(chunk, random, rock);
        net.amurdza.examplemod.worldgen.SnowyCaveColumns.finish(chunk, random, rock);
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++)
            column.setBlock(y, chunk.getBlockState(pos.set(x, y, z)));
    }

    private static final class Grid {
        private static final int NX = WIDTH / 2 + 1, NY = HEIGHT / 4 + 1;
        private final double[] values = new double[NX * NY * NX];
        private Grid(DensityFunction density, int minX, int minY, int minZ) {
            for (int x = 0; x < NX; x++) for (int y = 0; y < NY; y++) for (int z = 0; z < NX; z++)
                values[(x * NY + y) * NX + z] = density.compute(new DensityFunction.SinglePointContext(minX + x * 4, minY + y * 8, minZ + z * 4));
        }
        private double sample(int x, int y, int z) {
            int bx = x / 4, by = y / 8, bz = z / 4;
            double tx = x % 4 / 4.0, ty = y % 8 / 8.0, tz = z % 4 / 4.0, result = 0;
            for (int dx = 0; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = 0; dz <= 1; dz++)
                result += values[((bx + dx) * NY + by + dy) * NX + bz + dz]
                        * (dx == 0 ? 1 - tx : tx) * (dy == 0 ? 1 - ty : ty) * (dz == 0 ? 1 - tz : tz);
            return result;
        }
    }

    private static DensityFunction seeded(ServerLevel level, NoiseGeneratorSettings settings, DensityFunction density, long seed) {
        NoiseRouter r = settings.noiseRouter();
        NoiseRouter router = new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(), r.fluidLevelSpreadNoise(),
                r.lavaNoise(), r.temperature(), r.vegetation(), r.continents(), r.erosion(), r.depth(), r.ridges(),
                r.initialDensityWithoutJaggedness(), density, r.veinToggle(), r.veinRidged(), r.veinGap());
        var configured = new NoiseGeneratorSettings(settings.noiseSettings(), settings.defaultBlock(), settings.defaultFluid(),
                router, settings.surfaceRule(), settings.spawnTarget(), settings.seaLevel(), settings.disableMobGeneration(),
                settings.isAquifersEnabled(), settings.oreVeinsEnabled(), settings.useLegacyRandomSource());
        return RandomState.create(configured, level.registryAccess().lookupOrThrow(Registries.NOISE), seed).router().finalDensity();
    }

    private static void picture(GameTestHelper helper, ServerLevel level, NoiseBasedChunkGenerator generator,
                                long seed, Grid before, Grid after) throws Exception {
        int slice = 8;
        double bestCoverageError = Double.POSITIVE_INFINITY;
        for (int y = 8; y <= 104; y += 2) {
            int open = 0;
            for (int x = 0; x < 192; x += 2) for (int z = 0; z < 192; z += 2)
                if (after.sample(x, y, z) < 0) open++;
            double error = Math.abs(open / (96.0 * 96) - .3);
            if (error < bestCoverageError) { bestCoverageError = error; slice = y; }
        }
        var picture = new BufferedImage(1230, 1110, BufferedImage.TYPE_INT_RGB);
        var g = picture.createGraphics();
        try {
            g.setColor(new Color(0xf4f2ec)); g.fillRect(0, 0, 1230, 1110);
            g.setColor(new Color(0x27313c)); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            g.drawString("Snowy cave shape — seed 12345", 35, 40);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            g.drawString("Interpolated cave fields at Y=" + (slice - 64)
                    + ", X/Z −96..95 (before surface clipping and pillars)", 35, 72);
            g.drawString("Original vanilla cheese tail", 35, 105); g.drawString("Both vanilla cheese tails", 650, 105);
            for (int x = 0; x < 192; x++) for (int z = 0; z < 192; z++) {
                g.setColor(new Color(before.sample(x, slice, z) < 0 ? 0xe5edf1 : 0x656272));
                g.fillRect(35 + x * 3, 120 + z * 3, 3, 3);
                g.setColor(new Color(after.sample(x, slice, z) < 0 ? 0xe5edf1 : 0x656272));
                g.fillRect(650 + x * 3, 120 + z * 3, 3, 3);
            }
            g.setColor(new Color(0x27313c));
            g.drawString("Actual generated columns: X −128..127, Z=0, Y−88..79 (light=cave; blue=water)", 35, 735);
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            for (int x = 0; x < 256; x++) {
                var column = generator.getBaseColumn(x - 128, 0, level, random);
                for (int y = -88; y <= 79; y++) {
                    var block = column.getBlock(y);
                    g.setColor(new Color(!block.getFluidState().isEmpty() ? 0x4996b9 : block.isAir() ? 0xe5edf1 : 0x656272));
                    g.fillRect(35 + x * 4, 750 + (79 - y) * 2, 4, 2);
                }
            }
            Path path = Path.of("evidence", "snowy-cheese-caves.png");
            Files.createDirectories(path.getParent());
            helper.assertTrue(ImageIO.write(picture, "png", path.toFile()), "PNG encoder must be available");
            AOEMod.LOGGER.info("Saved snowy cave shape evidence to {}", path.toAbsolutePath());
        } finally { g.dispose(); }
    }
}
