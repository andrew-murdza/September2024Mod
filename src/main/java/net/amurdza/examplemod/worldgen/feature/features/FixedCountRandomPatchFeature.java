package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.FixedCountRandomPatchConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import snownee.snow.Hooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixedCountRandomPatchFeature extends Feature<FixedCountRandomPatchConfiguration> {
    private static final int FLOOR_SEARCH_RANGE = 8;
    private static final int MAX_FALLBACK_RINGS = 8;

    public FixedCountRandomPatchFeature(Codec<FixedCountRandomPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FixedCountRandomPatchConfiguration> context) {
        FixedCountRandomPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();

        int targetPlacements = config.count();
        int successfulPlacements = placeFromOffsets(
                context,
                config,
                random,
                level,
                origin,
                buildShuffledOffsets(config.xzSpread(), random),
                targetPlacements
        );

        for (int radius = config.xzSpread() + 1;
             successfulPlacements < targetPlacements
                     && radius <= config.xzSpread() + MAX_FALLBACK_RINGS;
             radius++) {
            successfulPlacements += placeFromOffsets(
                    context,
                    config,
                    random,
                    level,
                    origin,
                    buildShuffledRing(radius, random),
                    targetPlacements - successfulPlacements
            );
        }

        return successfulPlacements > 0;
    }

    private int placeFromOffsets(
            FeaturePlaceContext<FixedCountRandomPatchConfiguration> context,
            FixedCountRandomPatchConfiguration config,
            RandomSource random,
            WorldGenLevel level,
            BlockPos origin,
            List<CellOffset> offsets,
            int targetPlacements
    ) {
        int successfulPlacements = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (CellOffset offset : offsets) {
            if (successfulPlacements >= targetPlacements) {
                break;
            }

            mutablePos.setWithOffset(origin, offset.x(), 0, offset.z());

            if (tryPlaceNearFloor(context, config, random, level, mutablePos)) {
                successfulPlacements++;
            }
        }

        return successfulPlacements;
    }

    private List<CellOffset> buildShuffledOffsets(int xzSpread, RandomSource random) {
        List<CellOffset> offsets = new ArrayList<>((xzSpread * 2 + 1) * (xzSpread * 2 + 1));

        for (int x = -xzSpread; x <= xzSpread; x++) {
            for (int z = -xzSpread; z <= xzSpread; z++) {
                offsets.add(new CellOffset(x, z));
            }
        }

        shuffle(offsets, random);

        return offsets;
    }

    private List<CellOffset> buildShuffledRing(int radius, RandomSource random) {
        List<CellOffset> offsets = new ArrayList<>(radius * 8);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) == radius) {
                    offsets.add(new CellOffset(x, z));
                }
            }
        }

        shuffle(offsets, random);
        return offsets;
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int i = values.size() - 1; i > 0; i--) {
            Collections.swap(values, i, random.nextInt(i + 1));
        }
    }

    private boolean tryPlaceNearFloor(
            FeaturePlaceContext<FixedCountRandomPatchConfiguration> context,
            FixedCountRandomPatchConfiguration config,
            RandomSource random,
            WorldGenLevel level,
            BlockPos origin
    ) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int yOffset = 0; yOffset <= FLOOR_SEARCH_RANGE; yOffset++) {
            mutablePos.set(origin.getX(), origin.getY() + yOffset, origin.getZ());
            if (tryPlace(context, config, random, level, mutablePos)) {
                return true;
            }

            if (yOffset == 0) {
                continue;
            }

            mutablePos.set(origin.getX(), origin.getY() - yOffset, origin.getZ());
            if (tryPlace(context, config, random, level, mutablePos)) {
                return true;
            }
        }

        return false;
    }

    private boolean tryPlace(
            FeaturePlaceContext<FixedCountRandomPatchConfiguration> context,
            FixedCountRandomPatchConfiguration config,
            RandomSource random,
            WorldGenLevel level,
            BlockPos pos
    ) {
        if (!canTryLandPatch(config, level, pos)) {
            return false;
        }

        if (!config.ringSide().allows(pos)) {
            return false;
        }

        BlockState oldState = level.getBlockState(pos);
        int snowLayers = oldState.hasProperty(SnowLayerBlock.LAYERS)
                ? oldState.getValue(SnowLayerBlock.LAYERS)
                : 0;

        if (!config.feature().value().place(level, context.chunkGenerator(), random, pos)) {
            return false;
        }

        BlockState placedState = level.getBlockState(pos);
        if (snowLayers > 0 && placedState.getBlock() instanceof SweetBerryBushBlock) {
            Hooks.convert(level, pos, placedState, snowLayers, Block.UPDATE_CLIENTS, true);
        }

        return true;
    }

    private static boolean canTryLandPatch(
            FixedCountRandomPatchConfiguration config,
            WorldGenLevel level,
            BlockPos pos
    ) {
        if (!config.landOnly()) {
            return true;
        }

        BlockPos groundPos = pos.below();

        return level.getFluidState(pos).isEmpty()
                && level.getFluidState(groundPos).isEmpty()
                && !level.getBlockState(groundPos).isAir();
    }

    private record CellOffset(int x, int z) {
    }
}
