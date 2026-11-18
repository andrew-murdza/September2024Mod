package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Labels Plains surface water using the same interpolator and aquifer as its actual terrain. */
public final class SurfaceWaterBiome implements DensityFunction {
    public static final KeyDispatchDataCodec<SurfaceWaterBiome> CODEC = KeyDispatchDataCodec.of(
            MapCodec.unit(SurfaceWaterBiome::new));
    private static final DensityFunctions.BeardifierOrMarker NO_STRUCTURES = new DensityFunctions.BeardifierOrMarker() {
        @Override public double compute(FunctionContext context) { return 0; }
        @Override public double minValue() { return 0; }
        @Override public double maxValue() { return 0; }
    };
    public static final int MAX_CACHED_COLUMNS = 16384;
    private volatile SamplingContext sampling;

    public SurfaceWaterBiome() { }

    private SurfaceWaterBiome(SamplingContext sampling) { this.sampling = sampling; }

    /** Called for both runtime router and climate-sampler instances after their seed is installed. */
    public void bind(RandomState random, NoiseGeneratorSettings settings) {
        this.sampling = new SamplingContext(random, settings);
    }

    /** Router, climate sampler and their later NoiseChunk copies share this seed's bounded cache. */
    public static void bindTogether(RandomState random, NoiseGeneratorSettings settings, DensityFunction... functions) {
        SamplingContext context = new SamplingContext(random, settings);
        for (DensityFunction function : functions) {
            while (true) {
                if (function instanceof DensityFunctions.HolderHolder holder) function = holder.function().value();
                else if (function instanceof DensityFunctions.MarkerOrMarked marker) function = marker.wrapped();
                else {
                    if (function instanceof SurfaceWaterBiome surface) surface.sampling = context;
                    break;
                }
            }
        }
    }

    @Override
    public double compute(FunctionContext context) {
        SamplingContext current = sampling;
        if (current == null) throw new IllegalStateException("Surface-water biome needs a seeded RandomState");
        return current.compute(context.blockX(), context.blockZ());
    }

    public record CacheStats(int columns, long sampledColumns) { }

    public CacheStats cacheStats() {
        SamplingContext current = sampling;
        if (current == null) return new CacheStats(0, 0);
        synchronized (current.columns) {
            return new CacheStats(current.columns.size(), current.sampledColumns.get());
        }
    }

    private static boolean isSurfaceWater(SamplingContext current, int x, int z) {
        NoiseGeneratorSettings settings = current.settings;
        NoiseSettings noise = settings.noiseSettings();
        int width = noise.getCellWidth(), height = noise.getCellHeight();
        int minX = Math.floorDiv(x, width) * width, minZ = Math.floorDiv(z, width) * width;
        // Match DimensionFluidPicker's Plains cutoff exactly. Aquifer pressure
        // can seal a negative-density shoreline with stone, so density alone
        // cannot determine whether a column actually contains surface water.
        Aquifer.FluidStatus water = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        Aquifer.FluidStatus dry = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        Sampler sampler = new Sampler(current.random, minX, minZ, noise, settings,
                (fx, fy, fz) -> fy < 0 ? dry : water);
        int minCellY = Math.floorDiv(noise.minY(), height), cellsY = noise.height() / height;
        sampler.initializeForFirstCellX();
        try {
            sampler.advanceCellX(0);
            for (int cellY = cellsY - 1; cellY >= 0; cellY--) {
                sampler.selectCellYZ(cellY, 0);
                for (int dy = height - 1; dy >= 0; dy--) {
                    int y = (minCellY + cellY) * height + dy;
                    sampler.updateForY(y, dy / (double) height);
                    sampler.updateForX(x, (x - minX) / (double) width);
                    sampler.updateForZ(z, (z - minZ) / (double) width);
                    BlockState block = sampler.block();
                    if (block == null) block = settings.defaultBlock();
                    if (y == settings.seaLevel() - 1) return block.getFluidState().is(FluidTags.WATER);
                    if (block.blocksMotion() || y < settings.seaLevel() - 1) return false;
                }
            }
            return false;
        } finally {
            sampler.stopInterpolation();
        }
    }

    @Override public void fillArray(double[] values, ContextProvider context) { context.fillAllDirectly(values, this); }
    @Override public DensityFunction mapAll(Visitor visitor) { return visitor.apply(new SurfaceWaterBiome(sampling)); }
    @Override public double minValue() { return 0; }
    @Override public double maxValue() { return 1; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }

    private static final class SamplingContext {
        private final RandomState random;
        private final NoiseGeneratorSettings settings;
        private final Map<Long, CompletableFuture<Double>> columns = new LinkedHashMap<>(256, .75F, true);
        private final AtomicLong sampledColumns = new AtomicLong();

        private SamplingContext(RandomState random, NoiseGeneratorSettings settings) {
            this.random = random;
            this.settings = settings;
        }

        private double compute(int x, int z) {
            long key = ChunkPos.asLong(x, z);
            CompletableFuture<Double> value;
            boolean owner = false;
            synchronized (columns) {
                value = columns.get(key);
                if (value == null) {
                    trim(MAX_CACHED_COLUMNS - 1);
                    value = new CompletableFuture<>();
                    columns.put(key, value);
                    owner = true;
                }
            }
            if (owner) {
                try {
                    sampledColumns.incrementAndGet();
                    value.complete(isSurfaceWater(this, x, z) ? 1.0 : 0.0);
                    synchronized (columns) { trim(MAX_CACHED_COLUMNS); }
                } catch (RuntimeException | Error failure) {
                    value.completeExceptionally(failure);
                    synchronized (columns) { columns.remove(key, value); }
                    throw failure;
                }
            }
            return value.join();
        }

        /** Keep in-flight entries until completion, so two workers never build the same sampler. */
        private void trim(int maximum) {
            if (columns.size() <= maximum) return;
            var entries = columns.entrySet().iterator();
            while (columns.size() > maximum && entries.hasNext()) {
                if (entries.next().getValue().isDone()) entries.remove();
            }
        }
    }

    private static final class Sampler extends NoiseChunk {
        private Sampler(RandomState random, int x, int z, NoiseSettings noise,
                        NoiseGeneratorSettings settings, Aquifer.FluidPicker fluids) {
            super(1, random, x, z, noise, NO_STRUCTURES, settings, fluids, Blender.empty());
        }
        private BlockState block() { return getInterpolatedState(); }
    }
}
