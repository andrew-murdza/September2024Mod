package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.amurdza.examplemod.worldgen.SnowyLakeCaves;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyWideCaveRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void cheeseCavesPreservePillarsWithoutArtificialSurfaceOutlets(GameTestHelper helper) {
        var ridge = DensityFunctions.constant(.5);
        var sealed = new SnowyTerrainProfile(ridge, DensityFunctions.constant(1));
        var open = new SnowyTerrainProfile(ridge, DensityFunctions.constant(-.2));
        var supported = new SnowyTerrainProfile(ridge, DensityFunctions.constant(-.2), DensityFunctions.constant(.1), false);
        for (int x = -256; x <= 256; x += 16) for (int y = -64; y < 72; y++) {
            var point = new DensityFunction.SinglePointContext(x, y, 0);
            helper.assertTrue(sealed.compute(point) > 0,
                    "Positive cheese noise may not produce an independently authored outlet");
            helper.assertTrue(supported.compute(point) == open.compute(point),
                    "Legacy native pillar noise must not close cheese cavities before round supports are added");
            if (y < 48) helper.assertTrue(open.compute(point) < 0,
                    "A supported mountain must retain its deep cheese chambers");
        }
        // At Y80 the same cheese noise emerges through a forest hillside but still
        // leaves the summit closed. The shell above the horizontal passage is intact.
        double hillsideRidge = .4;
        while (SnowyTerrainProfile.height(hillsideRidge) < 81) hillsideRidge += .001;
        var hillside = new SnowyTerrainProfile(DensityFunctions.constant(hillsideRidge), DensityFunctions.constant(-.3));
        helper.assertTrue(hillside.compute(new DensityFunction.SinglePointContext(0, 80, 0)) < 0
                        && hillside.compute(new DensityFunction.SinglePointContext(0, 74, 0)) > 0,
                "A horizontal cave layer can daylight through a snowy slope without cutting a vertical shaft");
        var plateau = new SnowyTerrainProfile(DensityFunctions.constant(.90), DensityFunctions.constant(-.3));
        int summit = (int) SnowyTerrainProfile.PLATEAU_HEIGHT;
        helper.assertTrue(plateau.compute(new DensityFunction.SinglePointContext(0, summit, 0)) > 0,
                "Plateau surfaces retain their resistance");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void cheeseCavesKeepFreshShorelineColumnsSolidAfterInterpolation(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var random = level.getChunkSource().randomState();
        // Reserved coordinates in the isolated test world keep this seam reproducible.
        int fresh = 6_200_000;
        ChunkPos boundary = null;
        search: for (int x = fresh; x < fresh + 4096; x += 16) for (int z = -6_300_000; z < -6_295_904; z += 16) {
            double center = Math.abs(random.router().ridges().compute(new DensityFunction.SinglePointContext(x + 8, 0, z + 8)));
            if (Math.abs(center - MountainLakeOffset.LAKE_WEIRDNESS) > .025) continue;
            int wetSide = 0, drySide = 0;
            double minimum = 1;
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                double ridge = Math.abs(random.router().ridges().compute(new DensityFunction.SinglePointContext(x + dx, 0, z + dz)));
                minimum = Math.min(minimum, ridge);
                if (SnowyLakeCaves.lakeColumn(random, x + dx, z + dz)) wetSide++; else drySide++;
            }
            if (wetSide >= 24 && drySide >= 24 && minimum < MountainLakeOffset.LAKE_WEIRDNESS - .01) {
                boundary = new ChunkPos(x >> 4, z >> 4);
                break search;
            }
        }
        helper.assertTrue(boundary != null, "The shoreline regression requires a fresh chunk spanning both lake and land");
        var chunk = level.getChunk(boundary.x, boundary.z, ChunkStatus.CARVERS, true);
        int protectedColumns = 0, dryColumns = 0, waterColumns = 0;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = boundary.getMinBlockX() + dx, z = boundary.getMinBlockZ() + dz;
            if (!SnowyLakeCaves.lakeColumn(random, x, z)) { dryColumns++; continue; }
            protectedColumns++;
            int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
            if (chunk.getFluidState(new BlockPos(x, floor + 1, z)).is(FluidTags.WATER)) waterColumns++;
            for (int y = floor - 3; y <= floor; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                var state = chunk.getBlockState(pos);
                helper.assertTrue(!state.isAir() && state.getFluidState().isEmpty(),
                        "Cheese chambers must not perforate the lake boundary after interpolation/carving: " + pos);
            }
        }
        helper.assertTrue(protectedColumns >= 24 && dryColumns >= 24 && waterColumns >= 8,
                "The physical shoreline audit needs protected banks and actual lake water: protected="
                        + protectedColumns + ", dry=" + dryColumns + ", wet=" + waterColumns);
        AOEMod.LOGGER.info("Snowy cheese cave shoreline audit: chunk={} protected={} dry={} wet={}",
                boundary, protectedColumns, dryColumns, waterColumns);
        helper.succeed();
    }

}
