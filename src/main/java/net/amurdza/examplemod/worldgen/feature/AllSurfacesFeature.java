package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class AllSurfacesFeature extends Feature<AllSurfacesFeatureConfig> {

    public AllSurfacesFeature(Codec<AllSurfacesFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AllSurfacesFeatureConfig> context) {
        AllSurfacesFeatureConfig cfg = context.config();
        PlacedFeature feature = cfg.feature.value();

        WorldGenLevel level = context.level();
        ChunkAccess chunk = level.getChunk(context.origin());
        ChunkPos chunkPos = chunk.getPos();

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        boolean placedAnything = false;

        for (int dx = 0; dx < 16; dx++) {
            int x = minX + dx;
            for (int dz = 0; dz < 16; dz++) {
                int z = minZ + dz;

                int maxY = chunk.getMaxBuildHeight();
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, maxY, z);

                for (int y = maxY; y >= chunk.getMinBuildHeight() + 1; y--) {
                    boolean placed = tryPlace(
                            level, pos, context,
                            cfg, feature
                    );
                    placedAnything |= placed;

                    if (placed && !cfg.allLayers) {
                        break;
                    }
                    pos.move(0, -1, 0);
                }
            }
        }
        return placedAnything;
    }
    private static boolean isFullFluid(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        var fs = state.getFluidState();
        return fs.getType() == fluid && fs.isSource(); // ✅ “level is full”
    }

    private static boolean isSameFluid(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return state.getFluidState().getType() == fluid;
    }

    private boolean tryPlace(
            WorldGenLevel level,
            BlockPos pos,
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg,
            PlacedFeature feature
    ) {
        // ✅ Biome restriction from config
        if (cfg.biomes != null && !level.getBiome(pos).is(cfg.biomes)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        boolean validTarget;

        switch (cfg.target) {
            case AIR -> {
                validTarget = state.isAir() || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
            }
            case WATER -> {
                // ✅ only place in FULL water blocks (source)
                boolean hereFull = isFullFluid(state, net.minecraft.world.level.material.Fluids.WATER);
                boolean aboveIsWater = isSameFluid(above, net.minecraft.world.level.material.Fluids.WATER);

                validTarget = hereFull && (
                        (cfg.deep && aboveIsWater) ||
                                (!cfg.deep && !aboveIsWater)
                );
            }
            case LAVA -> {
                // ✅ only place in FULL lava blocks (source)
                boolean hereFull = isFullFluid(state, net.minecraft.world.level.material.Fluids.LAVA);
                boolean aboveIsLava = isSameFluid(above, net.minecraft.world.level.material.Fluids.LAVA);

                validTarget = hereFull && (
                        (cfg.deep && aboveIsLava) ||
                                (!cfg.deep && !aboveIsLava)
                );
            }
            default -> validTarget = false;
        }

        if (validTarget && cfg.predicate.test(level, pos.below())) {
            feature.place(level, context.chunkGenerator(), context.random(), pos);
            return true;
        }
        return false;
    }

}
