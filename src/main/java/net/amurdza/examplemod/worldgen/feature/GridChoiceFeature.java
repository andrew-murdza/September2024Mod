package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class GridChoiceFeature extends Feature<GridChoiceConfig> {

    public GridChoiceFeature(Codec<GridChoiceConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GridChoiceConfig> context) {
        GridChoiceConfig cfg = context.config();

        WeightedGrid weightedGrid = buildWeightedGrid(cfg);

        if (weightedGrid.side <= 0) {
            return false;
        }

        List<BlockPos> positions = applyAllowedPlacementModifiers(context, cfg);

        boolean placedAnything = false;

        for (BlockPos pos : positions) {
            Holder<ConfiguredFeature<?, ?>> selected = getFeatureForGridPosition(pos, cfg, weightedGrid);

            if (selected == null) {
                continue;
            }

            placedAnything |= selected.value().place(
                    context.level(),
                    context.chunkGenerator(),
                    context.random(),
                    pos
            );
        }

        return placedAnything;
    }

    private WeightedGrid buildWeightedGrid(GridChoiceConfig cfg) {
        List<Holder<ConfiguredFeature<?, ?>>> expandedFeatures = new ArrayList<>();

        for (GridChoiceEntry entry : cfg.features) {
            for (int i = 0; i < entry.count; i++) {
                expandedFeatures.add(entry.feature);
            }
        }

        int weightedCount = expandedFeatures.size();

        if (weightedCount == 0 && cfg.defaultFeature == null) {
            return new WeightedGrid(List.of(), 0, 0);
        }

        int side = Math.max(1, (int) Math.ceil(Math.sqrt(weightedCount)));
        int cellCount = side * side;

        return new WeightedGrid(expandedFeatures, side, cellCount);
    }

    private Holder<ConfiguredFeature<?, ?>> getFeatureForGridPosition(
            BlockPos pos,
            GridChoiceConfig cfg,
            WeightedGrid weightedGrid
    ) {
        ChunkPos chunkPos = new ChunkPos(pos);

        int localChunkX = Math.floorMod(chunkPos.x, weightedGrid.side);
        int localChunkZ = Math.floorMod(chunkPos.z, weightedGrid.side);

        int index = localChunkZ * weightedGrid.side + localChunkX;

        if (index < weightedGrid.expandedFeatures.size()) {
            return weightedGrid.expandedFeatures.get(index);
        }

        return cfg.defaultFeature;
    }

    private List<BlockPos> applyAllowedPlacementModifiers(
            FeaturePlaceContext<GridChoiceConfig> context,
            GridChoiceConfig cfg
    ) {
        PlacementContext placementContext = new PlacementContext(
                context.level(),
                context.chunkGenerator(),
                Optional.empty()
        );

        RandomSource random = context.random();

        Stream<BlockPos> stream = Stream.of(applyOffsetType(context.origin(), random, cfg.offsetType));

        for (PlacementModifier modifier : cfg.placement) {
            stream = stream.flatMap(pos -> modifier.getPositions(
                    placementContext,
                    random,
                    pos
            ));
        }

        return stream.toList();
    }

    private BlockPos applyOffsetType(
            BlockPos origin,
            RandomSource random,
            GridChoiceConfig.OffsetType offsetType
    ) {
        return switch (offsetType) {
            case IN_SQUARE -> origin.offset(
                    random.nextInt(16),
                    0,
                    random.nextInt(16)
            );
            case CENTER -> origin.offset(8, 0, 8);
            case CORNER -> origin;
        };
    }

    private record WeightedGrid(
            List<Holder<ConfiguredFeature<?, ?>>> expandedFeatures,
            int side,
            int cellCount
    ) {
    }
}