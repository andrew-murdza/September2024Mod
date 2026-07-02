package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.registry.ModConfiguredFeatures;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RainforestSaplingHelper {
    private static final Map<Block, ResourceKey<ConfiguredFeature<?, ?>>> RAINFOREST_MEGA_TREES = new HashMap<>();
    private static final Map<Block, ResourceKey<ConfiguredFeature<?, ?>>> NORMAL_SAPLING_TREES = new HashMap<>();

    static {
        RAINFOREST_MEGA_TREES.put(Blocks.OAK_SAPLING, ModConfiguredFeatures.OAK_LARGE_TREE_FROM_SAPLING);
        RAINFOREST_MEGA_TREES.put(Blocks.DARK_OAK_SAPLING, ModConfiguredFeatures.DARK_OAK_LARGE_TREE_FROM_SAPLING);
        RAINFOREST_MEGA_TREES.put(Blocks.MANGROVE_PROPAGULE, ModConfiguredFeatures.MANGROVE_LARGE_TREE_FROM_SAPLING);
        RAINFOREST_MEGA_TREES.put(Blocks.CHERRY_SAPLING, ModConfiguredFeatures.CHERRY_LARGE_TREE_FROM_SAPLING);
        RAINFOREST_MEGA_TREES.put(Blocks.JUNGLE_SAPLING, ModConfiguredFeatures.JUNGLE_LARGE_TREE_FROM_SAPLING);

        NORMAL_SAPLING_TREES.put(Blocks.OAK_SAPLING, ModConfiguredFeatures.OAK_TREE);
        NORMAL_SAPLING_TREES.put(Blocks.BIRCH_SAPLING, ModConfiguredFeatures.BIRCH_TREE);
        NORMAL_SAPLING_TREES.put(Blocks.SPRUCE_SAPLING, ModConfiguredFeatures.SPRUCE_TREE);
        NORMAL_SAPLING_TREES.put(Blocks.ACACIA_SAPLING, ModConfiguredFeatures.ACACIA_TREE);
    }

    private RainforestSaplingHelper() {}

    public static boolean shouldHandle(ServerLevel level, BlockPos pos, BlockState state) {
        return RAINFOREST_MEGA_TREES.containsKey(state.getBlock())
                && level.getBiome(pos).is(ModTags.Biomes.rainforestBiomes);
    }

    public static boolean shouldHandleNormalSapling(ServerLevel level, BlockPos pos, BlockState state) {
        return NORMAL_SAPLING_TREES.containsKey(state.getBlock())
                && !level.getBiome(pos).is(ModTags.Biomes.rainforestBiomes);
    }

    public static boolean advanceTree(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            @Nullable ChunkGenerator generator
    ) {
        ResourceKey<ConfiguredFeature<?, ?>> featureKey = RAINFOREST_MEGA_TREES.get(state.getBlock());
        if (featureKey == null) {
            return false;
        }

        Optional<BlockPos> base = findTwoByTwoSaplings(level, pos, state.getBlock());
        if (base.isEmpty()) {
            return false;
        }

        if (state.hasProperty(SaplingBlock.STAGE) && state.getValue(SaplingBlock.STAGE) == 0) {
            level.setBlock(pos, state.cycle(SaplingBlock.STAGE), 4);
            return false;
        }

        ChunkGenerator actualGenerator = generator == null
                ? level.getChunkSource().getGenerator()
                : generator;

        return placeTwoByTwoFeature(level, base.get(), random, featureKey, actualGenerator);
    }

    public static boolean growNormalSaplingTree(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            @Nullable ChunkGenerator generator
    ) {
        ResourceKey<ConfiguredFeature<?, ?>> featureKey = NORMAL_SAPLING_TREES.get(state.getBlock());
        if (featureKey == null) {
            return false;
        }

        ChunkGenerator actualGenerator = generator == null
                ? level.getChunkSource().getGenerator()
                : generator;

        return placeSingleFeature(level, pos, state, random, featureKey, actualGenerator);
    }

    private static Optional<BlockPos> findTwoByTwoSaplings(ServerLevel level, BlockPos pos, Block block) {
        for (int dx = 0; dx >= -1; dx--) {
            for (int dz = 0; dz >= -1; dz--) {
                BlockPos base = pos.offset(dx, 0, dz);
                if (isTwoByTwoSaplings(level, base, block)) {
                    return Optional.of(base);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isTwoByTwoSaplings(ServerLevel level, BlockPos base, Block block) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                if (level.getBlockState(base.offset(dx, 0, dz)).getBlock() != block) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean placeTwoByTwoFeature(
            ServerLevel level,
            BlockPos base,
            RandomSource random,
            ResourceKey<ConfiguredFeature<?, ?>> featureKey,
            ChunkGenerator generator
    ) {
        BlockState[] oldStates = new BlockState[]{
                level.getBlockState(base),
                level.getBlockState(base.east()),
                level.getBlockState(base.south()),
                level.getBlockState(base.east().south())
        };

        level.setBlock(base, Blocks.AIR.defaultBlockState(), 4);
        level.setBlock(base.east(), Blocks.AIR.defaultBlockState(), 4);
        level.setBlock(base.south(), Blocks.AIR.defaultBlockState(), 4);
        level.setBlock(base.east().south(), Blocks.AIR.defaultBlockState(), 4);

        ConfiguredFeature<?, ?> tree = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(featureKey);

        boolean placed = tree != null && tree.place(level, generator, random, base);

        if (!placed) {
            level.setBlock(base, oldStates[0], 4);
            level.setBlock(base.east(), oldStates[1], 4);
            level.setBlock(base.south(), oldStates[2], 4);
            level.setBlock(base.east().south(), oldStates[3], 4);
        }

        return placed;
    }

    private static boolean placeSingleFeature(
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            RandomSource random,
            ResourceKey<ConfiguredFeature<?, ?>> featureKey,
            ChunkGenerator generator
    ) {
        BlockState fluidState = level.getFluidState(pos).createLegacyBlock();
        level.setBlock(pos, fluidState, 4);

        ConfiguredFeature<?, ?> tree = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(featureKey);

        boolean placed = tree != null && tree.place(level, generator, random, pos);

        if (!placed) {
            level.setBlock(pos, oldState, 4);
        } else if (level.getBlockState(pos) == fluidState) {
            level.sendBlockUpdated(pos, oldState, fluidState, 2);
        }

        return placed;
    }
}
