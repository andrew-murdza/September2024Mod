package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static net.amurdza.examplemod.worldgen.feature.AllSurfacesFeatureConfig.Target.AIR;

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

                int maxY = chunk.getMaxBuildHeight() - 1;
                int minY = chunk.getMinBuildHeight() -1;

                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, maxY, z);

                for (int y = maxY; y >= minY; y--) {
                    pos.set(x, y, z);

                    boolean placed = tryPlace(level, pos, context, cfg, feature);
                    placedAnything |= placed;

                    if (placed && !cfg.allLayers) {
                        break;
                    }
                }
            }
        }

        return placedAnything;
    }

    private boolean tryPlace(
            WorldGenLevel level,
            BlockPos pos,
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg,
            PlacedFeature feature
    ) {
        if (cfg.biomes != null && !level.getBiome(pos.below()).is(cfg.biomes)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        boolean validTarget;

        switch (cfg.target) {
            case AIR -> validTarget =
                    state.isAir()
                            && level.getFluidState(pos).isEmpty()
                            && level.getFluidState(pos.above()).isEmpty();

            case WATER -> {
                boolean hereWater = state.is(Blocks.WATER);
                boolean aboveWater = above.is(Blocks.WATER);

                validTarget = hereWater && (
                        (cfg.deep && aboveWater) ||
                                (!cfg.deep && !aboveWater)
                );
            }

            case LAVA -> {
                boolean hereLava = state.is(Blocks.LAVA);
                boolean aboveLava = above.is(Blocks.LAVA);

                validTarget = hereLava && (
                        (cfg.deep && aboveLava) ||
                                (!cfg.deep && !aboveLava)
                );
            }

            default -> validTarget = false;
        }

        if (!validTarget) {
            return false;
        }

        if (!cfg.predicate.test(level, pos.below())) {
            return false;
        }

        if (cfg.target == AIR) {
            if (!isSafeAirOrigin(level, pos)) {
                return false;
            }
        }

        return feature.place(level, context.chunkGenerator(), context.random(), pos);
    }

    private boolean isSafeAirOrigin(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getFluidState(pos).isEmpty();
    }
}