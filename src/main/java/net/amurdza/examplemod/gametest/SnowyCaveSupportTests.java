package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.CaveSupports;
import net.amurdza.examplemod.worldgen.SnowyCaveSupports;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Measures complete round pillar footprints against the larger Nether-style supports. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyCaveSupportTests {
    private static final int WIDTH = 512;
    private static final double PILLAR_THRESHOLD = .03;

    private SnowyCaveSupportTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void snowyRoundPillarsAreSlimmerAndMoreFrequent(GameTestHelper helper) {
        Footprints vanilla = new Footprints();
        Footprints revised = new Footprints();
        for (long seed : new long[]{12345L, 98765L, -8152788754161584091L}) {
            CaveSupports before = new CaveSupports(seed);
            SnowyCaveSupports after = new SnowyCaveSupports(seed);
            collect((x, z) -> before.density(x, z, 0, 0) > 0, -256, -256, vanilla);
            collect((x, z) -> after.clearance(x, z) <= 0, -256, -256, revised);
        }
        helper.assertTrue(vanilla.diameters.size() > 100 && revised.diameters.size() > 100,
                "The pillar comparison needs substantial complete footprints in both profiles");
        double countRatio = revised.diameters.size() / (double) vanilla.diameters.size();
        double meanWidthRatio = revised.meanDiameter() / vanilla.meanDiameter();
        AOEMod.LOGGER.info("Snowy pillar footprints: native={} revised={} countRatio={} meanDiameter={}->{} "
                        + "medianDiameter={}->{} p90Diameter={}->{} sampledSolidArea={}->{}",
                vanilla.diameters.size(), revised.diameters.size(), countRatio,
                vanilla.meanDiameter(), revised.meanDiameter(), vanilla.percentile(.5), revised.percentile(.5),
                vanilla.percentile(.9), revised.percentile(.9), vanilla.area, revised.area);
        helper.assertTrue(countRatio > 2.5 && countRatio < 4.0,
                "The finer jittered grid must produce more frequent complete round supports: " + countRatio);
        helper.assertTrue(meanWidthRatio > .2 && meanWidthRatio < .4,
                "Snowy cores must be much slimmer than the existing Nether-style supports: " + meanWidthRatio);
        helper.assertTrue(revised.percentile(.9) < 6.5 && revised.percentile(.9) > revised.percentile(.5),
                "Round supports need small, varying radii rather than identical broad cylinders");
        helper.succeed();
    }

    private static void collect(java.util.function.BiPredicate<Integer, Integer> density, int minX, int minZ, Footprints result) {
        boolean[] solid = new boolean[WIDTH * WIDTH];
        for (int x = 0; x < WIDTH; x++) for (int z = 0; z < WIDTH; z++) {
            solid[x * WIDTH + z] = density.test(minX + x, minZ + z);
            if (solid[x * WIDTH + z]) result.area++;
        }
        int[] queue = new int[solid.length];
        for (int start = 0; start < solid.length; start++) {
            if (!solid[start]) continue;
            int head = 0, tail = 1;
            queue[0] = start;
            solid[start] = false;
            boolean edge = false;
            while (head < tail) {
                int cell = queue[head++], x = cell / WIDTH, z = cell % WIDTH;
                edge |= x == 0 || z == 0 || x == WIDTH - 1 || z == WIDTH - 1;
                // Face adjacency matches actual block connections; diagonal pixels are separate supports.
                for (int direction = 0; direction < 4; direction++) {
                    int next = switch (direction) {
                        case 0 -> x > 0 ? cell - WIDTH : -1;
                        case 1 -> x < WIDTH - 1 ? cell + WIDTH : -1;
                        case 2 -> z > 0 ? cell - 1 : -1;
                        default -> z < WIDTH - 1 ? cell + 1 : -1;
                    };
                    if (next >= 0 && solid[next]) {
                        solid[next] = false;
                        queue[tail++] = next;
                    }
                }
            }
            // Boundary fragments bias widths downward. A one-block peak is not a broad support footprint.
            if (!edge && tail > 1) result.diameters.add(2 * Math.sqrt(tail / Math.PI));
        }
    }

    private static DensityFunction seeded(ServerLevel level, NoiseGeneratorSettings settings,
                                           DensityFunction density, long seed) {
        NoiseRouter r = settings.noiseRouter();
        NoiseRouter replaced = new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(),
                r.fluidLevelSpreadNoise(), r.lavaNoise(), r.temperature(), r.vegetation(), r.continents(),
                r.erosion(), r.depth(), r.ridges(), r.initialDensityWithoutJaggedness(), density,
                r.veinToggle(), r.veinRidged(), r.veinGap());
        NoiseGeneratorSettings configured = new NoiseGeneratorSettings(settings.noiseSettings(), settings.defaultBlock(),
                settings.defaultFluid(), replaced, settings.surfaceRule(), settings.spawnTarget(), settings.seaLevel(),
                settings.disableMobGeneration(), settings.isAquifersEnabled(), settings.oreVeinsEnabled(),
                settings.useLegacyRandomSource());
        return RandomState.create(configured, level.registryAccess().lookupOrThrow(Registries.NOISE), seed)
                .router().finalDensity();
    }

    private static final class Footprints {
        private final List<Double> diameters = new ArrayList<>();
        private int area;

        private double meanDiameter() { return diameters.stream().mapToDouble(Double::doubleValue).average().orElse(0); }

        private double percentile(double fraction) {
            Collections.sort(diameters);
            return diameters.get(Math.min(diameters.size() - 1, (int) (diameters.size() * fraction)));
        }
    }
}
