package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SurfaceWaterBiome;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SurfaceWaterCacheTests {
    private SurfaceWaterCacheTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void clonesAndConcurrentQueriesReuseTheSameSeededColumn(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), 817231);
        SurfaceWaterBiome source = classifier(random.router().vegetation());
        var copies = new ArrayList<SurfaceWaterBiome>();
        for (int i = 0; i < 8; i++) copies.add((SurfaceWaterBiome) source.mapAll(function -> function));
        int x = 2_406_004, z = -3_120_008;
        long before = source.cacheStats().sampledColumns();
        var gate = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(4);
        var results = new ArrayList<java.util.concurrent.Future<Double>>();
        try {
            for (int i = 0; i < copies.size(); i++) {
                final int index = i;
                results.add(workers.submit((Callable<Double>) () -> {
                    gate.await();
                    return copies.get(index).compute(new DensityFunction.SinglePointContext(x, index * 23, z));
                }));
            }
            gate.countDown();
            double expected = results.get(0).get();
            for (var result : results) helper.assertTrue(result.get() == expected, "Concurrent classifier copies disagree");
            helper.assertTrue(source.cacheStats().sampledColumns() - before == 1,
                    "Concurrent copies must construct exactly one sampler for an uncached column");
            helper.assertTrue(random.sampler().sample(x >> 2, 0, z >> 2).humidity() == (long) (expected * 10000),
                    "Climate sampler must use the same cached classification");
            helper.assertTrue(source.cacheStats().sampledColumns() - before == 1,
                    "Climate and router leaves must share the cache");
        } finally {
            gate.countDown();
            workers.shutdownNow();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void cachedClassificationStillMatchesActualColumnsAcrossSeeds(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var settings = generator.generatorSettings().value();
        var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
        var first = RandomState.create(settings, noises, 817231);
        var second = RandomState.create(settings, noises, 728193);
        for (int x : new int[]{-384, 0, 384}) for (int z : new int[]{-384, 0, 384}) {
            var point = new DensityFunction.SinglePointContext(x, 0, z);
            double original = first.router().vegetation().compute(point);
            for (var random : new RandomState[]{first, second}) {
                var column = generator.getBaseColumn(x, z, level, random);
                boolean wet = column.getBlock(settings.seaLevel() - 1).getFluidState().is(FluidTags.WATER);
                for (int y = settings.seaLevel(); wet && y < level.getMaxBuildHeight(); y++) {
                    if (column.getBlock(y).blocksMotion()) wet = false;
                }
                helper.assertTrue((random.router().vegetation().compute(point) == 1) == wet,
                        "Cached lake classification changed the physical water result at " + x + "," + z);
            }
            helper.assertTrue(first.router().vegetation().compute(point) == original,
                    "A different seed must not replace the first world's cached column");
        }
        helper.succeed();
    }

    private static SurfaceWaterBiome classifier(DensityFunction function) {
        while (true) {
            if (function instanceof DensityFunctions.HolderHolder holder) function = holder.function().value();
            else if (function instanceof DensityFunctions.MarkerOrMarked marker) function = marker.wrapped();
            else return (SurfaceWaterBiome) function;
        }
    }
}
