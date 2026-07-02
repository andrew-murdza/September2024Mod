package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.GridChoiceConfig;
import net.amurdza.examplemod.worldgen.feature.configs.RegionalGridChoiceConfig;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RegionalGridChoiceFeature extends Feature<RegionalGridChoiceConfig> {
    private static final int FLOOR_SEARCH_RANGE = 8;

    public RegionalGridChoiceFeature(Codec<RegionalGridChoiceConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RegionalGridChoiceConfig> context) {
        RegionalGridChoiceConfig cfg = context.config();

        if (cfg.features.isEmpty()
                && cfg.groups.isEmpty()
                && cfg.pinnedFeatures.isEmpty()
                && cfg.minimumSections.isEmpty()
                && cfg.defaultFeature == null) {
            return false;
        }

        List<BlockPos> positions = applyAllowedPlacementModifiers(context, cfg);
        boolean placedAnything = false;

        for (BlockPos pos : positions) {
            if (isFullyRiverCornerFloorChunk(pos, cfg)) {
                continue;
            }

            Holder<ConfiguredFeature<?, ?>> selected = getFeatureForRegionalPosition(pos, context, cfg);

            if (selected == null) {
                continue;
            }

            placedAnything |= tryPlaceNearFloor(context, selected, pos);
        }

        return placedAnything;
    }

    private boolean isFullyRiverCornerFloorChunk(BlockPos selectionOrigin, RegionalGridChoiceConfig cfg) {
        if (cfg.offsetType != GridChoiceConfig.OffsetType.CORNER
                || cfg.regionWidth != 30
                || !cfg.salt.endsWith("_floor")) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(selectionOrigin);
        int regionChunkX = Math.floorMod(chunkPos.x, cfg.regionWidth);

        return regionChunkX > 0 && regionChunkX < 6;
    }

    private Holder<ConfiguredFeature<?, ?>> getFeatureForRegionalPosition(
            BlockPos pos,
            FeaturePlaceContext<RegionalGridChoiceConfig> context,
            RegionalGridChoiceConfig cfg
    ) {
        ChunkPos chunkPos = new ChunkPos(pos);

        int regionX = Math.floorDiv(chunkPos.x, cfg.regionWidth);
        int regionZ = Math.floorDiv(chunkPos.z, cfg.regionDepth);
        int localRegionX = Math.floorMod(chunkPos.x, cfg.regionWidth);
        int localRegionZ = Math.floorMod(chunkPos.z, cfg.regionDepth);

        int sectionX = localRegionX / cfg.sectionWidth;
        int sectionZ = localRegionZ / cfg.sectionDepth;
        int localSectionX = localRegionX % cfg.sectionWidth;
        int localSectionZ = localRegionZ % cfg.sectionDepth;
        int index = localSectionZ * cfg.sectionWidth + localSectionX;

        int sectionCellCount = cfg.sectionWidth * cfg.sectionDepth;
        List<Holder<ConfiguredFeature<?, ?>>> shuffledFeatures = buildShuffledFeatures(
                cfg,
                sectionCellCount,
                sectionX,
                sectionZ,
                mixSeed(context.level().getSeed(), regionX, regionZ, sectionX, sectionZ, cfg.salt)
        );

        if (index < shuffledFeatures.size() && shuffledFeatures.get(index) != null) {
            return shuffledFeatures.get(index);
        }

        return cfg.defaultFeature;
    }

    private List<Holder<ConfiguredFeature<?, ?>>> buildShuffledFeatures(
            RegionalGridChoiceConfig cfg,
            int sectionCellCount,
            int sectionX,
            int sectionZ,
            long seed
    ) {
        List<Holder<ConfiguredFeature<?, ?>>> expandedFeatures = new ArrayList<>();

        for (GridChoiceEntry entry : cfg.features) {
            for (int i = 0; i < entry.count; i++) {
                expandedFeatures.add(entry.feature);
            }
        }

        for (int groupIndex = 0; groupIndex < cfg.groups.size(); groupIndex++) {
            addGroupFeatures(expandedFeatures, cfg.groups.get(groupIndex), seed + groupIndex * 1299721L);
        }

        List<Holder<ConfiguredFeature<?, ?>>> shuffledFeatures = new ArrayList<>(Collections.nCopies(sectionCellCount, null));

        for (int pinnedIndex = 0; pinnedIndex < cfg.pinnedFeatures.size(); pinnedIndex++) {
            RegionalGridChoicePinnedEntry pinnedEntry = cfg.pinnedFeatures.get(pinnedIndex);
            List<Integer> candidates = getMatchingCellIndexes(cfg, pinnedEntry, sectionX, sectionZ);

            int assigned = assignFeatureToCells(
                    shuffledFeatures,
                    expandedFeatures,
                    pinnedEntry.feature,
                    pinnedEntry.count,
                    candidates,
                    cfg,
                    seed + 14983L + pinnedIndex * 8191L
            );

            for (int i = assigned; i < pinnedEntry.count; i++) {
                removeOne(expandedFeatures, pinnedEntry.feature);
            }
        }

        for (int minimumIndex = 0; minimumIndex < cfg.minimumSections.size(); minimumIndex++) {
            addMinimumSectionFeatures(
                    shuffledFeatures,
                    expandedFeatures,
                    cfg,
                    cfg.minimumSections.get(minimumIndex),
                    seed + 27823L + minimumIndex * 6151L
            );
        }

        RandomSource random = RandomSource.create(seed);
        fillRemainingCells(shuffledFeatures, expandedFeatures, cfg, random);

        return shuffledFeatures;
    }

    private void addMinimumSectionFeatures(
            List<Holder<ConfiguredFeature<?, ?>>> sectionFeatures,
            List<Holder<ConfiguredFeature<?, ?>>> remainingFeatures,
            RegionalGridChoiceConfig cfg,
            RegionalGridChoiceMinimumSection minimumSection,
            long seed
    ) {
        int childSectionCountX = cfg.sectionWidth / minimumSection.sectionWidth;
        int childSectionCountZ = cfg.sectionDepth / minimumSection.sectionDepth;

        for (int childSectionZ = 0; childSectionZ < childSectionCountZ; childSectionZ++) {
            for (int childSectionX = 0; childSectionX < childSectionCountX; childSectionX++) {
                int minX = childSectionX * minimumSection.sectionWidth;
                int minZ = childSectionZ * minimumSection.sectionDepth;
                List<Integer> candidates = getCellIndexesInRectangle(
                        cfg,
                        minX,
                        minZ,
                        minimumSection.sectionWidth,
                        minimumSection.sectionDepth
                );

                for (int entryIndex = 0; entryIndex < minimumSection.features.size(); entryIndex++) {
                    GridChoiceEntry entry = minimumSection.features.get(entryIndex);

                    assignFeatureToCells(
                            sectionFeatures,
                            remainingFeatures,
                            entry.feature,
                            entry.count,
                            candidates,
                            cfg,
                            seed + childSectionX * 341873128712L + childSectionZ * 132897987541L + entryIndex * 31L
                    );
                }
            }
        }
    }

    private int assignFeatureToCells(
            List<Holder<ConfiguredFeature<?, ?>>> sectionFeatures,
            List<Holder<ConfiguredFeature<?, ?>>> remainingFeatures,
            Holder<ConfiguredFeature<?, ?>> feature,
            int count,
            List<Integer> candidateIndexes,
            RegionalGridChoiceConfig cfg,
            long seed
    ) {
        List<Integer> shuffledCandidates = new ArrayList<>();

        for (int index : candidateIndexes) {
            if (sectionFeatures.get(index) == null && isFeatureAllowedAt(cfg, feature, index)) {
                shuffledCandidates.add(index);
            }
        }

        shuffleIndexes(shuffledCandidates, seed);

        int placements = Math.min(count, shuffledCandidates.size());

        for (int i = 0; i < placements; i++) {
            int index = shuffledCandidates.get(i);
            sectionFeatures.set(index, feature);
            removeOne(remainingFeatures, feature);
        }

        return placements;
    }

    private void fillRemainingCells(
            List<Holder<ConfiguredFeature<?, ?>>> sectionFeatures,
            List<Holder<ConfiguredFeature<?, ?>>> remainingFeatures,
            RegionalGridChoiceConfig cfg,
            RandomSource random
    ) {
        List<Integer> freeIndexes = new ArrayList<>();

        for (int i = 0; i < sectionFeatures.size(); i++) {
            if (sectionFeatures.get(i) == null) {
                freeIndexes.add(i);
            }
        }

        shuffleIndexes(freeIndexes, random.nextLong());
        freeIndexes.sort(Comparator.comparingInt(index -> countAllowedFeatures(cfg, remainingFeatures, index)));

        for (int index : freeIndexes) {
            int featureIndex = pickAllowedFeatureIndex(cfg, remainingFeatures, index, random);

            if (featureIndex < 0) {
                continue;
            }

            sectionFeatures.set(index, remainingFeatures.remove(featureIndex));
        }
    }

    private int pickAllowedFeatureIndex(
            RegionalGridChoiceConfig cfg,
            List<Holder<ConfiguredFeature<?, ?>>> remainingFeatures,
            int cellIndex,
            RandomSource random
    ) {
        List<Integer> candidates = new ArrayList<>();

        for (int i = 0; i < remainingFeatures.size(); i++) {
            if (isFeatureAllowedAt(cfg, remainingFeatures.get(i), cellIndex)) {
                candidates.add(i);
            }
        }

        if (candidates.isEmpty()) {
            return -1;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private int countAllowedFeatures(
            RegionalGridChoiceConfig cfg,
            List<Holder<ConfiguredFeature<?, ?>>> remainingFeatures,
            int cellIndex
    ) {
        int count = 0;

        for (Holder<ConfiguredFeature<?, ?>> feature : remainingFeatures) {
            if (isFeatureAllowedAt(cfg, feature, cellIndex)) {
                count++;
            }
        }

        return count;
    }

    private boolean isFeatureAllowedAt(
            RegionalGridChoiceConfig cfg,
            Holder<ConfiguredFeature<?, ?>> feature,
            int cellIndex
    ) {
        int localX = cellIndex % cfg.sectionWidth;
        int localZ = cellIndex / cfg.sectionWidth;

        for (RegionalGridChoiceCellRule rule : cfg.cellRules) {
            if (rule.matches(localX, localZ) && !rule.allowedFeatures.contains(feature)) {
                return false;
            }
        }

        return true;
    }

    private List<Integer> getMatchingCellIndexes(
            RegionalGridChoiceConfig cfg,
            RegionalGridChoicePinnedEntry pinnedEntry,
            int sectionX,
            int sectionZ
    ) {
        List<Integer> indexes = new ArrayList<>();
        int sectionRegionMinX = sectionX * cfg.sectionWidth;
        int sectionRegionMinZ = sectionZ * cfg.sectionDepth;

        for (int z = 0; z < cfg.sectionDepth; z++) {
            for (int x = 0; x < cfg.sectionWidth; x++) {
                if (pinnedEntry.matches(x, z, sectionRegionMinX + x, sectionRegionMinZ + z)) {
                    indexes.add(z * cfg.sectionWidth + x);
                }
            }
        }

        return indexes;
    }

    private List<Integer> getCellIndexesInRectangle(
            RegionalGridChoiceConfig cfg,
            int minX,
            int minZ,
            int width,
            int depth
    ) {
        List<Integer> indexes = new ArrayList<>();

        for (int z = minZ; z < Math.min(cfg.sectionDepth, minZ + depth); z++) {
            for (int x = minX; x < Math.min(cfg.sectionWidth, minX + width); x++) {
                indexes.add(z * cfg.sectionWidth + x);
            }
        }

        return indexes;
    }

    private boolean removeOne(
            List<Holder<ConfiguredFeature<?, ?>>> remainingFeatures,
            Holder<ConfiguredFeature<?, ?>> feature
    ) {
        for (int i = 0; i < remainingFeatures.size(); i++) {
            if (remainingFeatures.get(i).equals(feature)) {
                remainingFeatures.remove(i);
                return true;
            }
        }

        return false;
    }

    private void shuffleIndexes(List<Integer> indexes, long seed) {
        RandomSource random = RandomSource.create(seed);

        for (int i = indexes.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swapped = indexes.get(i);
            indexes.set(i, indexes.get(j));
            indexes.set(j, swapped);
        }
    }

    private void addGroupFeatures(
            List<Holder<ConfiguredFeature<?, ?>>> expandedFeatures,
            RegionalGridChoiceGroup group,
            long seed
    ) {
        List<Holder<ConfiguredFeature<?, ?>>> shuffledGroupFeatures = new ArrayList<>(group.features);

        RandomSource random = RandomSource.create(seed);
        for (int i = shuffledGroupFeatures.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Holder<ConfiguredFeature<?, ?>> swapped = shuffledGroupFeatures.get(i);
            shuffledGroupFeatures.set(i, shuffledGroupFeatures.get(j));
            shuffledGroupFeatures.set(j, swapped);
        }

        int extraFeatureCount = Math.min(group.extraFeatureCount, shuffledGroupFeatures.size());

        for (Holder<ConfiguredFeature<?, ?>> feature : group.features) {
            int count = group.count;

            for (int i = 0; i < extraFeatureCount; i++) {
                if (shuffledGroupFeatures.get(i).equals(feature)) {
                    count += group.extraCount;
                    break;
                }
            }

            for (int i = 0; i < Math.max(0, count); i++) {
                expandedFeatures.add(feature);
            }
        }
    }

    private long mixSeed(
            long worldSeed,
            int regionX,
            int regionZ,
            int sectionX,
            int sectionZ,
            String salt
    ) {
        long seed = worldSeed;
        seed ^= (long) regionX * 341873128712L;
        seed ^= (long) regionZ * 132897987541L;
        seed ^= (long) sectionX * 42317861L;
        seed ^= (long) sectionZ * 73428767L;
        seed ^= (long) salt.hashCode() * -7046029254386353131L;
        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= seed >>> 33;
        return seed;
    }

    private List<BlockPos> applyAllowedPlacementModifiers(
            FeaturePlaceContext<RegionalGridChoiceConfig> context,
            RegionalGridChoiceConfig cfg
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
            case CENTER -> origin.offset(7, 0, 7);
            case CORNER -> origin;
        };
    }

    private boolean tryPlaceNearFloor(
            FeaturePlaceContext<RegionalGridChoiceConfig> context,
            Holder<ConfiguredFeature<?, ?>> selected,
            BlockPos origin
    ) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int yOffset = 0; yOffset <= FLOOR_SEARCH_RANGE; yOffset++) {
            mutablePos.set(origin.getX(), origin.getY() + yOffset, origin.getZ());
            if (selected.value().place(context.level(), context.chunkGenerator(), context.random(), mutablePos)) {
                return true;
            }

            if (yOffset == 0) {
                continue;
            }

            mutablePos.set(origin.getX(), origin.getY() - yOffset, origin.getZ());
            if (selected.value().place(context.level(), context.chunkGenerator(), context.random(), mutablePos)) {
                return true;
            }
        }

        return false;
    }
}
