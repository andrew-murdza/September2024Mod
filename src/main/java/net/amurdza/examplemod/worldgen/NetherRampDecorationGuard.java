package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Natural decoration must leave a narrow usable center along the seeded interlayer ramps. */
public final class NetherRampDecorationGuard {
    private static final Map<RandomState, Optional<DimensionTerrain>> TERRAIN = Collections.synchronizedMap(new WeakHashMap<>());

    private NetherRampDecorationGuard() { }

    /** RandomState may retain holder/cache wrappers around the custom router entry. Resolve once per seeded state. */
    public static Optional<DimensionTerrain> terrain(RandomState random) {
        return TERRAIN.computeIfAbsent(random, state -> {
            DimensionTerrain[] found = {null};
            state.router().continents().mapAll(function -> {
                if (function instanceof DimensionTerrain terrain && terrain.dimension().equals("nether")) found[0] = terrain;
                return function;
            });
            return Optional.ofNullable(found[0]);
        });
    }

    public static boolean allows(WorldGenLevel level, BlockPos pos, BlockState proposed) {
        // ServerLevel also implements WorldGenLevel: player building and bonemeal remain ordinary gameplay.
        if (!(level instanceof WorldGenRegion) || !GameplayDimensions.NETHER.equals(level.getLevel().dimension())) return true;
        // Feature attempts may leave the write region. Do not read an unavailable chunk before vanilla rejects them.
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        DimensionTerrain terrain = terrain(level.getLevel().getChunkSource().randomState()).orElse(null);
        if (terrain == null) return true;
        var column = new DensityFunction.SinglePointContext(pos.getX(), 0, pos.getZ());
        if (DimensionTerrain.netherRampDistance(pos.getZ(), terrain.detail().compute(column)) > 2.5) return true;
        double floor = DimensionTerrain.netherRampFloor(pos.getX(), terrain.lake().compute(column));
        // Include interpolation's floor rounding, the supporting block, and adult headroom.
        if (pos.getY() < floor - 2 || pos.getY() > floor + 5) return true;
        if (!proposed.getFluidState().isEmpty() || hazardous(proposed)) return false;
        BlockState existing = level.getBlockState(pos);
        var oldShape = existing.getCollisionShape(level, pos);
        var newShape = proposed.getCollisionShape(level, pos);
        // Retain the generated support and headroom. Harmless flowers and material changes may remain.
        return !Shapes.joinIsNotEmpty(oldShape, newShape, BooleanOp.NOT_SAME);
    }

    public static boolean hazardous(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.WITHER_ROSE) || state.getBlock() instanceof CactusBlock
                || state.getBlock() instanceof SweetBerryBushBlock;
    }
}
