package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyTerrainRevisionTests {
    private SnowyTerrainRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void lakeBanksDescendAtRequestedSlopeAndStopNearFiftyDeep(GameTestHelper helper) {
        var lake = new MountainLakeOffset(DensityFunctions.constant(-0.4), new LinearRidge(),
                DensityFunctions.constant(0.4), DensityFunctions.constant(-0.15));
        double before = height(lake, 120);
        helper.assertTrue(Math.abs(before - 76.8) < 0.0001, "Highlands must retain the exact vanilla offset");
        for (int x = 99; x >= 70; x--) {
            double next = height(lake, x);
            if (x < 99) helper.assertTrue(before - next >= 1 - 1e-9 && before - next <= 2 + 1e-9,
                    "A straight noise contour must descend one to two blocks per horizontal block");
            before = next;
        }
        helper.assertTrue(Math.abs(height(lake, 0) - 13) < 0.0001,
                "Extra lake displacement must stop fifty blocks below sea level63");
        var varied = new MountainLakeOffset(DensityFunctions.constant(-0.4), new LinearRidge(),
                DensityFunctions.constant(0.6), DensityFunctions.constant(-0.45));
        helper.assertTrue(height(varied, 0) > height(lake, 0),
                "Continentalness and erosion must still affect the deepest lake floor");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedDappledLandHasOneGrassAndTwoDirtLayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int fresh = (int) ((System.currentTimeMillis() / 1000) % 10000) * 512;
        BlockPos inland = null;
        search: for (int x = 3_700_000 + fresh; x < 3_704_096 + fresh; x += 64) {
            for (int z = -3_800_000; z < -3_795_904; z += 64) {
                var biome = generator.getBiomeSource().getNoiseBiome(x >> 2, 40, z >> 2, random.sampler());
                if (!biome.unwrapKey().orElseThrow().location().getPath().equals("dappled_forest")) continue;
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                if (height > 66 && height < 150) { inland = new BlockPos(x, height, z); break search; }
            }
        }
        helper.assertTrue(inland != null, "Could not locate a fresh dry dappled column");
        int cx = inland.getX() >> 4, cz = inland.getZ() >> 4;
        ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.SURFACE, true);
        int checked = 0;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int bed = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
            BlockPos pos = new BlockPos(cx * 16 + dx, bed, cz * 16 + dz);
            if (chunk.getFluidState(pos.above()).is(FluidTags.WATER)
                    || !chunk.getNoiseBiome(pos.getX() >> 2, (bed + 1) >> 2, pos.getZ() >> 2)
                    .unwrapKey().orElseThrow().location().getPath().equals("dappled_forest")) continue;
            if (!chunk.getBlockState(pos).is(Blocks.GRASS_BLOCK)) continue; // Natural cliffs/cave mouths retain stone.
            helper.assertTrue(chunk.getBlockState(pos.below()).is(Blocks.DIRT)
                            && chunk.getBlockState(pos.below(2)).is(Blocks.DIRT),
                    "Dry dappled soil must have grass and two dirt layers at " + pos);
            helper.assertTrue(chunk.getBlockState(pos.above()).isAir(), "Dappled surface generation must not add snow");
            checked++;
        }
        helper.assertTrue(checked >= 16, "The physical surface test must inspect actual generated soil");
        helper.succeed();
    }

    private static double height(DensityFunction offset, int x) {
        return 128 + 128 * offset.compute(new DensityFunction.SinglePointContext(x, 0, 0));
    }

    private record LinearRidge() implements DensityFunction.SimpleFunction {
        @Override public double compute(FunctionContext context) { return context.blockX() * 0.001; }
        @Override public double minValue() { return -1_000_000; }
        @Override public double maxValue() { return 1_000_000; }
        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return DensityFunctions.constant(0).codec(); }
    }
}
