package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.registry.ModConfiguredFeatures;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class AzaleaSaplingTreeGrower extends AbstractMegaTreeGrower {
    @Override
    public boolean growTree(
            ServerLevel level,
            ChunkGenerator generator,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        BlockPos base = findValidTwoByTwoBase(level, pos, state.getBlock());
        if (base == null) {
            return false;
        }

        return this.placeMega(
                level,
                generator,
                pos,
                state,
                random,
                base.getX() - pos.getX(),
                base.getZ() - pos.getZ()
        );
    }

    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        return null;
    }

    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource random) {
        return ModConfiguredFeatures.AZALEA_LARGE_TREE_FROM_SAPLING;
    }

    private BlockPos findValidTwoByTwoBase(ServerLevel level, BlockPos pos, Block sapling) {
        for (int offsetX = 0; offsetX >= -1; offsetX--) {
            for (int offsetZ = 0; offsetZ >= -1; offsetZ--) {
                BlockPos base = pos.offset(offsetX, 0, offsetZ);
                if (isValidTwoByTwo(level, base, sapling)) {
                    return base;
                }
            }
        }

        return null;
    }

    private boolean isValidTwoByTwo(ServerLevel level, BlockPos base, Block sapling) {
        for (int offsetX = 0; offsetX <= 1; offsetX++) {
            for (int offsetZ = 0; offsetZ <= 1; offsetZ++) {
                BlockPos saplingPos = base.offset(offsetX, 0, offsetZ);
                if (!level.getBlockState(saplingPos).is(sapling)
                        || !level.getBiome(saplingPos).is(ModTags.Biomes.aoeJungleBiomes)) {
                    return false;
                }
            }
        }

        return true;
    }
}
