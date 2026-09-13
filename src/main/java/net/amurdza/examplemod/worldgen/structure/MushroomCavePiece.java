package net.amurdza.examplemod.worldgen.structure;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import net.amurdza.examplemod.config.CaveStructureConfig;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.worldgen.MushroomCaveRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MushroomCavePiece extends AbstractSpiralCavePiece {
    private static final double YAW_START = Math.PI * 1.5D;
    private static final double FULL_TURN = Math.PI * 2.0D;
    private static final int UPPER_GRASS_SURFACE_DEPTH = 6;
    private static final double MUSHROOM_SECTION_START_TURNS =
            CaveStructureConfig.MUSHROOM_CAVE_SECTION_START_TURNS;
    private static final double MUSHROOM_SECTION_END_TURNS =
            CaveStructureConfig.MUSHROOM_CAVE_SECTION_END_TURNS;
    private static final int[][] BERRY_PATCH_OFFSETS = new int[][] {
            {0, 0},
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {-1, 1},
            {1, -1},
            {-1, -1}
    };
    private static final double[] SWEET_BERRY_PATCH_PATH_FRACTIONS = new double[] {0.18D};
    private static final double[] BLUE_BERRY_PATCH_PATH_FRACTIONS = new double[] {0.38D};
    private static final double MOOSE_GROUP_PATH_FRACTION = 0.12D;
    private static final double POLAR_BEAR_GROUP_BASE_PATH_FRACTION = 0.64D;
    private static final double INTERMEDIATE_GROUP_POLAR_ARC_SHIFT = 20.0D;
    private static final double POLAR_BEAR_UPWARD_ARC_SHIFT = 36.0D;
    private static final double GOAT_POSITION_BETWEEN_MOOSE_AND_POLAR_BEAR = 0.25D;
    private static final double RABBIT_POSITION_BETWEEN_MOOSE_AND_POLAR_BEAR = 0.5D;
    private static final double FOX_POSITION_BETWEEN_MOOSE_AND_POLAR_BEAR = 0.75D;
    private static final double POLAR_BEAR_RIVERWARD_SHIFT_FRACTION =
            10.0D / CaveStructureConfig.MUSHROOM_CAVE_HORIZONTAL_RADIUS;
    private static final int ANIMAL_GROUP_WIDTH = 10;
    private static final int BERRY_PATCH_RADIUS = 1;
    private static final int BERRY_TREE_CLEARANCE = 3;
    private static final int BERRY_CENTER_SEARCH_RADIUS = 4;
    private static final int BERRY_LOG_SCAN_HEIGHT = 24;

    public MushroomCavePiece(
            BlockPos origin,
            long seed,
            int endY,
            int endX,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(
                ModStructures.MUSHROOM_CAVE_PIECE.get(),
                origin,
                seed,
                endY,
                endX,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius,
                placedFeatures
        );

    }

    @SuppressWarnings("unused")
    public MushroomCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.MUSHROOM_CAVE_PIECE.get(), tag);
    }

    @Override
    protected void addSubclassSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
    }

    @Override
    protected void decorateCaveFloor(WorldGenLevel level, BlockPos top) {
        int floorY = top.getY();

        if (floorY == finalEllipsoidFloorTopY()) {
            setBlock(level, top, Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(), Blocks.CALCITE);
            setBlock(level, top.below(2), Blocks.CALCITE);
            setBlock(level, top.below(3), Blocks.CALCITE);
            setBlock(level, top.below(4), Blocks.SMOOTH_BASALT);
        }

        else if (floorY == finalEllipsoidFloorTopY() - 1) {
            setBlock(level, top, Blocks.CALCITE);
            setBlock(level, top.below(), Blocks.CALCITE);
            setBlock(level, top.below(2), Blocks.CALCITE);
            setBlock(level, top.below(3), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(4), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(5), Blocks.SMOOTH_BASALT);
        }

        else if (floorY == finalEllipsoidFloorTopY() - 2) {
            setBlock(level, top, bordersOneBlockHigher(top) ? Blocks.BUDDING_AMETHYST : Blocks.AMETHYST_BLOCK);
            setBlock(level, top.below(), Blocks.CALCITE);
            setBlock(level, top.below(2), Blocks.CALCITE);
            setBlock(level, top.below(3), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(4), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(5), Blocks.SMOOTH_BASALT);
        }

        else if (isMushroomFloor(top)) {
            setBlock(level, top, Blocks.MYCELIUM);
        }

        replaceExposedUndesirableBoundaryBlock(level, top, overworldBoundaryReplacement(level, top), false);
        replaceNaturalClayBoundary(level, top);
    }

    @Override
    protected void afterCarvingBeforeFeatures(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        restoreFinalMushroomFloorSurfaces(level, box, chunkPos, chunkEllipsoids);

        int minX = Math.max(box.minX(), chunkPos.getMinBlockX());
        int maxX = Math.min(box.maxX(), chunkPos.getMaxBlockX());
        int minZ = Math.max(box.minZ(), chunkPos.getMinBlockZ());
        int maxZ = Math.min(box.maxZ(), chunkPos.getMaxBlockZ());
        int upperGrassAirY = Mth.ceil(this.origin.getY() - this.verticalRadius);
        int minFloorAirY = Math.max(
                Mth.ceil(mushroomSectionStartFloorAirY()),
                Math.max(box.minY(), level.getMinBuildHeight()) + UPPER_GRASS_SURFACE_DEPTH
        );
        int maxFloorAirY = Math.min(
                upperGrassAirY,
                Math.min(box.maxY(), level.getMaxBuildHeight() - 1)
        );
        if (minX > maxX || minZ > maxZ || minFloorAirY > maxFloorAirY) {
            applyMudToBuriedRiverWalls(level, box, chunkPos, chunkEllipsoids);
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!isInsideCaveFootprint(x, z)) {
                    continue;
                }

                cursor.set(x, upperGrassAirY, z);
                if (!box.isInside(cursor) || !level.getBlockState(cursor).isAir()) {
                    continue;
                }

                List<ColumnCarveInterval> carveIntervals = columnCarveIntervals(
                        x,
                        z,
                        minFloorAirY,
                        maxFloorAirY,
                        chunkEllipsoids
                );
                if (carveIntervals.isEmpty()) {
                    continue;
                }

                for (ColumnCarveInterval carveInterval : carveIntervals) {
                    if (carveInterval.minY() > upperGrassAirY
                            || carveInterval.maxY() < upperGrassAirY) {
                        continue;
                    }

                    for (int floorAirY = carveInterval.minY();
                         floorAirY <= upperGrassAirY;
                         floorAirY++) {
                        cursor.set(x, floorAirY, z);
                        if (!level.getBlockState(cursor).isAir()) {
                            continue;
                        }

                        cursor.move(Direction.DOWN);
                        if (!level.getBlockState(cursor).isSolid()) {
                            continue;
                        }

                        restoreUpperGrassSurfaceLayers(
                                level,
                                box,
                                cursor,
                                x,
                                floorAirY - 1,
                                z
                        );
                        break;
                    }
                    break;
                }
            }
        }

        applyMudToBuriedRiverWalls(level, box, chunkPos, chunkEllipsoids);
    }

    private void restoreFinalMushroomFloorSurfaces(
            WorldGenLevel level,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        int minX = Math.max(box.minX(), chunkPos.getMinBlockX());
        int maxX = Math.min(box.maxX(), chunkPos.getMaxBlockX());
        int minZ = Math.max(box.minZ(), chunkPos.getMinBlockZ());
        int maxZ = Math.min(box.maxZ(), chunkPos.getMaxBlockZ());
        int minY = Math.max(box.minY(), level.getMinBuildHeight());
        int maxY = Math.min(box.maxY(), level.getMaxBuildHeight() - 1);
        if (minX > maxX || minZ > maxZ || minY > maxY) {
            return;
        }

        BlockPos.MutableBlockPos airPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos floorPos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!isInsideCaveFootprint(x, z)) {
                    continue;
                }

                for (ColumnCarveInterval interval : columnCarveIntervals(
                        x,
                        z,
                        minY,
                        maxY,
                        chunkEllipsoids
                )) {
                    for (int y = interval.minY(); y <= interval.maxY(); y++) {
                        airPos.set(x, y, z);
                        floorPos.set(x, y - 1, z);
                        if (!level.getBlockState(airPos).isAir()
                                || !box.isInside(floorPos)
                                || !canReplace(level, floorPos)) {
                            continue;
                        }

                        if (isMushroomFloor(floorPos)) {
                            level.setBlock(
                                    floorPos,
                                    Blocks.MYCELIUM.defaultBlockState(),
                                    Block.UPDATE_CLIENTS
                            );
                        }
                        break;
                    }
                }
            }
        }
    }

    private boolean isInsideCaveFootprint(int x, int z) {
        double tunnelRadius = getTunnelHorizontalRadius();
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double dx = x + 0.5D - this.pillarCenterX;
        double dz = z + 0.5D - this.pillarCenterZ;
        double radialDistance = Math.sqrt(dx * dx + dz * dz);
        double pillarRadius = pathRadius - tunnelRadius;

        return radialDistance <= pillarRadius + 2.0D
                || Math.abs(radialDistance - pathRadius) <= tunnelRadius + 2.0D;
    }

    private List<ColumnCarveInterval> columnCarveIntervals(
            int x,
            int z,
            int minY,
            int maxY,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        List<ColumnCarveInterval> intervals = new ArrayList<>();

        for (CarveEllipsoid ellipsoid : chunkEllipsoids) {
            double relativeX = (x + 0.5D - ellipsoid.centerX())
                    / ellipsoid.horizontalRadius();
            double relativeZ = (z + 0.5D - ellipsoid.centerZ())
                    / ellipsoid.horizontalRadius();
            double remainingVerticalShape = 1.0D
                    - relativeX * relativeX
                    - relativeZ * relativeZ;
            if (remainingVerticalShape <= 0.0D) {
                continue;
            }

            double verticalScale = Math.pow(remainingVerticalShape, 0.25D);
            double lowerCenterY = ellipsoid.centerY()
                    - ellipsoid.lowerVerticalRadius() * verticalScale;
            double upperCenterY = ellipsoid.centerY()
                    + ellipsoid.upperVerticalRadius() * verticalScale;
            int intervalMinY = Mth.floor(lowerCenterY - 0.5D) + 1;
            int intervalMaxY = Mth.ceil(upperCenterY - 0.5D) - 1;

            intervalMinY = Math.max(
                    intervalMinY,
                    Math.max(minY, mainAirCarveMinY(this.endY))
            );
            intervalMaxY = Math.min(
                    intervalMaxY,
                    Math.min(maxY, ellipsoid.maxCarveY())
            );
            if (intervalMinY <= intervalMaxY) {
                intervals.add(new ColumnCarveInterval(intervalMinY, intervalMaxY));
            }
        }

        return mergeColumnIntervals(intervals);
    }

    private List<ColumnCarveInterval> mergeColumnIntervals(
            List<ColumnCarveInterval> intervals
    ) {
        if (intervals.size() < 2) {
            return intervals;
        }

        intervals.sort((left, right) -> Integer.compare(left.minY(), right.minY()));
        List<ColumnCarveInterval> merged = new ArrayList<>();
        ColumnCarveInterval current = intervals.get(0);

        for (int index = 1; index < intervals.size(); index++) {
            ColumnCarveInterval next = intervals.get(index);
            if (next.minY() <= current.maxY() + 1) {
                current = new ColumnCarveInterval(
                        current.minY(),
                        Math.max(current.maxY(), next.maxY())
                );
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    private void restoreUpperGrassSurfaceLayers(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos.MutableBlockPos cursor,
            int x,
            int surfaceY,
            int z
    ) {
        for (int depth = 0; depth < UPPER_GRASS_SURFACE_DEPTH; depth++) {
            int y = surfaceY - depth;
            cursor.set(x, y, z);
            if (!box.isInside(cursor)) {
                return;
            }

            Block intendedLayer = upperGrassSurfaceLayer(depth);
            BlockState state = level.getBlockState(cursor);
            if (state.is(intendedLayer)) {
                continue;
            }

            if (state.isAir()) {
                /*
                 * The surface was found from the final merged carve union. Fill
                 * any deeper air in its six-block substrate so overlapping
                 * ellipsoids cannot strip off the grass or dirt layer.
                 */
                if (depth == 0) {
                    return;
                }
                continue;
            }

            if (!canReplace(state) || isProtectedMushroomSurfaceLayer(state)) {
                return;
            }
        }

        for (int depth = UPPER_GRASS_SURFACE_DEPTH - 1; depth >= 0; depth--) {
            cursor.set(x, surfaceY - depth, z);
            level.setBlock(
                    cursor,
                    upperGrassSurfaceLayer(depth).defaultBlockState(),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    private Block upperGrassSurfaceLayer(int depth) {
        return switch (depth) {
            case 0 -> Blocks.GRASS_BLOCK;
            case 1 -> Blocks.DIRT;
            case 2, 3, 4, 5 -> Blocks.STONE;
            default -> throw new IllegalArgumentException("Unexpected upper surface depth: " + depth);
        };
    }

    private boolean isProtectedMushroomSurfaceLayer(BlockState state) {
        return state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.AMETHYST_BLOCK)
                || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_CATALYST)
                || state.is(Blocks.SCULK_SENSOR)
                || state.is(Blocks.SCULK_SHRIEKER)
                || state.is(Blocks.END_STONE);
    }

    private record ColumnCarveInterval(int minY, int maxY) {
    }

    private boolean isMushroomFloor(BlockPos floorPos) {
        return MushroomCaveRegion.containsFloorForExactCave(
                floorPos,
                this.origin.getX(),
                this.origin.getY(),
                this.origin.getZ(),
                this.horizontalRadius,
                this.verticalRadius,
                this.centralPillarDiameter,
                this.minFloorThickness,
                this.liquidDepth,
                this.liquidRadius
        );
    }

    private double mushroomSectionStartFloorAirY() {
        return this.origin.getY()
                - this.verticalRadius
                - MUSHROOM_SECTION_START_TURNS * dropPerTurn();
    }

    private double dropPerTurn() {
        return this.verticalRadius * 2.0D
                + this.minFloorThickness
                + 1.0D
                + this.liquidDepth;
    }

    private int finalEllipsoidFloorTopY() {
        double finalCenterY = this.origin.getY() - 2.0D * dropPerTurn();
        return (int) Math.ceil(finalCenterY - this.verticalRadius) + 3;
    }

    @Override
    protected void afterCarveBlock(WorldGenLevel level, BlockPos carvedPos, BlockState oldState) {
        if (!oldState.is(Blocks.MYCELIUM)) {
            return;
        }

        if (isMushroomFloor(carvedPos.below())) {
            return;
        }

        restoreClearedMushroomSubstrate(level, carvedPos.below());
        restoreClearedMushroomSubstrate(level, carvedPos.below(2));
        restoreClearedMushroomSubstrate(level, carvedPos.below(3));
    }

    private void restoreClearedMushroomSubstrate(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.is(Blocks.MYCELIUM) && !state.is(Blocks.DIRT) && !state.is(Blocks.CLAY)) {
            return;
        }

        Block replacement = pos.getY() >= 0 ? Blocks.STONE : Blocks.DEEPSLATE;
        level.setBlock(pos, replacement.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private boolean bordersOneBlockHigher(BlockPos top) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!anyMainEllipsoidWouldCarve(top.offset(dx, 1, dz))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected boolean canReplace(BlockState state) {
        return super.canReplace(state)
                && !state.is(Blocks.SPRUCE_LEAVES)
                && !state.is(Blocks.SPRUCE_LOG)
                && !state.is(Blocks.SPRUCE_WOOD)
                && !state.is(Blocks.STRIPPED_SPRUCE_LOG)
                && !state.is(Blocks.STRIPPED_SPRUCE_WOOD)
                && !state.is(BlockTags.LEAVES);
    }

    @Override
    protected boolean canReplaceWithFluid(BlockState state) {
        return super.canReplaceWithFluid(state)
                && !state.is(Blocks.SPRUCE_LEAVES)
                && !state.is(Blocks.SPRUCE_LOG)
                && !state.is(Blocks.SPRUCE_WOOD)
                && !state.is(Blocks.STRIPPED_SPRUCE_LOG)
                && !state.is(Blocks.STRIPPED_SPRUCE_WOOD)
                && !state.is(BlockTags.LEAVES);
    }

    @Override
    protected void decorateRiverFloor(WorldGenLevel level, BlockPos surfacePos) {
        setBlock(level, surfacePos, Blocks.MUD);
        setBlock(level, surfacePos.below(), Blocks.MUD);
        setBlock(level, surfacePos.below(2), Blocks.MUD);
        setBlock(level, surfacePos.below(3), Blocks.MUD);
    }

    @Override
    protected void decorateCaveWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {
        replaceExposedUndesirableBoundaryBlock(level, wallPos, overworldBoundaryReplacement(level, wallPos), true);
        replaceNaturalClayBoundary(level, wallPos);
    }

    @Override
    protected void decorateRiverWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {
        replaceExposedUndesirableBoundaryBlock(level, wallPos, overworldBoundaryReplacement(level, wallPos), true);
        replaceNaturalClayBoundary(level, wallPos);
    }

    @Override
    protected void decorateCaveCeiling(WorldGenLevel level, BlockPos ceilingPos) {
        replaceExposedUndesirableBoundaryBlock(level, ceilingPos, overworldBoundaryReplacement(level, ceilingPos), true);
        replaceNaturalClayBoundary(level, ceilingPos);
    }

    private void replaceNaturalClayBoundary(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.CLAY)) {
            return;
        }

        level.setBlock(
                pos,
                overworldStoneReplacement(pos, state).defaultBlockState(),
                Block.UPDATE_CLIENTS
        );
    }

    private void applyMudToBuriedRiverWalls(
            WorldGenLevel level,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        for (long packedPos : collectFinalRiverWallPositions(
                box,
                chunkPos,
                chunkEllipsoids
        )) {
            BlockPos pos = BlockPos.of(packedPos);
            if (!canReplace(level, pos)) {
                continue;
            }

            replaceExposedUndesirableBoundaryBlock(
                    level,
                    pos,
                    overworldBoundaryReplacement(level, pos),
                    true
            );
            replaceNaturalClayBoundary(level, pos);

            BlockState state = level.getBlockState(pos);
            BlockState aboveState = level.getBlockState(pos.above());
            if ((state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE))
                    && (aboveState.isSolid() || !aboveState.getFluidState().isEmpty())) {
                level.setBlock(pos, Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private Set<Long> collectFinalRiverWallPositions(
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        Set<Long> wallPositions = new HashSet<>();
        if (this.liquidRadius <= 0.0F || this.liquidDepth <= 0.0F) {
            return wallPositions;
        }

        BoundingBox pieceBox = this.getBoundingBox();
        int ownedMinX = Math.max(
                chunkPos.getMinBlockX(),
                Math.max(box.minX(), pieceBox.minX())
        );
        int ownedMaxX = Math.min(
                chunkPos.getMaxBlockX(),
                Math.min(box.maxX(), pieceBox.maxX())
        );
        int ownedMinY = Math.max(box.minY(), pieceBox.minY());
        int ownedMaxY = Math.min(box.maxY(), pieceBox.maxY());
        int ownedMinZ = Math.max(
                chunkPos.getMinBlockZ(),
                Math.max(box.minZ(), pieceBox.minZ())
        );
        int ownedMaxZ = Math.min(
                chunkPos.getMaxBlockZ(),
                Math.min(box.maxZ(), pieceBox.maxZ())
        );
        if (ownedMinX > ownedMaxX || ownedMinY > ownedMaxY || ownedMinZ > ownedMaxZ) {
            return wallPositions;
        }

        BoundingBox ownedBox = new BoundingBox(
                ownedMinX,
                ownedMinY,
                ownedMinZ,
                ownedMaxX,
                ownedMaxY,
                ownedMaxZ
        );

        BlockPos.MutableBlockPos wallPos = new BlockPos.MutableBlockPos();

        /*
         * Rebuild walls from the same mathematical profile that carved the
         * river. The one-block halo predicts fluid beyond the owned chunk, but
         * only wall blocks inside this chunk are collected and changed.
         */
        for (int x = ownedBox.minX() - 1; x <= ownedBox.maxX() + 1; x++) {
            for (int z = ownedBox.minZ() - 1; z <= ownedBox.maxZ() + 1; z++) {
                for (RiverColumnProfile profile : riverColumnProfiles(
                        x,
                        z,
                        chunkEllipsoids
                )) {
                    int minY = Math.max(profile.minY(), ownedBox.minY());
                    int maxY = Math.min(profile.maxY(), ownedBox.maxY());
                    for (int y = minY; y <= maxY; y++) {
                        if (getRiverFluidState(profile.source(), x, y, z) == null) {
                            continue;
                        }

                        for (Direction direction : HORIZONTAL_DIRECTIONS) {
                            wallPos.set(
                                    x + direction.getStepX(),
                                    y,
                                    z + direction.getStepZ()
                            );
                            if (ownedBox.isInside(wallPos)) {
                                wallPositions.add(wallPos.asLong());
                            }
                        }
                    }
                }
            }
        }

        return wallPositions;
    }

    private Block overworldBoundaryReplacement(WorldGenLevel level, BlockPos pos) {
        return overworldStoneReplacement(pos, level.getBlockState(pos));
    }

    @Override
    protected BlockState getRiverFluidState(
            CarveEllipsoid source,
            int x,
            int y,
            int z
    ) {
        int highestMainSpiralFloorY = Mth.ceil(this.origin.getY() - this.verticalRadius) - 1;
        if (y > highestMainSpiralFloorY) {
            return null;
        }

        return Blocks.WATER.defaultBlockState();
    }

    @Override
    protected double maximumSpiralTurns() {
        return 2.0D;
    }

    @Override
    protected void configureGuaranteedMob(Mob mob, int structureSpawnIndex) {
        if (mob instanceof MushroomCow mushroomCow) {
            MushroomCow.MushroomType variant = structureSpawnIndex < 3
                    ? MushroomCow.MushroomType.RED
                    : MushroomCow.MushroomType.BROWN;
            mushroomCow.setVariant(variant);
        }
    }

    private boolean isNearDescendingRingAnimalGroup(
            BlockPos pos,
            double pathFraction
    ) {
        return isNearDescendingRingAnimalGroup(pos, pathFraction, 0.0D);
    }

    private boolean isNearDescendingRingAnimalGroup(
            BlockPos pos,
            double pathFraction,
            double radialShift
    ) {
        double ringRadius = this.centralPillarDiameter * 0.5D
                + this.horizontalRadius * 0.5D
                + radialShift;
        double yaw = YAW_START - pathFraction * FULL_TURN;
        double groupCenterX = this.pillarCenterX + Math.cos(yaw) * ringRadius;
        double groupCenterZ = this.pillarCenterZ + Math.sin(yaw) * ringRadius;
        int groupMinX = Mth.floor(groupCenterX) - ANIMAL_GROUP_WIDTH / 2;
        int groupMinZ = Mth.floor(groupCenterZ) - ANIMAL_GROUP_WIDTH / 2;

        return pos.getX() >= groupMinX
                && pos.getX() < groupMinX + ANIMAL_GROUP_WIDTH
                && pos.getZ() >= groupMinZ
                && pos.getZ() < groupMinZ + ANIMAL_GROUP_WIDTH;
    }

    private double polarBearGroupPathFraction(double upwardArcShift) {
        double ringRadius = this.centralPillarDiameter * 0.5D + this.horizontalRadius * 0.5D;
        double arcShiftAsPathFraction = upwardArcShift / (FULL_TURN * ringRadius);
        return POLAR_BEAR_GROUP_BASE_PATH_FRACTION - arcShiftAsPathFraction;
    }

    private double mooshroomGroupPathFraction() {
        double middleTurns = (MUSHROOM_SECTION_START_TURNS + MUSHROOM_SECTION_END_TURNS) * 0.5D;
        return middleTurns - Math.floor(middleTurns);
    }

    private void placeGuaranteedInnerRingBerryPatches(WorldGenLevel level, BoundingBox box) {
        placeGuaranteedGrassBerryPatchType(
                level,
                box,
                SWEET_BERRY_PATCH_PATH_FRACTIONS,
                Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3)
        );
        placeGuaranteedGrassBerryPatchType(
                level,
                box,
                BLUE_BERRY_PATCH_PATH_FRACTIONS,
                ModBlocks.BLUE_BERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3)
        );
    }

    private void placeGuaranteedGrassBerryPatchType(
            WorldGenLevel level,
            BoundingBox box,
            double[] pathFractions,
            BlockState bushState
    ) {
        int centerX = (int) Math.floor(this.pillarCenterX);
        int centerZ = (int) Math.floor(this.pillarCenterZ);
        double innerRingRadius = this.centralPillarDiameter * 0.5D + this.horizontalRadius * 0.5D;

        for (double pathFraction : pathFractions) {
            double yaw = YAW_START - pathFraction * FULL_TURN;
            int nominalX = centerX + (int) Math.round(Math.cos(yaw) * innerRingRadius);
            int nominalZ = centerZ + (int) Math.round(Math.sin(yaw) * innerRingRadius);
            int patchMargin = BERRY_PATCH_RADIUS + BERRY_TREE_CLEARANCE;
            int patchCenterX = moveIntoChunkInterior(nominalX, patchMargin);
            int patchCenterZ = moveIntoChunkInterior(nominalZ, patchMargin);

            if (patchCenterX < box.minX() || patchCenterX > box.maxX()
                    || patchCenterZ < box.minZ() || patchCenterZ > box.maxZ()) {
                continue;
            }

            List<BlockPos> plantPositions = findClearBerryPatch(
                    level,
                    box,
                    patchCenterX,
                    patchCenterZ
            );
            if (plantPositions == null) {
                continue;
            }

            for (BlockPos plantPos : plantPositions) {
                clearTallPlantAbove(level, plantPos);
                level.setBlock(plantPos, bushState, Block.UPDATE_CLIENTS);
            }
        }
    }

    private int moveIntoChunkInterior(int coordinate, int margin) {
        int chunkMin = Math.floorDiv(coordinate, 16) * 16;
        return Mth.clamp(coordinate, chunkMin + margin, chunkMin + 15 - margin);
    }

    private List<BlockPos> findClearBerryPatch(
            WorldGenLevel level,
            BoundingBox box,
            int nominalCenterX,
            int nominalCenterZ
    ) {
        for (int radius = 0; radius <= BERRY_CENTER_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    List<BlockPos> positions = getValidBerryPatchPositions(
                            level,
                            box,
                            nominalCenterX + dx,
                            nominalCenterZ + dz
                    );
                    if (positions != null) {
                        return positions;
                    }
                }
            }
        }

        return null;
    }

    private List<BlockPos> getValidBerryPatchPositions(
            WorldGenLevel level,
            BoundingBox box,
            int centerX,
            int centerZ
    ) {
        int requiredMargin = BERRY_PATCH_RADIUS + BERRY_TREE_CLEARANCE;
        if (centerX - requiredMargin < box.minX() || centerX + requiredMargin > box.maxX()
                || centerZ - requiredMargin < box.minZ() || centerZ + requiredMargin > box.maxZ()) {
            return null;
        }

        List<BlockPos> positions = new ArrayList<>(BERRY_PATCH_OFFSETS.length);
        int minimumPlantY = Integer.MAX_VALUE;
        int maximumPlantY = Integer.MIN_VALUE;

        for (int[] offset : BERRY_PATCH_OFFSETS) {
            BlockPos plantPos = findUpperGrassPlantPosition(level, centerX + offset[0], centerZ + offset[1]);
            if (plantPos == null || !box.isInside(plantPos)) {
                return null;
            }

            positions.add(plantPos);
            minimumPlantY = Math.min(minimumPlantY, plantPos.getY());
            maximumPlantY = Math.max(maximumPlantY, plantPos.getY());
        }

        int logScanRadius = BERRY_PATCH_RADIUS + BERRY_TREE_CLEARANCE;
        for (int x = centerX - logScanRadius; x <= centerX + logScanRadius; x++) {
            for (int z = centerZ - logScanRadius; z <= centerZ + logScanRadius; z++) {
                for (int y = minimumPlantY - 1; y <= maximumPlantY + BERRY_LOG_SCAN_HEIGHT; y++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.LOGS)) {
                        return null;
                    }
                }
            }
        }

        return positions;
    }

    private BlockPos findUpperGrassPlantPosition(WorldGenLevel level, int x, int z) {
        int maxGroundY = (int) Math.ceil(this.origin.getY() - this.verticalRadius) - 1;
        int minGroundY = Mth.floor(mushroomSectionStartFloorAirY());

        for (int y = maxGroundY; y >= minGroundY; y--) {
            BlockPos ground = new BlockPos(x, y, z);
            BlockPos plant = ground.above();

            if (level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                    && canReplaceWithBerryBush(level.getBlockState(plant))) {
                return plant;
            }
        }

        return null;
    }

    private boolean canReplaceWithBerryBush(BlockState state) {
        return state.isAir()
                || state.is(Blocks.GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN);
    }

    private void clearTallPlantAbove(WorldGenLevel level, BlockPos plantPos) {
        BlockState state = level.getBlockState(plantPos);
        if (!(state.getBlock() instanceof DoublePlantBlock)) {
            return;
        }

        DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
        BlockPos otherHalf = half == DoubleBlockHalf.LOWER ? plantPos.above() : plantPos.below();
        level.setBlock(otherHalf, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void afterPostProcess(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos
    ) {
        placeGuaranteedInnerRingBerryPatches(level, box);
        double mooshroomPathFraction = mooshroomGroupPathFraction();
        double intermediateGroupUpperPathFraction = polarBearGroupPathFraction(
                INTERMEDIATE_GROUP_POLAR_ARC_SHIFT
        );
        double polarBearPathFraction = polarBearGroupPathFraction(POLAR_BEAR_UPWARD_ARC_SHIFT);
        double goatPathFraction = Mth.lerp(
                GOAT_POSITION_BETWEEN_MOOSE_AND_POLAR_BEAR,
                MOOSE_GROUP_PATH_FRACTION,
                intermediateGroupUpperPathFraction
        );
        double rabbitPathFraction = Mth.lerp(
                RABBIT_POSITION_BETWEEN_MOOSE_AND_POLAR_BEAR,
                MOOSE_GROUP_PATH_FRACTION,
                intermediateGroupUpperPathFraction
        );
        double foxPathFraction = Mth.lerp(
                FOX_POSITION_BETWEEN_MOOSE_AND_POLAR_BEAR,
                MOOSE_GROUP_PATH_FRACTION,
                intermediateGroupUpperPathFraction
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, EntityType.MOOSHROOM, 6,
                state -> state.is(Blocks.MYCELIUM),
                pos -> isNearDescendingRingAnimalGroup(pos, mooshroomPathFraction)
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, AMEntityRegistry.MOOSE.get(), 4,
                state -> state.is(Blocks.GRASS_BLOCK),
                pos -> isNearDescendingRingAnimalGroup(pos, MOOSE_GROUP_PATH_FRACTION)
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, EntityType.FOX, 4,
                state -> state.is(Blocks.GRASS_BLOCK),
                pos -> isNearDescendingRingAnimalGroup(pos, foxPathFraction)
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, EntityType.RABBIT, 8,
                state -> state.is(Blocks.GRASS_BLOCK),
                pos -> isNearDescendingRingAnimalGroup(pos, rabbitPathFraction)
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, EntityType.POLAR_BEAR, 4,
                state -> state.is(Blocks.GRASS_BLOCK),
                pos -> isNearDescendingRingAnimalGroup(
                        pos,
                        polarBearPathFraction,
                        this.horizontalRadius * POLAR_BEAR_RIVERWARD_SHIFT_FRACTION
                )
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, EntityType.GOAT, 6,
                state -> state.is(Blocks.GRASS_BLOCK),
                pos -> isNearDescendingRingAnimalGroup(pos, goatPathFraction)
        );
    }
}
