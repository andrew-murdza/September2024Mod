package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.amurdza.examplemod.worldgen.SnowyBandCompression;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoblinFortressTerrainTests {
    private GoblinFortressTerrainTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void mountainFootRisesSteeplyToASmallerContinuousCrown(GameTestHelper helper) {
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(MountainLakeOffset.LAKE_WEIRDNESS) - 62.5) < 1e-9
                        && Math.abs(SnowyTerrainProfile.height(.4) - 69.15) < 1e-9,
                "The taller mountain face must preserve the lake and dappled terrace elevations");
        helper.assertTrue(SnowyTerrainProfile.height(.54) > 94
                        && SnowyTerrainProfile.height(.9) > 101 && SnowyTerrainProfile.height(.9) < 103,
                "Most of the mountain climb belongs to its lower face, with the existing summit altitude");
        helper.assertTrue(SnowyTerrainProfile.height(.89) < SnowyTerrainProfile.height(.9),
                "The old broad summit must keep climbing until its narrow crown");
        double crown = SnowyBandCompression.plateauWidth(2);
        helper.assertTrue(crown > 30 && crown < 40,
                "The level crown must shrink from roughly81 blocks to fewer than40, was " + crown);
        double previous = SnowyTerrainProfile.height(.4);
        for (int sample = 1; sample <= 5000; sample++) {
            double w = .4 + sample * .0001;
            double next = SnowyTerrainProfile.height(w);
            helper.assertTrue(next >= previous - 1e-9 && Double.isFinite(next),
                    "Every mountain transect must climb continuously without pits or overshooting ridges");
            helper.assertTrue(next == SnowyTerrainProfile.height(-w), "Opposite flanks must share the profile");
            previous = next;
        }
        for (double seam : new double[]{.4, SnowyTerrainProfile.MOUNTAIN_SHOULDER, SnowyTerrainProfile.PLATEAU_START}) {
            double epsilon = 1e-7;
            double left = (SnowyTerrainProfile.height(seam) - SnowyTerrainProfile.height(seam - epsilon)) / epsilon;
            double right = (SnowyTerrainProfile.height(seam + epsilon) - SnowyTerrainProfile.height(seam)) / epsilon;
            helper.assertTrue(Math.abs(left - right) < .01,
                    "Cliff shaping must not create a discontinuous height or slope at " + seam);
        }
        double maximumGrade = 0;
        for (int step = 0; step < 14400; step++) {
            double t = .7 + step * .3 / 14400, dt = .3 / 14400;
            double rise = SnowyTerrainProfile.height(MountainRidges.contour(t + dt))
                    - SnowyTerrainProfile.height(MountainRidges.contour(t));
            maximumGrade = Math.max(maximumGrade, rise / (480 * dt));
        }
        helper.assertTrue(maximumGrade > .85 && maximumGrade < 1.1,
                "The mountain face must be roughly twice the former0.454 grade without becoming a vertical wall");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void seededFortressSitesKeepCropsAndLivestockInTheirHabitatBands(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var crops = new ResourceLocation("aoemod", "dappled_forest");
        var snow = new ResourceLocation("aoemod", "snowy_forest");
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(new ResourceLocation("aoemod", "yeti_caves"));
        helper.assertTrue(set.placement() instanceof GoblinFortressPlacement,
                "The actual snowy boss structure set must use its mountainside placement");
        for (long seed : new long[]{0, 12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            boolean[] active = {false};
            random.router().finalDensity().mapAll(function -> {
                if (function instanceof SnowyTerrainProfile) active[0] = true;
                return function;
            });
            helper.assertTrue(active[0], "The revised mountains must feed the whole dimension's density router");
            int accepted = 0;
            for (int band = -1; band <= 1; band++) for (int station = -2; station <= 2; station++) {
                ChunkPos owner = GoblinFortressPlacement.candidate(random, band, station);
                var site = GoblinFortressPlacement.site(random, owner);
                if (site == null) continue;
                accepted++;
                helper.assertTrue(new ChunkPos(site.origin()).equals(owner)
                                && site.equals(GoblinFortressPlacement.site(random, owner)),
                        "Placement must be deterministic and owned by the central footprint chunk");
                helper.assertTrue(site.origin().getY() >= 65 && site.origin().getY() <= 70,
                        "The courtyard must meet the dry dappled terrace without floating above it");
                for (int x = -72; x <= 72; x += 12) {
                    for (int z : new int[]{-50, -32, -16}) {
                        BlockPos pos = site.origin().offset(x, 0, z * site.uphill());
                        helper.assertTrue(generator.getBiomeSource().getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2,
                                        pos.getZ() >> 2, random.sampler()).is(crops),
                                "Every seeded farmland strip must remain in dappled forest: " + pos + " seed=" + seed);
                    }
                    for (int z : new int[]{12, 35}) {
                        BlockPos pos = site.origin().offset(x, 12, z * site.uphill());
                        helper.assertTrue(generator.getBiomeSource().getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2,
                                        pos.getZ() >> 2, random.sampler()).is(snow),
                                "The upper livestock terraces must belong to snowy forest: " + pos + " seed=" + seed);
                    }
                }
            }
            helper.assertTrue(accepted >= 12,
                    "The kilometre station search must retain at least80% of the sites across seed " + seed + ": " + accepted + "/15");
        }
        helper.succeed();
    }
}
