package net.amurdza.examplemod.worldgen.structure;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.CaveStructureConfig;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.worldgen.NetherCaveSectionClassifier;
import net.amurdza.examplemod.worldgen.NetherCaveSectionClassifier.Section;
import net.amurdza.examplemod.worldgen.surface_rule.BandlandsSurfaceRuleSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetherCavePiece extends AbstractSpiralCavePiece {
    private static final double REFERENCE_HORIZONTAL_RADIUS =
            CaveStructureConfig.NETHER_CAVE_HORIZONTAL_RADIUS;
    private static final double YAW_START = Math.PI * 1.5D;
    private static final double FULL_TURN = Math.PI * 2.0D;
    private static final double UPPER_SPIRAL_TURNS = 1.0D;
    private static final double SOUTHBOUND_UPWARD_SPIRAL_TURNS = 0.25D;
    private static final double UPPER_VERTICAL_RADIUS_INCREASE = 16.0D;
    private static final int DESERT_LAND_SAND_MAX_Y = 211;
    private static final int DESERT_TERRACE_MAX_SURFACE_Y = DESERT_LAND_SAND_MAX_Y + 1;
    private static final int UPPER_LAND_SAND_DEPTH = 2;
    private static final int UPPER_LAND_SANDSTONE_DEPTH = 2;
    private static final int UPPER_LAND_STONE_DEPTH = 4;
    private static final int DESERT_RIVER_FLOOR_LAYER_DEPTH = 4;
    private static final int STRAIGHT_TERRACE_SAND_DEPTH = 2;
    private static final int STRAIGHT_TERRACE_SANDSTONE_DEPTH = 2;
    private static final int STRAIGHT_RIVER_SAND_DEPTH = 3;
    private static final int STRAIGHT_RIVER_SANDSTONE_DEPTH = 1;
    private static final int STRAIGHT_TERRACE_SHORE_WIDTH = 72;
    private static final int STRAIGHT_TERRACE_TOP_Y = 217;
    private static final int STRAIGHT_TERRACE_BIOME_WIDTH_BLOCKS =
            CaveStructureConfig.NETHER_CAVE_BIOME_WIDTH * 16;
    private static final int STRAIGHT_TERRACE_X_SPACING_BLOCKS =
            CaveStructureConfig.NETHER_CAVE_X_SPACING * 16;
    private static final int STRAIGHT_TERRACE_RIVER_WIDTH_BLOCKS = 7 * 16;
    private static final int STRAIGHT_TERRACE_FLAT_AFTER_RIVER = 40;
    private static final int STRAIGHT_TERRACE_FLAT_AFTER_BIOME_EDGE = 96;
    private static final int STRAIGHT_TERRACE_MAX_BAND_DISTANCE = 416;
    private static final int STRAIGHT_TERRACE_BAND_HEIGHT = 6;
    private static final int STRAIGHT_TERRACE_SURFACE_DEPTH = 4;
    private static final int STRAIGHT_TERRACE_RED_SAND_STEPS = 4;
    private static final int STRAIGHT_TRANSITION_LENGTH = 24;
    private static final int STRAIGHT_TRANSITION_STONE_DEPTH = 4;
    private static final int STRAIGHT_TRANSITION_TERRACOTTA_DEPTH = 3;
    private static final int STRAIGHT_TRANSITION_RED_SANDSTONE_DEPTH = 2;
    private static final int STRAIGHT_TRANSITION_RUN_PER_RISE = 8;
    private static final int STRAIGHT_RIVER_END_BUFFER = 44;
    private static final int STRAIGHT_TERRACE_WALL_EAST_TRIM = 30;
    private static final int STRAIGHT_TERRACE_AIR_ONLY_END_LENGTH = 4;
    private static final int DESERT_ANIMAL_GROUP_WIDTH = 10;
    private static final double CAMEL_GROUP_TURNS_AFTER_FULL_RADIUS = 0.10D;
    private static final double RABBIT_GROUP_TURNS_AFTER_FULL_RADIUS = 0.20D;
    private static final double RED_MUSHROOM_UPWARD_TURNS = 0.10D;
    private static final double BROWN_MUSHROOM_UPWARD_TURNS = 0.15D;
    private static final double MUSHROOM_INNER_BANK_INSET_FRACTION =
            24.0D / REFERENCE_HORIZONTAL_RADIUS;
    private static final int MUSHROOM_GROUND_SEARCH_ABOVE = 4;
    private static final int MUSHROOM_GROUND_SEARCH_BELOW = 16;
    private static final int MUSHROOM_PATCH_ANCHOR_SEARCH_RADIUS = 6;
    private static final int GUARANTEED_SUGAR_CANE_CAVE_CENTER_X =
            CaveStructureConfig.NETHER_CAVE_X_START
                    - CaveStructureConfig.NETHER_CAVE_X_SPACING * 16;
    private static final int GUARANTEED_SUGAR_CANE_CAVE_CENTER_Z = (int) Math.floor(
            CaveStructureConfig.NETHER_CAVE_TARGET_CONTINENTS
                    * CaveStructureConfig.NETHER_CAVE_BIOME_WIDTH
                    * 10
    ) * 16;
    private static final int GUARANTEED_SUGAR_CANE_MIN_X =
            GUARANTEED_SUGAR_CANE_CAVE_CENTER_X - 5;
    private static final int GUARANTEED_SUGAR_CANE_MAX_X =
            GUARANTEED_SUGAR_CANE_CAVE_CENTER_X + 3;
    private static final int GUARANTEED_SUGAR_CANE_GROUND_Y = 186;
    private static final int GUARANTEED_SUGAR_CANE_MIN_Y = 187;
    private static final int GUARANTEED_SUGAR_CANE_MAX_Y = 189;
    private static final int WATER_LAVA_BUFFER_DEEP_DARK_FLOORS = 2;
    private static final int WATER_LAVA_BUFFER_SOUL_SAND_FLOORS = 5;
    private static final double NETHER_MOB_BIOME_CENTER_HALF_HEIGHT = 6.0D;
    private static final double NETHER_MOB_INNER_RING_FRACTION =
            16.0D / REFERENCE_HORIZONTAL_RADIUS;
    private static final double NETHER_MOB_RADIAL_HALF_WIDTH = 2.0D;
    private static final int FLAT_PATCH_SEARCH_RADIUS = 24;
    private static final int FLAT_PATCH_CENTERING_RADIUS = 24;
    private static final int PALE_OAK_REFERENCE_INNER_COUNT = 24;
    private static final double PALE_OAK_REFERENCE_PILLAR_RADIUS =
            CaveStructureConfig.NETHER_CAVE_CENTRAL_PILLAR_DIAMETER * 0.5D;
    private static final double PALE_OAK_REFERENCE_INNER_AREA = Math.PI * (
            Math.pow(PALE_OAK_REFERENCE_PILLAR_RADIUS + REFERENCE_HORIZONTAL_RADIUS, 2.0D)
                    - Math.pow(PALE_OAK_REFERENCE_PILLAR_RADIUS, 2.0D)
    );
    private static final int PALE_OAK_LAYOUT_ATTEMPTS_PER_SLOT = 128;
    private static final int PALE_OAK_WALL_CLEARANCE = 8;
    private static final int PALE_OAK_PATCH_CLEARANCE = 4;
    private static final int PALE_OAK_MINIMUM_SPACING = 10;
    private static final long PALE_OAK_INNER_SALT = 0x49F36E2A45B18C71L;
    private static final long PALE_OAK_OUTER_SALT = 0x6C8A91D3472EF025L;
    private static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK_CAVE_FEATURE =
            ResourceKey.create(
                    Registries.CONFIGURED_FEATURE,
                    AOEMod.makeID("soul_sand_valley/pale_oak")
            );
    private static final int[][] GUARANTEED_MUSHROOM_PATCH_OFFSETS = new int[][] {
            {0, 0},
            {1, 1},
            {-1, 1},
            {0, -1}
    };
    private static final int[][] FOUR_BY_FOUR_PATCH_OFFSETS = new int[][] {
            {-1, -1}, {0, -1}, {1, -1}, {2, -1},
            {-1, 0}, {0, 0}, {1, 0}, {2, 0},
            {-1, 1}, {0, 1}, {1, 1}, {2, 1},
            {-1, 2}, {0, 2}, {1, 2}, {2, 2}
    };
    private static final int[][] EYEBLOSSOM_PATCH_OFFSETS = new int[][] {
            {0, 0},
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {-1, 1},
            {1, -1},
            {-1, -1},
            {2, 0},
            {-2, 0},
            {0, 2},
            {0, -2},
            {2, 1},
            {-2, -1},
            {1, -2}
    };
    private static final double[] NETHER_WART_PATCH_INNER_FRACTIONS = new double[] {0.5D};
    private static final double[] SOUL_BERRY_PATCH_INNER_FRACTIONS = new double[] {0.5D};
    private static final double[] EYEBLOSSOM_PATCH_INNER_FRACTIONS = new double[] {
            7.0D / REFERENCE_HORIZONTAL_RADIUS,
            20.0D / REFERENCE_HORIZONTAL_RADIUS,
            12.0D / REFERENCE_HORIZONTAL_RADIUS,
            30.0D / REFERENCE_HORIZONTAL_RADIUS,
            16.0D / REFERENCE_HORIZONTAL_RADIUS,
            24.0D / REFERENCE_HORIZONTAL_RADIUS,
            10.0D / REFERENCE_HORIZONTAL_RADIUS
    };
    private static final double[] NETHER_WART_PATCH_PATH_FRACTIONS = new double[] {0.28D};
    private static final double[] SOUL_BERRY_PATCH_PATH_FRACTIONS = new double[] {0.68D};
    private static final double[] EYEBLOSSOM_PATCH_PATH_FRACTIONS = new double[] {
            1.0D / 14.0D,
            3.0D / 14.0D,
            5.0D / 14.0D,
            7.0D / 14.0D,
            9.0D / 14.0D,
            11.0D / 14.0D,
            13.0D / 14.0D
    };
    private static final Map<FlatPatchSearchKey, Optional<FlatPatchAnchor>> FLAT_PATCH_ANCHOR_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<PaleOakLayoutKey, List<PaleOakAnchor>> PALE_OAK_LAYOUT_CACHE =
            new ConcurrentHashMap<>();

    private final int maxDeepDarkY;
    private final int maxSoulSandValleyY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltasY;

    public NetherCavePiece(
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
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(
                ModStructures.NETHER_CAVE_PIECE.get(),
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
                placedFeatures,
                (int) Math.ceil(calculateUpperSpiralSurfaceY(
                        origin.getY(),
                        horizontalRadius,
                        verticalRadius,
                        centralPillarDiameter,
                        minFloorThickness,
                        liquidDepth,
                        liquidRadius
                ))
        );

        this.maxDeepDarkY = maxDeepDarkY;
        this.maxSoulSandValleyY = maxSoulSandValleyY;
        this.maxWarpedForestY = maxWarpedForestY;
        this.maxCrimsonForestY = maxCrimsonForestY;
        this.maxBasaltDeltasY = maxBasaltDeltasY;
    }

    @SuppressWarnings("unused")
    public NetherCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), tag);

        this.maxDeepDarkY = tag.getInt("MaxDeepDarkY");
        this.maxSoulSandValleyY = tag.getInt("MaxSoulSandValleyY");
        this.maxWarpedForestY = tag.getInt("MaxWarpedForestY");
        this.maxCrimsonForestY = tag.getInt("MaxCrimsonForestY");
        this.maxBasaltDeltasY = tag.getInt("MaxBasaltDeltasY");
    }

    @Override
    protected void addSubclassSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt("MaxDeepDarkY", this.maxDeepDarkY);
        tag.putInt("MaxSoulSandValleyY", this.maxSoulSandValleyY);
        tag.putInt("MaxWarpedForestY", this.maxWarpedForestY);
        tag.putInt("MaxCrimsonForestY", this.maxCrimsonForestY);
        tag.putInt("MaxBasaltDeltasY", this.maxBasaltDeltasY);
    }

    @Override
    protected void decorateCaveFloor(WorldGenLevel level, BlockPos surfacePos) {
        Section section = sectionFor(surfacePos.above());

        switch (section) {
            case DEEP_DARK -> setSculk(level, surfacePos, Direction.DOWN, section);
            case SOUL_SAND_VALLEY, BASALT_DELTAS -> replaceExposedNetherBoundaryBlock(level, surfacePos, section, false);
            case WARPED_FOREST -> setBlock(level, surfacePos, Blocks.WARPED_NYLIUM);
            case CRIMSON_FOREST -> setBlock(level, surfacePos, Blocks.CRIMSON_NYLIUM);
            case DESERT -> {
                BlockState state = level.getBlockState(surfacePos);
                if (!isProtectedDesertLandLayer(state)
                        && !isSandySurfaceBuilderLayer(surfacePos, state)) {
                    setBlock(level, surfacePos, Blocks.STONE);
                }
            }
        }

        replaceExposedUndesirableBoundaryBlock(level, surfacePos, Blocks.STONE, false);
    }

    @Override
    protected void decorateRiverFloor(WorldGenLevel level, BlockPos surfacePos) {
        if (sectionFor(surfacePos.above()) == Section.DESERT) {
            setCoveredDesertRiverLayers(level, surfacePos);
        } else {
            decorateCaveFloor(level, surfacePos);
        }
    }

    private void setDesertSeafloorLayers(WorldGenLevel level, BlockPos surfacePos) {
        setBlock(level, surfacePos, Blocks.SAND);
        setBlock(level, surfacePos.below(), Blocks.SAND);
        setBlock(level, surfacePos.below(2), Blocks.SAND);
        setBlock(level, surfacePos.below(3), Blocks.SANDSTONE);
    }

    private void setCoveredDesertRiverLayers(WorldGenLevel level, BlockPos surfacePos) {
        for (int depth = 0; depth < DESERT_RIVER_FLOOR_LAYER_DEPTH; depth++) {
            setBlock(level, surfacePos.below(depth), Blocks.STONE);
        }
    }

    @Override
    protected void decorateCaveWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {
        Section section = sectionFor(wallPos);

        if (section == Section.DESERT) {
            replaceExposedDesertBoundaryBlock(level, wallPos);
            return;
        }

        if (section == Section.DEEP_DARK) {
            setSculk(level, wallPos, wallDirection, section);
        }

        if (isNetherBoundarySection(section)) {
            replaceExposedNetherBoundaryBlock(level, wallPos, section, false);
            return;
        }

        replaceExposedUndesirableBoundaryBlock(level, wallPos, Blocks.STONE, false);
    }

    @Override
    protected void decorateRiverWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {
        decorateCaveWall(level, wallPos, wallDirection);
    }

    @Override
    protected void decorateCaveCeiling(WorldGenLevel level, BlockPos ceilingPos) {
        Section section = sectionFor(ceilingPos.below());

        if (section == Section.DESERT) {
            replaceExposedDesertBoundaryBlock(level, ceilingPos);
            return;
        }

        if (section == Section.DEEP_DARK) {
            setSculk(level, ceilingPos, Direction.UP, section);
        }

        if (isNetherBoundarySection(section)) {
            replaceExposedNetherBoundaryBlock(level, ceilingPos, section, shouldReplaceLooseSandOnCeiling(section));
            return;
        }

        replaceExposedUndesirableBoundaryBlock(level, ceilingPos, Blocks.STONE, shouldReplaceLooseSandOnCeiling(section));
    }

    private boolean shouldReplaceLooseSandOnCeiling(Section section) {
        return section == Section.DEEP_DARK;
    }

    private boolean isNetherBoundarySection(Section section) {
        return section == Section.SOUL_SAND_VALLEY
                || section == Section.WARPED_FOREST
                || section == Section.CRIMSON_FOREST
                || section == Section.BASALT_DELTAS;
    }

    private void replaceExposedNetherBoundaryBlock(
            WorldGenLevel level,
            BlockPos pos,
            Section section,
            boolean replaceSandAndSandstone
    ) {
        BlockState state = level.getBlockState(pos);
        Block replacement = regionalBoundaryReplacement(section, pos);

        if (replacement == null || !canReplace(state) || isProtectedNetherBoundaryState(state)) {
            return;
        }

        if (shouldReplaceWithRegionalBoundary(state, replaceSandAndSandstone)) {
            level.setBlock(
                    pos,
                    boundaryReplacementForState(state, replacement).defaultBlockState(),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    private Block regionalBoundaryReplacement(Section section, BlockPos pos) {
        return switch (section) {
            case SOUL_SAND_VALLEY -> ((pos.getX() + pos.getZ()) & 1) == 0 ? Blocks.SOUL_SAND : Blocks.SOUL_SOIL;
            case BASALT_DELTAS -> ((pos.getX() + pos.getY() + pos.getZ()) & 1) == 0 ? Blocks.BASALT : Blocks.BLACKSTONE;
            case WARPED_FOREST, CRIMSON_FOREST -> Blocks.NETHERRACK;
            case DESERT, DEEP_DARK -> null;
        };
    }

    private boolean shouldReplaceWithRegionalBoundary(BlockState state, boolean replaceSandAndSandstone) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_CATALYST)
                || state.is(Blocks.SCULK_SENSOR)
                || state.is(Blocks.SCULK_SHRIEKER)
                || oreHostReplacementForState(state) != null
                || (replaceSandAndSandstone && isLooseSandOrNonRedSandstone(state));
    }

    private boolean isProtectedNetherBoundaryState(BlockState state) {
        return state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.END_STONE);
    }

    private boolean isLooseSandOrNonRedSandstone(BlockState state) {
        return state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SUSPICIOUS_SAND)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE)
                || state.is(Blocks.SMOOTH_SANDSTONE);
    }

    private boolean isRedSandstone(BlockState state) {
        return state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.CHISELED_RED_SANDSTONE)
                || state.is(Blocks.CUT_RED_SANDSTONE)
                || state.is(Blocks.SMOOTH_RED_SANDSTONE);
    }

    private void replaceExposedDesertBoundaryBlock(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!canReplace(state)
                || isRedSandstone(state)
                || isSandySurfaceBuilderLayer(pos, state)) {
            return;
        }

        if (oreHostReplacementForState(state) != null) {
            level.setBlock(
                    pos,
                    boundaryReplacementForState(state, Blocks.STONE).defaultBlockState(),
                    Block.UPDATE_CLIENTS
            );
        } else if (isLooseSandOrNonRedSandstone(state)) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean isSandySurfaceBuilderLayer(BlockPos pos, BlockState state) {
        int minimumLayerY = DESERT_TERRACE_MAX_SURFACE_Y
                - UPPER_LAND_SAND_DEPTH
                - UPPER_LAND_SANDSTONE_DEPTH
                + 1;
        return pos.getY() >= minimumLayerY
                && pos.getY() <= DESERT_TERRACE_MAX_SURFACE_Y
                && BandlandsSurfaceRuleSource.usesSandySurfaceAt(pos.getX(), pos.getZ())
                && (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE));
    }

    protected void setSculk(WorldGenLevel level, BlockPos pos, Direction direction){
        setSculk(level, pos, direction, sectionFor(pos));
    }

    private void setSculk(WorldGenLevel level, BlockPos pos, Direction direction, Section surfaceSection) {
        boolean verticalSurface = direction.getAxis() == Direction.Axis.Y;
        if (surfaceSection != Section.DEEP_DARK) {
            return;
        }

        for(int i=0; i<4; i++){
            if(i>1 && verticalSurface){
                return;
            }
            BlockPos pos1 = pos.relative(direction,i);
            if(!verticalSurface && sectionFor(pos1) != Section.DEEP_DARK){
                return;
            }
            BlockState state = level.getBlockState(pos1);
            if(!state.is(Blocks.SCULK_CATALYST)&&!state.is(Blocks.SCULK_SHRIEKER)
                    &&!state.is(Blocks.SCULK_SENSOR)
                    &&!state.is(Blocks.CHORUS_FLOWER)
                    &&!state.is(Blocks.CHORUS_PLANT)&&!state.is(Blocks.SCULK)){
                Block replaceBlock = i < 2 ? Blocks.SCULK : Blocks.END_STONE;
                setBlock(level, pos1, replaceBlock);
            }
            else {
                break;
            }
        }
    }

    @Override
    protected BlockState getRiverFluidState(
            CarveEllipsoid source,
            int x,
            int y,
            int z
    ) {
        double caveCenterY = source.centerY() - 1.0D;
        if (isWaterLavaBuffer(caveCenterY)) {
            return null;
        }

        if (isOutsideStraightExitRiverShape(source, x, z)) {
            return null;
        }

        Section section = sectionFor(x, (int) Math.round(caveCenterY), z);

        return switch (section) {
            case DESERT, DEEP_DARK -> Blocks.WATER.defaultBlockState();
            case SOUL_SAND_VALLEY, WARPED_FOREST, CRIMSON_FOREST, BASALT_DELTAS -> Blocks.LAVA.defaultBlockState();
        };
    }

    @Override
    protected void afterCarvingBeforeFeatures(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        decorateLocalChunkSeams(level, box, chunkPos, chunkEllipsoids);
        finishDesertTerrainColumns(level, box, chunkPos, chunkEllipsoids);
        replaceExposedSandyRiverStone(level, box, chunkPos, chunkEllipsoids);
        placeGuaranteedSoulSandValleyPatches(level, box);
    }

    private void replaceExposedSandyRiverStone(
            WorldGenLevel level,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        BoundingBox pieceBox = this.getBoundingBox();
        int minX = Math.max(chunkPos.getMinBlockX(), Math.max(box.minX(), pieceBox.minX()));
        int maxX = Math.min(chunkPos.getMaxBlockX(), Math.min(box.maxX(), pieceBox.maxX()));
        int minY = Math.max(box.minY(), pieceBox.minY());
        int maxY = Math.min(box.maxY(), pieceBox.maxY());
        int minZ = Math.max(chunkPos.getMinBlockZ(), Math.max(box.minZ(), pieceBox.minZ()));
        int maxZ = Math.min(chunkPos.getMaxBlockZ(), Math.min(box.maxZ(), pieceBox.maxZ()));
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }

        BlockPos.MutableBlockPos boundaryPos = new BlockPos.MutableBlockPos();

        /*
         * Run after the final terrain-column pass because that pass can restore
         * the lower sandy substrate to stone. The one-block halo lets this
         * chunk finish its own edge even when the water cell is in a neighbor.
         */
        for (int fluidX = minX - 1; fluidX <= maxX + 1; fluidX++) {
            for (int fluidZ = minZ - 1; fluidZ <= maxZ + 1; fluidZ++) {
                for (RiverColumnProfile profile : riverColumnProfiles(
                        fluidX,
                        fluidZ,
                        chunkEllipsoids
                )) {
                    int profileMinY = Math.max(profile.minY(), minY);
                    int profileMaxY = Math.min(profile.maxY(), maxY);

                    for (int y = profileMinY; y <= profileMaxY; y++) {
                        BlockState fluidState = getRiverFluidState(
                                profile.source(),
                                fluidX,
                                y,
                                fluidZ
                        );
                        if (fluidState == null || !fluidState.is(Blocks.WATER)) {
                            continue;
                        }

                        for (Direction direction : HORIZONTAL_DIRECTIONS) {
                            int boundaryX = fluidX + direction.getStepX();
                            int boundaryZ = fluidZ + direction.getStepZ();
                            if (boundaryX < minX || boundaryX > maxX
                                    || boundaryZ < minZ || boundaryZ > maxZ) {
                                continue;
                            }

                            boundaryPos.set(boundaryX, y, boundaryZ);
                            if (!box.isInside(boundaryPos)
                                    || !BandlandsSurfaceRuleSource.usesSandySurfaceAt(boundaryX, boundaryZ)
                                    || sectionFor(boundaryPos) != Section.DESERT
                                    || !level.getBlockState(boundaryPos).is(Blocks.STONE)) {
                                continue;
                            }

                            level.setBlock(
                                    boundaryPos,
                                    Blocks.SANDSTONE.defaultBlockState(),
                                    Block.UPDATE_CLIENTS
                            );
                        }
                    }
                }
            }
        }
    }

    private void decorateLocalChunkSeams(
            WorldGenLevel level,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        BoundingBox pieceBox = this.getBoundingBox();
        int minX = Math.max(chunkPos.getMinBlockX(), Math.max(box.minX(), pieceBox.minX()));
        int maxX = Math.min(chunkPos.getMaxBlockX(), Math.min(box.maxX(), pieceBox.maxX()));
        int minZ = Math.max(chunkPos.getMinBlockZ(), Math.max(box.minZ(), pieceBox.minZ()));
        int maxZ = Math.min(chunkPos.getMaxBlockZ(), Math.min(box.maxZ(), pieceBox.maxZ()));
        int minY = Math.max(box.minY(), pieceBox.minY());
        int maxY = Math.min(box.maxY(), pieceBox.maxY());
        if (minX > maxX || minZ > maxZ || minY > maxY) {
            return;
        }

        if (minX == chunkPos.getMinBlockX()) {
            decorateLocalChunkSeam(
                    level,
                    chunkEllipsoids,
                    minX,
                    minZ,
                    maxZ,
                    minY,
                    maxY,
                    Direction.WEST
            );
        }
        if (maxX == chunkPos.getMaxBlockX()) {
            decorateLocalChunkSeam(
                    level,
                    chunkEllipsoids,
                    maxX,
                    minZ,
                    maxZ,
                    minY,
                    maxY,
                    Direction.EAST
            );
        }
        if (minZ == chunkPos.getMinBlockZ()) {
            decorateLocalChunkSeam(
                    level,
                    chunkEllipsoids,
                    minZ,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    Direction.NORTH
            );
        }
        if (maxZ == chunkPos.getMaxBlockZ()) {
            decorateLocalChunkSeam(
                    level,
                    chunkEllipsoids,
                    maxZ,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    Direction.SOUTH
            );
        }
    }

    private void decorateLocalChunkSeam(
            WorldGenLevel level,
            List<CarveEllipsoid> chunkEllipsoids,
            int fixedCoordinate,
            int variableMin,
            int variableMax,
            int minY,
            int maxY,
            Direction outwardDirection
    ) {
        boolean xEdge = outwardDirection.getAxis() == Direction.Axis.X;
        BlockPos.MutableBlockPos boundary = new BlockPos.MutableBlockPos();

        for (int variable = variableMin; variable <= variableMax; variable++) {
            int x = xEdge ? fixedCoordinate : variable;
            int z = xEdge ? variable : fixedCoordinate;

            int outsideX = x + outwardDirection.getStepX();
            int outsideZ = z + outwardDirection.getStepZ();
            List<ColumnCarveInterval> outsideIntervals = columnCarveIntervals(
                    outsideX,
                    outsideZ,
                    minY,
                    maxY,
                    chunkEllipsoids
            );
            outsideIntervals.addAll(columnFluidIntervals(
                    outsideX,
                    outsideZ,
                    minY,
                    maxY,
                    chunkEllipsoids
            ));

            for (ColumnCarveInterval interval : mergeColumnIntervals(outsideIntervals)) {
                for (int y = interval.minY(); y <= interval.maxY(); y++) {
                    boundary.set(x, y, z);
                    if (canReplace(level.getBlockState(boundary))) {
                        decorateCaveWall(level, boundary, outwardDirection.getOpposite());
                    }
                }
            }
        }
    }

    private void finishDesertTerrainColumns(
            WorldGenLevel level,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        BoundingBox pieceBox = this.getBoundingBox();
        int minX = Math.max(
                chunkPos.getMinBlockX(),
                Math.max(box.minX(), pieceBox.minX())
        );
        int maxX = Math.min(
                chunkPos.getMaxBlockX(),
                Math.min(box.maxX(), pieceBox.maxX())
        );
        int minZ = Math.max(
                chunkPos.getMinBlockZ(),
                Math.max(box.minZ(), pieceBox.minZ())
        );
        int maxZ = Math.min(
                chunkPos.getMaxBlockZ(),
                Math.min(box.maxZ(), pieceBox.maxZ())
        );
        int minY = Math.max(
                box.minY(),
                this.maxDeepDarkY - Mth.ceil(this.liquidDepth) - 6
        );
        int terraceTopY = Math.min(
                DESERT_TERRACE_MAX_SURFACE_Y,
                Math.min(pieceBox.maxY(), box.maxY())
        );
        if (minX > maxX || minZ > maxZ || minY > terraceTopY) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                List<ColumnCarveInterval> intervals = columnCarveIntervals(
                        x,
                        z,
                        box.minY(),
                        box.maxY(),
                        chunkEllipsoids
                );
                if (intervals.isEmpty()) {
                    continue;
                }

                cursor.set(x, terraceTopY, z);
                BlockState terraceTopState = level.getBlockState(cursor);
                boolean skyOpenAtTerraceTop = terraceTopState.isAir()
                        || (terraceTopState.getFluidState().isEmpty()
                        && !terraceTopState.isSolid());
                if (skyOpenAtTerraceTop) {
                    finishSkyExposedDesertColumn(
                            level,
                            box,
                            cursor,
                            x,
                            z,
                            minY,
                            terraceTopY
                    );
                    continue;
                }

                ColumnCarveInterval highestRelevantInterval = null;
                for (int index = intervals.size() - 1; index >= 0; index--) {
                    ColumnCarveInterval interval = intervals.get(index);
                    if (interval.maxY() < minY) {
                        continue;
                    }

                    if (highestRelevantInterval == null) {
                        highestRelevantInterval = interval;
                    }
                    finishCoveredDesertCarveInterval(
                            level,
                            cursor,
                            x,
                            z,
                            interval,
                            box.minY()
                    );
                }

                if (highestRelevantInterval == null) {
                    continue;
                }

                ensureSandyCeilingThickness(
                        level,
                        box,
                        cursor,
                        x,
                        z,
                        highestRelevantInterval,
                        terraceTopY
                );
            }
        }
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

    private List<ColumnCarveInterval> columnFluidIntervals(
            int x,
            int z,
            int minY,
            int maxY,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
        List<ColumnCarveInterval> intervals = new ArrayList<>();
        if (this.liquidRadius <= 0.0F || this.liquidDepth <= 0.0F) {
            return intervals;
        }

        for (RiverColumnProfile profile : riverColumnProfiles(x, z, chunkEllipsoids)) {
            int actualFluidMinY = Integer.MAX_VALUE;
            int actualFluidMaxY = Integer.MIN_VALUE;
            for (int y = profile.minY(); y <= profile.maxY(); y++) {
                if (getRiverFluidState(profile.source(), x, y, z) != null) {
                    actualFluidMinY = Math.min(actualFluidMinY, y);
                    actualFluidMaxY = Math.max(actualFluidMaxY, y);
                }
            }
            if (actualFluidMinY == Integer.MAX_VALUE) {
                continue;
            }

            int intervalMinY = Math.max(actualFluidMinY, minY);
            int intervalMaxY = Math.min(
                    actualFluidMaxY + 2,
                    Math.min(maxY, profile.source().maxCarveY())
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

    private void finishCoveredDesertCarveInterval(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            ColumnCarveInterval interval,
            int worldMinY
    ) {
        int searchMinY = Math.max(
                worldMinY,
                interval.minY() - Mth.ceil(this.liquidDepth) - 3
        );

        for (int y = interval.minY() - 1; y >= searchMinY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }

            if (state.is(Blocks.WATER)) {
                finishDesertWaterColumn(
                        level,
                        cursor,
                        x,
                        z,
                        y,
                        searchMinY,
                        false
                );
                return;
            }

            if (!state.getFluidState().isEmpty()) {
                return;
            }

            if (!state.isSolid()) {
                continue;
            }

            return;
        }
    }

    private void finishDesertWaterColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int waterY,
            int minY,
            boolean exposedAtTerraceTop
    ) {
        if (sectionFor(cursor) != Section.DESERT) {
            return;
        }

        for (int y = waterY - 1; y >= minY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.is(Blocks.WATER) || state.isAir()) {
                continue;
            }

            if (canReplace(state)) {
                if (exposedAtTerraceTop) {
                    setDesertSeafloorLayers(level, cursor.immutable());
                } else {
                    setCoveredDesertRiverLayers(level, cursor.immutable());
                }
            }
            return;
        }
    }

    private void finishSkyExposedDesertColumn(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int minY,
            int terraceTopY
    ) {
        for (int y = terraceTopY; y >= minY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }

            if (state.is(Blocks.WATER)) {
                finishDesertWaterColumn(
                        level,
                        cursor,
                        x,
                        z,
                        y,
                        minY,
                        true
                );
                return;
            }

            if (!state.getFluidState().isEmpty()) {
                return;
            }

            if (!state.isSolid()) {
                continue;
            }

            if (y <= DESERT_LAND_SAND_MAX_Y
                    && sectionFor(cursor.above()) == Section.DESERT) {
                if (BandlandsSurfaceRuleSource.usesSandySurfaceAt(x, z)) {
                    restoreSandyBoundaryLayers(level, box, cursor, x, y, z, false);
                } else if (!isProtectedDesertLandLayer(state)) {
                    setUpperSpiralLandLayers(level, cursor.immutable());
                }
            }
            return;
        }
    }

    private void ensureSandyCeilingThickness(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            ColumnCarveInterval highestInterval,
            int terraceTopY
    ) {
        if (!BandlandsSurfaceRuleSource.usesSandySurfaceAt(x, z)
                || sectionFor(x, highestInterval.maxY(), z) != Section.DESERT) {
            return;
        }

        int ceilingTopY = Integer.MIN_VALUE;
        for (int y = terraceTopY; y >= highestInterval.maxY(); y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty() || !state.isSolid()) {
                return;
            }

            ceilingTopY = y;
            break;
        }

        if (ceilingTopY == Integer.MIN_VALUE) {
            return;
        }

        restoreSandyBoundaryLayers(level, box, cursor, x, ceilingTopY, z, true);
    }

    private void restoreSandyBoundaryLayers(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos.MutableBlockPos cursor,
            int x,
            int surfaceY,
            int z,
            boolean fillAirBelowSurface
    ) {
        int totalDepth = UPPER_LAND_SAND_DEPTH
                + UPPER_LAND_SANDSTONE_DEPTH
                + UPPER_LAND_STONE_DEPTH;
        for (int depth = 0; depth < totalDepth; depth++) {
            cursor.set(x, surfaceY - depth, z);
            if (!box.isInside(cursor)) {
                return;
            }

            BlockState state = level.getBlockState(cursor);
            boolean replaceableSolid = state.getFluidState().isEmpty()
                    && state.isSolid()
                    && canReplace(state);
            if (isProtectedDesertLandLayer(state)
                    || (state.isAir() && !fillAirBelowSurface)
                    || (!state.isAir() && !replaceableSolid)) {
                return;
            }
        }

        for (int depth = totalDepth - 1; depth >= 0; depth--) {
            cursor.set(x, surfaceY - depth, z);
            Block layer;
            if (depth < UPPER_LAND_SAND_DEPTH) {
                layer = Blocks.SAND;
            } else if (depth < UPPER_LAND_SAND_DEPTH + UPPER_LAND_SANDSTONE_DEPTH) {
                layer = Blocks.SANDSTONE;
            } else {
                layer = Blocks.STONE;
            }
            level.setBlock(cursor, layer.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void setUpperSpiralLandLayers(WorldGenLevel level, BlockPos surfacePos) {
        int totalDepth = UPPER_LAND_SAND_DEPTH
                + UPPER_LAND_SANDSTONE_DEPTH
                + UPPER_LAND_STONE_DEPTH;

        for (int depth = 0; depth < totalDepth; depth++) {
            BlockPos layerPos = surfacePos.below(depth);
            BlockState state = level.getBlockState(layerPos);
            if (!canReplace(state)) {
                return;
            }
        }

        for (int depth = 0; depth < totalDepth; depth++) {
            BlockPos layerPos = surfacePos.below(depth);
            BlockState state = level.getBlockState(layerPos);
            if (isProtectedDesertLandLayer(state)) {
                continue;
            }

            Block layer;
            if (depth < UPPER_LAND_SAND_DEPTH) {
                layer = Blocks.SAND;
            } else if (depth < UPPER_LAND_SAND_DEPTH + UPPER_LAND_SANDSTONE_DEPTH) {
                layer = Blocks.SANDSTONE;
            } else {
                layer = Blocks.STONE;
            }
            level.setBlock(layerPos, layer.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean isProtectedDesertLandLayer(BlockState state) {
        return state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.RED_SAND)
                || isRedSandstone(state);
    }

    private record ColumnCarveInterval(int minY, int maxY) {
    }

    @Override
    protected double upwardSpiralSurfaceY() {
        return calculateUpperSpiralSurfaceY(
                this.origin.getY(),
                this.horizontalRadius,
                this.verticalRadius,
                this.centralPillarDiameter,
                this.minFloorThickness,
                this.liquidDepth,
                this.liquidRadius
        );
    }

    @Override
    protected double upwardSpiralUpperVerticalRadius(double upwardTurns) {
        double radiusIncrease = Mth.clamp(
                (upwardTurns - SOUTHBOUND_UPWARD_SPIRAL_TURNS)
                        * upperSpiralRisePerTurn(),
                0.0D,
                UPPER_VERTICAL_RADIUS_INCREASE
        );
        return this.verticalRadius + radiusIncrease;
    }

    @Override
    protected int maxCarveYForEllipsoid(double centerY, double upperVerticalRadius) {
        return Math.min(
                DESERT_TERRACE_MAX_SURFACE_Y,
                super.maxCarveYForEllipsoid(centerY, upperVerticalRadius)
        );
    }

    private double upperSpiralRisePerTurn() {
        return calculateSpiralRisePerTurn(
                this.horizontalRadius,
                this.verticalRadius,
                this.centralPillarDiameter,
                this.minFloorThickness,
                this.liquidDepth,
                this.liquidRadius
        );
    }

    private static double calculateUpperSpiralSurfaceY(
            double originY,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius
    ) {
        double endCenterY = originY + calculateSpiralRisePerTurn(
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius
        ) * UPPER_SPIRAL_TURNS;
        return endCenterY - verticalRadius - 1.0D;
    }

    private static double calculateSpiralRisePerTurn(
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius
    ) {
        double pathRadius = getPathRadius(horizontalRadius, centralPillarDiameter, liquidRadius);
        double turnPerStep = turnPerStepForPathRadius(pathRadius);
        double stepsPerTurn = FULL_TURN / turnPerStep;
        double requiredVerticalSeparation =
                2.0D * verticalRadius + minFloorThickness + 1.0D + liquidDepth;
        double risePerStep = Mth.clamp(
                requiredVerticalSeparation / stepsPerTurn,
                0.01D,
                0.85D
        );
        return risePerStep * stepsPerTurn;
    }

    private void buildOpenStraightTerrace(WorldGenLevel level, BoundingBox box) {
        if (this.endX <= 0) {
            return;
        }

        int startX = Mth.floor(this.pillarCenterX);
        int terraceEndX = startX + this.endX;
        int riverEndX = terraceEndX - STRAIGHT_RIVER_END_BUFFER;
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double centerZ = this.pillarCenterZ - pathRadius;
        int openTerraceHalfWidth = Mth.ceil(this.liquidRadius + STRAIGHT_TERRACE_SHORE_WIDTH);
        int coveredTerraceHalfWidth = Mth.ceil(getTunnelHorizontalRadius());
        int flatSurfaceY = straightTerraceFlatSurfaceY();
        int terraceRise = Math.max(0, STRAIGHT_TERRACE_TOP_Y - flatSurfaceY);
        double terraceInnerHalfWidth = openTerraceHalfWidth - terraceRise;
        int wallCornerStartX = terraceEndX - STRAIGHT_TERRACE_WALL_EAST_TRIM;

        int minX = Math.max(startX, box.minX());
        int maxX = Math.min(terraceEndX, box.maxX());
        int minZ = Math.max(Mth.floor(centerZ - openTerraceHalfWidth), box.minZ());
        int maxZ = Math.min(Mth.ceil(centerZ + openTerraceHalfWidth), box.maxZ());

        if (minX > maxX || minZ > maxZ) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int maxBuildY = level.getMaxBuildHeight() - 1;

        for (int x = minX; x <= maxX; x++) {
            int distanceFromStart = x - startX;
            boolean coveredTransition = distanceFromStart < STRAIGHT_TRANSITION_LENGTH;
            int halfWidth = coveredTransition ? coveredTerraceHalfWidth : openTerraceHalfWidth;
            int waterSurfaceY = straightTerraceFlatSurfaceY();

            for (int z = minZ; z <= maxZ; z++) {
                double zDistance = Math.abs(z + 0.5D - centerZ);
                boolean inWallZRange = zDistance > terraceInnerHalfWidth
                        && zDistance <= openTerraceHalfWidth;
                int sideSlopeDistance = inWallZRange
                        ? Mth.ceil(zDistance - terraceInnerHalfWidth)
                        : 0;

                if (coveredTransition && inWallZRange) {
                    fillStraightTransitionWall(
                            level,
                            cursor,
                            x,
                            z,
                            distanceFromStart,
                            sideSlopeDistance
                    );
                    continue;
                }

                boolean airOnlyEnd = terraceEndX - x < STRAIGHT_TERRACE_AIR_ONLY_END_LENGTH;
                if (!coveredTransition && inWallZRange) {
                    int cornerSlopeDistance = x < wallCornerStartX
                            ? sideSlopeDistance
                            : Math.min(sideSlopeDistance, Math.max(0, terraceEndX - x));
                    int slopeSurfaceY = flatSurfaceY + cornerSlopeDistance;
                    if (airOnlyEnd) {
                        clearColumnAbove(level, cursor, x, slopeSurfaceY, z, maxBuildY);
                        continue;
                    }

                    setStraightTerraceSlopeFloor(
                            level,
                            cursor,
                            x,
                            slopeSurfaceY,
                            z,
                            sideSlopeDistance
                    );
                    clearColumnAbove(level, cursor, x, slopeSurfaceY, z, maxBuildY);
                    continue;
                }

                if (zDistance > halfWidth) {
                    continue;
                }

                int landSurfaceY = straightTerraceSurfaceY(distanceFromStart, z, zDistance);
                if (airOnlyEnd) {
                    clearColumnAbove(level, cursor, x, landSurfaceY, z, maxBuildY);
                    continue;
                }

                double riverShapeDistance = straightRiverShapeDistance(x, riverEndX, zDistance);
                boolean riverColumn = riverShapeDistance <= this.liquidRadius;
                if (riverColumn) {
                    int riverFloorY = waterSurfaceY - straightRiverDepth(riverShapeDistance);
                    setStraightTerraceFloor(level, cursor, x, riverFloorY, z, false, true);

                    for (int y = riverFloorY + 1; y <= waterSurfaceY; y++) {
                        cursor.set(x, y, z);
                        level.setBlock(cursor, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                } else {
                    setStraightTerraceFloor(level, cursor, x, landSurfaceY, z, coveredTransition, false);
                }

                int exposedSurfaceY = riverColumn ? waterSurfaceY : landSurfaceY;
                if (coveredTransition) {
                    int interiorTopY = Math.min(maxBuildY, Mth.floor(this.origin.getY()));
                    clearStraightTransitionInterior(level, cursor, x, exposedSurfaceY, z, interiorTopY);
                    setStraightTransitionCeiling(level, cursor, x, interiorTopY, z, maxBuildY);
                    continue;
                }

                clearColumnAbove(level, cursor, x, exposedSurfaceY, z, maxBuildY);
            }
        }
    }

    private void fillStraightTransitionWall(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int distanceFromWest,
            int sideSlopeDistance
    ) {
        int terracottaStartY = STRAIGHT_TERRACE_TOP_Y
                - STRAIGHT_TRANSITION_TERRACOTTA_DEPTH
                + 1;
        int redSandstoneStartY = terracottaStartY
                - STRAIGHT_TRANSITION_RED_SANDSTONE_DEPTH;
        Block terracottaBlock = straightTerraceBandBlockAt(x, z);

        for (int y = straightTerraceSupportBottomY(); y <= STRAIGHT_TERRACE_TOP_Y; y++) {
            cursor.set(x, y, z);
            Block block;
            if (y >= terracottaStartY) {
                block = terracottaBlock;
            } else if (y >= redSandstoneStartY) {
                block = Blocks.RED_SANDSTONE;
            } else if (distanceFromWest < 2) {
                block = Blocks.SCULK;
            } else if (distanceFromWest < 4 && sideSlopeDistance > 2) {
                block = Blocks.END_STONE;
            } else {
                block = Blocks.STONE;
            }
            level.setBlock(cursor, block.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void clearColumnAbove(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int surfaceY,
            int z,
            int maxBuildY
    ) {
        for (int y = surfaceY + 1; y <= maxBuildY; y++) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(Blocks.BEDROCK) && !state.isAir()) {
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void clearStraightTransitionInterior(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int surfaceY,
            int z,
            int interiorTopY
    ) {
        for (int y = surfaceY + 1; y <= interiorTopY; y++) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(Blocks.BEDROCK) && !state.isAir()) {
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void setStraightTransitionCeiling(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int interiorTopY,
            int z,
            int maxBuildY
    ) {
        int searchTopY = Math.min(maxBuildY, Mth.ceil(this.origin.getY() + this.verticalRadius) + 2);

        for (int y = interiorTopY + 1; y <= searchTopY; y++) {
            cursor.set(x, y, z);
            if (!canReplace(level.getBlockState(cursor))) {
                continue;
            }

            for (int depth = 0; depth < STRAIGHT_TRANSITION_STONE_DEPTH; depth++) {
                cursor.set(x, y + depth, z);
                level.setBlock(cursor, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return;
        }
    }

    private void setStraightTerraceFloor(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int surfaceY,
            int z,
            boolean stoneTransition,
            boolean riverFloor
    ) {
        if (stoneTransition) {
            for (int depth = 0; depth < STRAIGHT_TERRACE_SAND_DEPTH; depth++) {
                cursor.set(x, surfaceY - depth, z);
                level.setBlock(cursor, Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            for (int depth = 0; depth < STRAIGHT_TERRACE_SANDSTONE_DEPTH; depth++) {
                cursor.set(x, surfaceY - STRAIGHT_TERRACE_SAND_DEPTH - depth, z);
                level.setBlock(cursor, Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return;
        }

        int sandDepth = riverFloor ? STRAIGHT_RIVER_SAND_DEPTH : STRAIGHT_TERRACE_SAND_DEPTH;
        int sandstoneDepth = riverFloor
                ? STRAIGHT_RIVER_SANDSTONE_DEPTH
                : STRAIGHT_TERRACE_SANDSTONE_DEPTH;

        for (int depth = 0; depth < sandDepth; depth++) {
            cursor.set(x, surfaceY - depth, z);
            level.setBlock(cursor, Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        for (int depth = 0; depth < sandstoneDepth; depth++) {
            cursor.set(x, surfaceY - sandDepth - depth, z);
            level.setBlock(cursor, Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void setStraightTerraceSlopeFloor(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int surfaceY,
            int z,
            int slopeDistance
    ) {
        if (slopeDistance <= STRAIGHT_TERRACE_RED_SAND_STEPS) {
            for (int depth = 0; depth < STRAIGHT_TERRACE_SAND_DEPTH; depth++) {
                cursor.set(x, surfaceY - depth, z);
                level.setBlock(cursor, Blocks.RED_SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            }

            for (int depth = 0; depth < STRAIGHT_TERRACE_SANDSTONE_DEPTH; depth++) {
                cursor.set(x, surfaceY - STRAIGHT_TERRACE_SAND_DEPTH - depth, z);
                level.setBlock(cursor, Blocks.RED_SANDSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            fillStraightTerraceSupport(
                    level,
                    cursor,
                    x,
                    surfaceY - STRAIGHT_TERRACE_SAND_DEPTH - STRAIGHT_TERRACE_SANDSTONE_DEPTH,
                    z
            );
            return;
        }

        Block surfaceBlock = straightTerraceBandBlockAt(x, z);
        for (int depth = 0; depth < STRAIGHT_TERRACE_SURFACE_DEPTH; depth++) {
            cursor.set(x, surfaceY - depth, z);
            level.setBlock(cursor, surfaceBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        for (int depth = 0; depth < STRAIGHT_TERRACE_SANDSTONE_DEPTH; depth++) {
            cursor.set(x, surfaceY - STRAIGHT_TERRACE_SURFACE_DEPTH - depth, z);
            level.setBlock(cursor, Blocks.RED_SANDSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        fillStraightTerraceSupport(
                level,
                cursor,
                x,
                surfaceY - STRAIGHT_TERRACE_SURFACE_DEPTH - STRAIGHT_TERRACE_SANDSTONE_DEPTH,
                z
        );
    }

    private Block straightTerraceBandBlockAt(int x, int z) {
        return BandlandsSurfaceRuleSource.terraceBandBlockAt(
                x,
                z,
                STRAIGHT_TERRACE_BAND_HEIGHT,
                STRAIGHT_TERRACE_BIOME_WIDTH_BLOCKS,
                STRAIGHT_TERRACE_X_SPACING_BLOCKS,
                STRAIGHT_TERRACE_RIVER_WIDTH_BLOCKS,
                STRAIGHT_TERRACE_FLAT_AFTER_RIVER,
                STRAIGHT_TERRACE_FLAT_AFTER_BIOME_EDGE,
                STRAIGHT_TERRACE_MAX_BAND_DISTANCE
        );
    }

    private void fillStraightTerraceSupport(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int firstSupportY,
            int z
    ) {
        int supportBottomY = straightTerraceSupportBottomY();
        for (int y = firstSupportY; y >= supportBottomY; y--) {
            cursor.set(x, y, z);
            level.setBlock(cursor, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int straightTerraceSupportBottomY() {
        return straightTerraceFlatSurfaceY()
                - STRAIGHT_TERRACE_SAND_DEPTH
                - STRAIGHT_TERRACE_SANDSTONE_DEPTH;
    }

    private int straightTerraceSurfaceY(int distanceFromStart, int z, double zDistance) {
        int flatSurfaceY = straightTerraceFlatSurfaceY();
        if (distanceFromStart >= STRAIGHT_TRANSITION_LENGTH) {
            return flatSurfaceY;
        }

        int caveFloorY = straightTunnelFloorY(z, zDistance);
        int completedRise = distanceFromStart / STRAIGHT_TRANSITION_RUN_PER_RISE;
        if (caveFloorY > flatSurfaceY) {
            return Math.max(flatSurfaceY, caveFloorY - completedRise);
        }
        return Math.min(flatSurfaceY, caveFloorY + completedRise);
    }

    private int straightTerraceFlatSurfaceY() {
        return Mth.floor(this.origin.getY() - this.verticalRadius - 2.5D);
    }

    private int straightTunnelFloorY(int z, double zDistance) {
        double tunnelRadius = getTunnelHorizontalRadius();
        double normalizedZ = Mth.clamp(zDistance / tunnelRadius, 0.0D, 1.0D);
        double verticalScale = Math.pow(Math.max(0.0D, 1.0D - normalizedZ * normalizedZ), 0.25D);
        double lowerBoundaryY = this.origin.getY() - this.verticalRadius * verticalScale;
        int fallbackY = Mth.floor(lowerBoundaryY - 0.5D);
        int sculkEdgeX = Mth.floor(this.pillarCenterX) - 1;
        double minimumEntranceCenterY = this.origin.getY() - this.verticalRadius;
        return lowestCarvedFloorYAt(sculkEdgeX, z, minimumEntranceCenterY, fallbackY);
    }

    private double straightRiverShapeDistance(int x, int riverEndX, double zDistance) {
        double terminalX = riverEndX + 0.5D;
        double capCenterX = terminalX - this.liquidRadius;
        double queryX = x + 0.5D;

        if (queryX <= capCenterX) {
            return zDistance;
        }

        double xDistance = queryX - capCenterX;
        return Math.sqrt(xDistance * xDistance + zDistance * zDistance);
    }

    private int straightRiverDepth(double distanceFromCenter) {
        double normalizedDistance = distanceFromCenter / this.liquidRadius;
        double depthScale = Math.sqrt(Math.max(0.0D, 1.0D - normalizedDistance * normalizedDistance));
        return Math.max(1, Mth.ceil(this.liquidDepth * depthScale));
    }

    private boolean isOutsideStraightExitRiverShape(
            CarveEllipsoid source,
            int x,
            int z
    ) {
        if (this.endX <= STRAIGHT_RIVER_END_BUFFER) {
            return false;
        }

        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double centerZ = this.pillarCenterZ - pathRadius;
        int startX = Mth.floor(this.pillarCenterX);
        if (!isStraightExitEllipsoid(source, centerZ)) {
            return false;
        }

        int riverEndX = startX + this.endX - STRAIGHT_RIVER_END_BUFFER;
        double zDistance = Math.abs(z + 0.5D - centerZ);

        return x >= startX
                && x <= startX + this.endX + Mth.ceil(getTunnelHorizontalRadius())
                && zDistance <= getTunnelHorizontalRadius()
                && straightRiverShapeDistance(x, riverEndX, zDistance) > this.liquidRadius;
    }

    private boolean isStraightExitEllipsoid(
            CarveEllipsoid source,
            double centerZ
    ) {
        double epsilon = 1.0E-4D;
        double startX = this.pillarCenterX;

        return Math.abs(source.centerY() - this.origin.getY()) <= epsilon
                && Math.abs(source.centerZ() - centerZ) <= epsilon
                && source.centerX() >= startX - epsilon
                && source.centerX() <= startX + this.endX + epsilon;
    }

    private boolean isNearSkyExposedSandyAnimalGroup(
            WorldGenLevel level,
            BlockPos pos,
            double upwardTurns
    ) {
        if (!isColumnOpenToTerraceTop(level, pos) || sectionFor(pos) != Section.DESERT) {
            return false;
        }

        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double pillarRadius = this.centralPillarDiameter * 0.5D;
        double innerRiverBankRadius = pathRadius - this.liquidRadius;
        double groupRadius = (pillarRadius + innerRiverBankRadius) * 0.5D;
        double yaw = YAW_START + FULL_TURN * upwardTurns;
        double groupCenterX = this.pillarCenterX + Math.cos(yaw) * groupRadius;
        double groupCenterZ = this.pillarCenterZ + Math.sin(yaw) * groupRadius;
        int groupMinX = Mth.floor(groupCenterX) - DESERT_ANIMAL_GROUP_WIDTH / 2;
        int groupMinZ = Mth.floor(groupCenterZ) - DESERT_ANIMAL_GROUP_WIDTH / 2;

        return pos.getX() >= groupMinX
                && pos.getX() < groupMinX + DESERT_ANIMAL_GROUP_WIDTH
                && pos.getZ() >= groupMinZ
                && pos.getZ() < groupMinZ + DESERT_ANIMAL_GROUP_WIDTH;
    }

    private boolean isColumnOpenToTerraceTop(WorldGenLevel level, BlockPos pos) {
        int terraceTopY = Math.min(DESERT_TERRACE_MAX_SURFACE_Y, level.getMaxBuildHeight() - 1);
        if (pos.getY() > terraceTopY) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = terraceTopY; y >= pos.getY(); y--) {
            cursor.set(pos.getX(), y, pos.getZ());
            if (!level.getBlockState(cursor).isAir()) {
                return false;
            }
        }
        return true;
    }

    private void placeGuaranteedSugarCaneLine(WorldGenLevel level, BoundingBox box) {
        if (Mth.floor(this.pillarCenterX) != GUARANTEED_SUGAR_CANE_CAVE_CENTER_X
                || Mth.floor(this.pillarCenterZ) != GUARANTEED_SUGAR_CANE_CAVE_CENTER_Z
                || GUARANTEED_SUGAR_CANE_MAX_Y < box.minY()
                || GUARANTEED_SUGAR_CANE_GROUND_Y > box.maxY()) {
            return;
        }

        int minX = Math.max(GUARANTEED_SUGAR_CANE_MIN_X, box.minX());
        int maxX = Math.min(GUARANTEED_SUGAR_CANE_MAX_X, box.maxX());
        double innerRiverBankRadius = this.centralPillarDiameter * 0.5D
                + this.horizontalRadius;

        for (int x = minX; x <= maxX; x++) {
            double dx = x + 0.5D - this.pillarCenterX;
            double remainingRadiusSquared = innerRiverBankRadius * innerRiverBankRadius
                    - dx * dx;
            if (remainingRadiusSquared <= 0.0D) {
                continue;
            }

            int groundZ = Mth.floor(
                    this.pillarCenterZ
                            + Math.sqrt(remainingRadiusSquared)
                            - 0.5D
            );
            BlockPos ground = new BlockPos(
                    x,
                    GUARANTEED_SUGAR_CANE_GROUND_Y,
                    groundZ
            );
            BlockPos adjacentWater = ground.south();
            if (!box.isInside(ground)
                    || !box.isInside(adjacentWater)
                    || !level.getBlockState(adjacentWater).is(Blocks.WATER)) {
                continue;
            }

            level.setBlock(ground, Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = GUARANTEED_SUGAR_CANE_MIN_Y; y <= GUARANTEED_SUGAR_CANE_MAX_Y; y++) {
                BlockPos canePos = new BlockPos(x, y, groundZ);
                if (box.isInside(canePos)) {
                    level.setBlock(
                            canePos,
                            Blocks.SUGAR_CANE.defaultBlockState(),
                            Block.UPDATE_CLIENTS
                    );
                }
            }
        }
    }

    private void placeGuaranteedMushroomPatches(WorldGenLevel level, BoundingBox box) {
        boolean placeRedPatch = isMushroomPatchCenterInside(
                box,
                RED_MUSHROOM_UPWARD_TURNS
        );
        boolean placeBrownPatch = isMushroomPatchCenterInside(
                box,
                BROWN_MUSHROOM_UPWARD_TURNS
        );
        if (!placeRedPatch && !placeBrownPatch) {
            return;
        }

        List<PatchSample> upwardSamples = upwardSpiralPathSamples();
        Map<Long, Integer> floorCache = new HashMap<>();
        if (placeRedPatch) {
            placeGuaranteedMushroomPatch(
                    level,
                    box,
                    RED_MUSHROOM_UPWARD_TURNS,
                    Blocks.RED_MUSHROOM,
                    upwardSamples,
                    floorCache
            );
        }
        if (placeBrownPatch) {
            placeGuaranteedMushroomPatch(
                    level,
                    box,
                    BROWN_MUSHROOM_UPWARD_TURNS,
                    Blocks.BROWN_MUSHROOM,
                    upwardSamples,
                    floorCache
            );
        }
    }

    private boolean isMushroomPatchCenterInside(BoundingBox box, double upwardTurns) {
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double yaw = YAW_START + FULL_TURN * upwardTurns;
        double patchRadius = pathRadius
                - this.liquidRadius
                - scaledInnerRingDistance(MUSHROOM_INNER_BANK_INSET_FRACTION, 2.0D);
        int centerX = Mth.floor(this.pillarCenterX + Math.cos(yaw) * patchRadius + 0.5D);
        int centerZ = Mth.floor(this.pillarCenterZ + Math.sin(yaw) * patchRadius + 0.5D);
        return centerX >= box.minX()
                && centerX <= box.maxX()
                && centerZ >= box.minZ()
                && centerZ <= box.maxZ();
    }

    private void placeGuaranteedMushroomPatch(
            WorldGenLevel level,
            BoundingBox box,
            double upwardTurns,
            Block mushroom,
            List<PatchSample> upwardSamples,
            Map<Long, Integer> floorCache
    ) {
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double yaw = YAW_START + FULL_TURN * upwardTurns;
        double centerY = this.origin.getY()
                + upperSpiralRisePerTurn() * upwardTurns;
        double patchRadius = pathRadius
                - this.liquidRadius
                - scaledInnerRingDistance(MUSHROOM_INNER_BANK_INSET_FRACTION, 2.0D);
        int centerX = Mth.floor(this.pillarCenterX + Math.cos(yaw) * patchRadius + 0.5D);
        int centerZ = Mth.floor(this.pillarCenterZ + Math.sin(yaw) * patchRadius + 0.5D);
        if (centerX < box.minX() || centerX > box.maxX()
                || centerZ < box.minZ() || centerZ > box.maxZ()) {
            return;
        }

        BlockState mushroomState = mushroom.defaultBlockState();
        List<BlockPos> bestGroundPositions = List.of();
        for (int radius = 0; radius <= MUSHROOM_PATCH_ANCHOR_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    List<BlockPos> groundPositions = new ArrayList<>();
                    for (int[] offset : GUARANTEED_MUSHROOM_PATCH_OFFSETS) {
                        int x = centerX + dx + offset[0];
                        int z = centerZ + dz + offset[1];
                        if (x < box.minX() || x > box.maxX()
                                || z < box.minZ() || z > box.maxZ()) {
                            groundPositions.clear();
                            break;
                        }

                        int expectedGroundY = floorCache.computeIfAbsent(
                                ChunkPos.asLong(x, z),
                                ignored -> upperSpiralFloorYAt(upwardSamples, x, z, centerY)
                        );
                        BlockPos groundPos = findStoneMushroomGround(
                                level,
                                x,
                                z,
                                expectedGroundY
                        );
                        if (groundPos == null
                                || !box.isInside(groundPos.above())) {
                            groundPositions.clear();
                            break;
                        }
                        groundPositions.add(groundPos);
                    }

                    if (groundPositions.size() == GUARANTEED_MUSHROOM_PATCH_OFFSETS.length) {
                        bestGroundPositions = List.copyOf(groundPositions);
                    }
                    if (bestGroundPositions.size() == GUARANTEED_MUSHROOM_PATCH_OFFSETS.length) {
                        break;
                    }
                }
                if (bestGroundPositions.size() == GUARANTEED_MUSHROOM_PATCH_OFFSETS.length) {
                    break;
                }
            }
            if (bestGroundPositions.size() == GUARANTEED_MUSHROOM_PATCH_OFFSETS.length) {
                break;
            }
        }

        for (BlockPos groundPos : bestGroundPositions) {
            BlockPos mushroomPos = groundPos.above();
            level.setBlock(mushroomPos, mushroomState, Block.UPDATE_CLIENTS);
        }
    }

    private List<PatchSample> upwardSpiralPathSamples() {
        List<PatchSample> samples = new ArrayList<>();
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double turnPerStep = turnPerStepForPathRadius(pathRadius);
        double stepsPerTurn = FULL_TURN / turnPerStep;
        double risePerStep = upperSpiralRisePerTurn() / stepsPerTurn;
        double targetCenterY = upwardSpiralSurfaceY() + this.verticalRadius + 1.0D;
        int stepCount = Math.max(
                1,
                Mth.ceil((targetCenterY - this.origin.getY()) / risePerStep)
        );

        for (int step = 0; step <= stepCount; step++) {
            double centerY = Math.min(this.origin.getY() + step * risePerStep, targetCenterY);
            double fractionalStep = (centerY - this.origin.getY()) / risePerStep;
            samples.add(new PatchSample(YAW_START + turnPerStep * fractionalStep, centerY));
        }
        return samples;
    }

    private int upperSpiralFloorYAt(
            List<PatchSample> upwardSamples,
            int x,
            int z,
            double fallbackCenterY
    ) {
        int lowestFloorY = Integer.MAX_VALUE;
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double tunnelRadius = getTunnelHorizontalRadius();
        for (PatchSample sample : upwardSamples) {
            double centerX = this.pillarCenterX + Math.cos(sample.yaw()) * pathRadius;
            double centerZ = this.pillarCenterZ + Math.sin(sample.yaw()) * pathRadius;
            double relativeX = (x + 0.5D - centerX) / tunnelRadius;
            double relativeZ = (z + 0.5D - centerZ) / tunnelRadius;
            double remainingVerticalShape = 1.0D
                    - relativeX * relativeX
                    - relativeZ * relativeZ;
            if (remainingVerticalShape > 0.0D) {
                double floorScale = Math.pow(remainingVerticalShape, 0.25D);
                double lowerBoundaryY = sample.centerY() - this.verticalRadius * floorScale;
                lowestFloorY = Math.min(
                        lowestFloorY,
                        Mth.floor(lowerBoundaryY - 0.5D)
                );
            }
        }

        return lowestFloorY == Integer.MAX_VALUE
                ? Mth.floor(fallbackCenterY - this.verticalRadius - 0.5D)
                : lowestFloorY;
    }

    private BlockPos findStoneMushroomGround(
            WorldGenLevel level,
            int x,
            int z,
            int expectedGroundY
    ) {
        BlockPos.MutableBlockPos ground = new BlockPos.MutableBlockPos();
        for (int y = expectedGroundY + MUSHROOM_GROUND_SEARCH_ABOVE;
             y >= expectedGroundY - MUSHROOM_GROUND_SEARCH_BELOW;
             y--) {
            ground.set(x, y, z);
            if (!level.getBlockState(ground).is(Blocks.STONE)
                    || !level.getBlockState(ground.above()).isAir()
                    || sectionFor(ground.above()) != Section.DESERT) {
                continue;
            }

            return ground.immutable();
        }

        return null;
    }

    private void placeGuaranteedSoulSandValleyPatches(WorldGenLevel level, BoundingBox box) {
        List<PatchSample> allSamples = spiralPathSamples();
        List<PatchSample> soulSandValleySamples = soulSandValleyPathSamples(allSamples);

        if (soulSandValleySamples.isEmpty()) {
            return;
        }

        placeGuaranteedSoulSandValleyPatchType(
                level,
                box,
                allSamples,
                soulSandValleySamples,
                FOUR_BY_FOUR_PATCH_OFFSETS,
                NETHER_WART_PATCH_INNER_FRACTIONS,
                NETHER_WART_PATCH_PATH_FRACTIONS,
                Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 3),
                true
        );
        placeGuaranteedSoulSandValleyPatchType(
                level,
                box,
                allSamples,
                soulSandValleySamples,
                FOUR_BY_FOUR_PATCH_OFFSETS,
                SOUL_BERRY_PATCH_INNER_FRACTIONS,
                SOUL_BERRY_PATCH_PATH_FRACTIONS,
                ModBlocks.SOUL_BERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3),
                true
        );
        placeGuaranteedSoulSandValleyPatchType(
                level,
                box,
                allSamples,
                soulSandValleySamples,
                EYEBLOSSOM_PATCH_OFFSETS,
                EYEBLOSSOM_PATCH_INNER_FRACTIONS,
                EYEBLOSSOM_PATCH_PATH_FRACTIONS,
                ModBlocks.CLOSED_EYEBLOSSOM.get().defaultBlockState(),
                false
        );
    }

    private void placeGuaranteedPaleOaks(
            WorldGenLevel level,
            BoundingBox box,
            ChunkPos chunkPos
    ) {
        PaleOakLayoutKey layoutKey = new PaleOakLayoutKey(this.seed, this.origin.asLong());
        List<PaleOakAnchor> anchors = PALE_OAK_LAYOUT_CACHE.computeIfAbsent(
                layoutKey,
                ignored -> buildGuaranteedPaleOakLayout()
        );
        if (anchors.isEmpty()) {
            return;
        }

        ConfiguredFeature<?, ?> paleOak = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(PALE_OAK_CAVE_FEATURE);
        ChunkGenerator generator = level.getLevel().getChunkSource().getGenerator();

        for (PaleOakAnchor anchor : anchors) {
            BlockPos root = anchor.root();
            if ((root.getX() >> 4) != chunkPos.x
                    || (root.getZ() >> 4) != chunkPos.z
                    || !box.isInside(root)) {
                continue;
            }

            long treeSeed = this.seed
                    ^ root.asLong()
                    ^ (anchor.outerRing() ? PALE_OAK_OUTER_SALT : PALE_OAK_INNER_SALT);
            paleOak.place(level, generator, RandomSource.create(treeSeed), root);
        }
    }

    private List<PaleOakAnchor> buildGuaranteedPaleOakLayout() {
        List<PatchSample> allSamples = spiralPathSamples();
        List<PatchSample> soulSandValleySamples = soulSandValleyPathSamples(allSamples);
        if (soulSandValleySamples.isEmpty()) {
            return List.of();
        }

        Set<Long> protectedPatchColumns = guaranteedSoulSandValleyPatchColumns(
                allSamples,
                soulSandValleySamples
        );
        double tunnelRadius = getTunnelHorizontalRadius();
        double pillarRadius = this.centralPillarDiameter * 0.5D;
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double innerRingOuterRadius = pathRadius - this.liquidRadius;
        double outerRingInnerRadius = pathRadius + this.liquidRadius;
        double outerRingOuterRadius = pathRadius + tunnelRadius;
        double innerArea = Math.PI * (
                innerRingOuterRadius * innerRingOuterRadius - pillarRadius * pillarRadius
        );
        double outerArea = Math.PI * (
                outerRingOuterRadius * outerRingOuterRadius
                        - outerRingInnerRadius * outerRingInnerRadius
        );
        int innerCount = Math.max(
                1,
                Mth.floor(
                        PALE_OAK_REFERENCE_INNER_COUNT
                                * innerArea / PALE_OAK_REFERENCE_INNER_AREA
                                + 0.5D
                )
        );
        int outerCount = Math.max(
                1,
                Mth.floor(innerCount * outerArea / innerArea + 0.5D)
        );

        List<PaleOakAnchor> anchors = new ArrayList<>(innerCount + outerCount);
        addPaleOakRingAnchors(
                anchors,
                allSamples,
                soulSandValleySamples,
                protectedPatchColumns,
                innerCount,
                pillarRadius + PALE_OAK_WALL_CLEARANCE,
                innerRingOuterRadius - PALE_OAK_WALL_CLEARANCE,
                PALE_OAK_INNER_SALT,
                false
        );
        addPaleOakRingAnchors(
                anchors,
                allSamples,
                soulSandValleySamples,
                protectedPatchColumns,
                outerCount,
                outerRingInnerRadius + PALE_OAK_WALL_CLEARANCE,
                outerRingOuterRadius - PALE_OAK_WALL_CLEARANCE,
                PALE_OAK_OUTER_SALT,
                true
        );
        return List.copyOf(anchors);
    }

    private void addPaleOakRingAnchors(
            List<PaleOakAnchor> anchors,
            List<PatchSample> allSamples,
            List<PatchSample> soulSandValleySamples,
            Set<Long> protectedPatchColumns,
            int count,
            double minimumRadius,
            double maximumRadius,
            long salt,
            boolean outerRing
    ) {
        RandomSource random = RandomSource.create(this.seed ^ this.origin.asLong() ^ salt);

        for (int slot = 0; slot < count; slot++) {
            PaleOakAnchor selected = null;

            for (int attempt = 0; attempt < PALE_OAK_LAYOUT_ATTEMPTS_PER_SLOT; attempt++) {
                double pathFraction = (slot + 0.2D + random.nextDouble() * 0.6D) / count;
                int sampleIndex = Mth.clamp(
                        Mth.floor(pathFraction * soulSandValleySamples.size()),
                        0,
                        soulSandValleySamples.size() - 1
                );
                PatchSample sample = soulSandValleySamples.get(sampleIndex);
                double areaFraction = random.nextDouble();
                double radius = Math.sqrt(
                        minimumRadius * minimumRadius
                                + areaFraction * (
                                maximumRadius * maximumRadius - minimumRadius * minimumRadius
                        )
                );
                int x = Mth.floor(this.pillarCenterX + Math.cos(sample.yaw()) * radius + 0.5D);
                int z = Mth.floor(this.pillarCenterZ + Math.sin(sample.yaw()) * radius + 0.5D);
                int groundY = soulSandValleyFloorYAt(allSamples, sample, x, z);

                if (!isValidPaleOakAnchor(
                        anchors,
                        allSamples,
                        sample,
                        protectedPatchColumns,
                        x,
                        groundY,
                        z
                )) {
                    continue;
                }

                selected = new PaleOakAnchor(new BlockPos(x, groundY + 1, z), outerRing);
                break;
            }

            if (selected != null) {
                anchors.add(selected);
            }
        }
    }

    private boolean isValidPaleOakAnchor(
            List<PaleOakAnchor> existingAnchors,
            List<PatchSample> allSamples,
            PatchSample sample,
            Set<Long> protectedPatchColumns,
            int x,
            int groundY,
            int z
    ) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                int footprintX = x + dx;
                int footprintZ = z + dz;
                int footprintGroundY = soulSandValleyFloorYAt(
                        allSamples,
                        sample,
                        footprintX,
                        footprintZ
                );

                if (footprintGroundY != groundY
                        || sectionFor(footprintX, groundY + 1, footprintZ)
                        != Section.SOUL_SAND_VALLEY) {
                    return false;
                }
            }
        }

        for (int checkX = x - PALE_OAK_PATCH_CLEARANCE;
             checkX <= x + 1 + PALE_OAK_PATCH_CLEARANCE;
             checkX++) {
            for (int checkZ = z - PALE_OAK_PATCH_CLEARANCE;
                 checkZ <= z + 1 + PALE_OAK_PATCH_CLEARANCE;
                 checkZ++) {
                if (protectedPatchColumns.contains(ChunkPos.asLong(checkX, checkZ))) {
                    return false;
                }
            }
        }

        int minimumSpacingSquared = PALE_OAK_MINIMUM_SPACING * PALE_OAK_MINIMUM_SPACING;
        for (PaleOakAnchor existing : existingAnchors) {
            int dx = existing.root().getX() - x;
            int dz = existing.root().getZ() - z;
            if (dx * dx + dz * dz < minimumSpacingSquared) {
                return false;
            }
        }

        return true;
    }

    private Set<Long> guaranteedSoulSandValleyPatchColumns(
            List<PatchSample> allSamples,
            List<PatchSample> soulSandValleySamples
    ) {
        Set<Long> columns = new HashSet<>();
        addGuaranteedPatchColumns(
                columns,
                allSamples,
                soulSandValleySamples,
                FOUR_BY_FOUR_PATCH_OFFSETS,
                NETHER_WART_PATCH_INNER_FRACTIONS,
                NETHER_WART_PATCH_PATH_FRACTIONS,
                true
        );
        addGuaranteedPatchColumns(
                columns,
                allSamples,
                soulSandValleySamples,
                FOUR_BY_FOUR_PATCH_OFFSETS,
                SOUL_BERRY_PATCH_INNER_FRACTIONS,
                SOUL_BERRY_PATCH_PATH_FRACTIONS,
                true
        );
        addGuaranteedPatchColumns(
                columns,
                allSamples,
                soulSandValleySamples,
                EYEBLOSSOM_PATCH_OFFSETS,
                EYEBLOSSOM_PATCH_INNER_FRACTIONS,
                EYEBLOSSOM_PATCH_PATH_FRACTIONS,
                false
        );
        return columns;
    }

    private void addGuaranteedPatchColumns(
            Set<Long> columns,
            List<PatchSample> allSamples,
            List<PatchSample> soulSandValleySamples,
            int[][] offsets,
            double[] innerBankInsetFractions,
            double[] pathFractions,
            boolean requireFlatGround
    ) {
        List<PatchAnchor> anchors = soulSandValleyInnerPatchAnchors(
                soulSandValleySamples,
                innerBankInsetFractions,
                pathFractions,
                offsets
        );

        for (PatchAnchor anchor : anchors) {
            FlatPatchAnchor flatAnchor = requireFlatGround
                    ? findFlatSoulSandValleyPatchAnchor(allSamples, anchor, offsets)
                    : null;
            if (requireFlatGround && flatAnchor == null) {
                continue;
            }

            int centerX = flatAnchor == null ? anchor.x() : flatAnchor.x();
            int centerZ = flatAnchor == null ? anchor.z() : flatAnchor.z();
            for (int[] offset : offsets) {
                columns.add(ChunkPos.asLong(centerX + offset[0], centerZ + offset[1]));
            }
        }
    }

    private void placeGuaranteedSoulSandValleyPatchType(
            WorldGenLevel level,
            BoundingBox box,
            List<PatchSample> allSamples,
            List<PatchSample> soulSandValleySamples,
            int[][] offsets,
            double[] innerBankInsetFractions,
            double[] pathFractions,
            BlockState state,
            boolean requireFlatGround
    ) {
        List<PatchAnchor> anchors = soulSandValleyInnerPatchAnchors(
                soulSandValleySamples,
                innerBankInsetFractions,
                pathFractions,
                offsets
        );

        for (PatchAnchor anchor : anchors) {
            FlatPatchAnchor flatAnchor = requireFlatGround
                    ? findFlatSoulSandValleyPatchAnchor(allSamples, anchor, offsets)
                    : null;
            if (requireFlatGround && flatAnchor == null) {
                continue;
            }

            int centerX = flatAnchor == null ? anchor.x() : flatAnchor.x();
            int centerZ = flatAnchor == null ? anchor.z() : flatAnchor.z();
            for (int[] offset : offsets) {
                int x = centerX + offset[0];
                int z = centerZ + offset[1];

                if (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ()) {
                    continue;
                }

                int groundY = flatAnchor == null
                        ? soulSandValleyFloorYAt(allSamples, anchor.sample(), x, z)
                        : flatAnchor.groundY();
                BlockPos placementPos = new BlockPos(x, groundY + 1, z);

                if (box.isInside(placementPos)) {
                    level.setBlock(placementPos, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private FlatPatchAnchor findFlatSoulSandValleyPatchAnchor(
            List<PatchSample> allSamples,
            PatchAnchor nominalAnchor,
            int[][] offsets
    ) {
        FlatPatchSearchKey cacheKey = new FlatPatchSearchKey(
                this.seed,
                this.origin.asLong(),
                nominalAnchor.x(),
                nominalAnchor.z(),
                Double.doubleToLongBits(nominalAnchor.sample().yaw()),
                Double.doubleToLongBits(nominalAnchor.sample().centerY()),
                Arrays.deepHashCode(offsets)
        );
        return FLAT_PATCH_ANCHOR_CACHE.computeIfAbsent(
                cacheKey,
                ignored -> Optional.ofNullable(searchFlatSoulSandValleyPatchAnchor(
                        nearbyPatchSamples(allSamples, nominalAnchor.sample()),
                        nominalAnchor,
                        offsets
                ))
        ).orElse(null);
    }

    private List<PatchSample> nearbyPatchSamples(
            List<PatchSample> allSamples,
            PatchSample anchorSample
    ) {
        List<PatchSample> nearbySamples = new ArrayList<>();
        double maximumCenterDistance = this.verticalRadius + 1.0D;

        for (PatchSample sample : allSamples) {
            if (Math.abs(sample.centerY() - anchorSample.centerY()) <= maximumCenterDistance) {
                nearbySamples.add(sample);
            }
        }

        return nearbySamples;
    }

    private FlatPatchAnchor searchFlatSoulSandValleyPatchAnchor(
            List<PatchSample> nearbySamples,
            PatchAnchor nominalAnchor,
            int[][] offsets
    ) {
        Map<Long, PatchFloorCell> floorCache = new HashMap<>();
        Set<Long> visitedCenters = new HashSet<>();
        FlatPatchAnchor bestAnchor = null;
        int bestBalancedClearance = -1;
        int bestTotalClearance = -1;
        int bestRadialOffset = Integer.MAX_VALUE;
        int bestTangentialOffset = Integer.MAX_VALUE;
        double radialX = Math.cos(nominalAnchor.sample().yaw());
        double radialZ = Math.sin(nominalAnchor.sample().yaw());
        double tangentX = -radialZ;
        double tangentZ = radialX;
        int[] radialOffsets = {0, -1, 1, -2, 2};

        for (int radialOffset : radialOffsets) {
            for (int tangentialOffset = -FLAT_PATCH_SEARCH_RADIUS;
                 tangentialOffset <= FLAT_PATCH_SEARCH_RADIUS;
                 tangentialOffset++) {
                int centerX = nominalAnchor.x() + Mth.floor(
                        radialX * radialOffset + tangentX * tangentialOffset + 0.5D
                );
                int centerZ = nominalAnchor.z() + Mth.floor(
                        radialZ * radialOffset + tangentZ * tangentialOffset + 0.5D
                );
                if (!visitedCenters.add(ChunkPos.asLong(centerX, centerZ))) {
                    continue;
                }

                Integer commonGroundY = flatPatchGroundY(
                        nearbySamples,
                        nominalAnchor.sample(),
                        offsets,
                        centerX,
                        centerZ,
                        floorCache
                );
                if (commonGroundY == null) {
                    continue;
                }

                TangentialFlatSpan span = tangentialFlatSpan(
                        nearbySamples,
                        nominalAnchor.sample(),
                        offsets,
                        centerX,
                        centerZ,
                        commonGroundY,
                        tangentX,
                        tangentZ,
                        floorCache
                );
                int balancedClearance = Math.min(span.negative(), span.positive());
                int totalClearance = span.negative() + span.positive();
                int radialDistance = Math.abs(radialOffset);
                int tangentialDistance = Math.abs(tangentialOffset);

                if (balancedClearance > bestBalancedClearance
                        || (balancedClearance == bestBalancedClearance
                        && totalClearance > bestTotalClearance)
                        || (balancedClearance == bestBalancedClearance
                        && totalClearance == bestTotalClearance
                        && radialDistance < bestRadialOffset)
                        || (balancedClearance == bestBalancedClearance
                        && totalClearance == bestTotalClearance
                        && radialDistance == bestRadialOffset
                        && tangentialDistance < bestTangentialOffset)) {
                    bestAnchor = new FlatPatchAnchor(centerX, centerZ, commonGroundY);
                    bestBalancedClearance = balancedClearance;
                    bestTotalClearance = totalClearance;
                    bestRadialOffset = radialDistance;
                    bestTangentialOffset = tangentialDistance;
                }
            }
        }

        return bestAnchor;
    }

    private Integer flatPatchGroundY(
            List<PatchSample> nearbySamples,
            PatchSample anchorSample,
            int[][] offsets,
            int centerX,
            int centerZ,
            Map<Long, PatchFloorCell> floorCache
    ) {
        Integer commonGroundY = null;

        for (int[] offset : offsets) {
            PatchFloorCell cell = patchFloorCell(
                    nearbySamples,
                    anchorSample,
                    centerX + offset[0],
                    centerZ + offset[1],
                    floorCache
            );
            if (!cell.soulSandValley()) {
                return null;
            }

            if (commonGroundY == null) {
                commonGroundY = cell.groundY();
            } else if (cell.groundY() != commonGroundY) {
                return null;
            }
        }

        return commonGroundY;
    }

    private TangentialFlatSpan tangentialFlatSpan(
            List<PatchSample> nearbySamples,
            PatchSample anchorSample,
            int[][] offsets,
            int centerX,
            int centerZ,
            int groundY,
            double tangentX,
            double tangentZ,
            Map<Long, PatchFloorCell> floorCache
    ) {
        int negative = tangentialFlatClearance(
                nearbySamples,
                anchorSample,
                offsets,
                centerX,
                centerZ,
                groundY,
                -1,
                tangentX,
                tangentZ,
                floorCache
        );
        int positive = tangentialFlatClearance(
                nearbySamples,
                anchorSample,
                offsets,
                centerX,
                centerZ,
                groundY,
                1,
                tangentX,
                tangentZ,
                floorCache
        );
        return new TangentialFlatSpan(negative, positive);
    }

    private int tangentialFlatClearance(
            List<PatchSample> nearbySamples,
            PatchSample anchorSample,
            int[][] offsets,
            int centerX,
            int centerZ,
            int groundY,
            int direction,
            double tangentX,
            double tangentZ,
            Map<Long, PatchFloorCell> floorCache
    ) {
        for (int clearance = 1; clearance <= FLAT_PATCH_CENTERING_RADIUS; clearance++) {
            int shiftedCenterX = centerX + Mth.floor(
                    tangentX * clearance * direction + 0.5D
            );
            int shiftedCenterZ = centerZ + Mth.floor(
                    tangentZ * clearance * direction + 0.5D
            );
            Integer shiftedGroundY = flatPatchGroundY(
                    nearbySamples,
                    anchorSample,
                    offsets,
                    shiftedCenterX,
                    shiftedCenterZ,
                    floorCache
            );
            if (shiftedGroundY == null || shiftedGroundY != groundY) {
                return clearance - 1;
            }
        }

        return FLAT_PATCH_CENTERING_RADIUS;
    }

    private PatchFloorCell patchFloorCell(
            List<PatchSample> nearbySamples,
            PatchSample anchorSample,
            int x,
            int z,
            Map<Long, PatchFloorCell> floorCache
    ) {
        return floorCache.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> {
            int groundY = soulSandValleyFloorYAt(nearbySamples, anchorSample, x, z);
            boolean soulSandValley = sectionFor(x, groundY + 1, z) == Section.SOUL_SAND_VALLEY;
            return new PatchFloorCell(groundY, soulSandValley);
        });
    }

    private List<PatchAnchor> soulSandValleyInnerPatchAnchors(
            List<PatchSample> soulSandValleySamples,
            double[] innerBankInsetFractions,
            double[] pathFractions,
            int[][] offsets
    ) {
        if (soulSandValleySamples.isEmpty() || pathFractions.length == 0) {
            return List.of();
        }

        List<PatchAnchor> anchors = new ArrayList<>(pathFractions.length);

        for (int i = 0; i < pathFractions.length; i++) {
            int index = Mth.clamp(
                    Mth.floor(pathFractions[i] * soulSandValleySamples.size()),
                    0,
                    soulSandValleySamples.size() - 1
            );
            double insetFraction = innerBankInsetFractions[
                    i % innerBankInsetFractions.length
            ];
            double distance = scaledInnerRingDistance(
                    insetFraction,
                    minimumPatchBoundaryClearance(offsets)
            );
            anchors.add(soulSandValleyPatchAnchor(soulSandValleySamples.get(index), distance));
        }

        return anchors;
    }

    private double minimumPatchBoundaryClearance(int[][] offsets) {
        int maximumOffset = 0;
        for (int[] offset : offsets) {
            maximumOffset = Math.max(
                    maximumOffset,
                    Math.max(Math.abs(offset[0]), Math.abs(offset[1]))
            );
        }
        return maximumOffset + 1.0D;
    }

    private double scaledInnerRingDistance(double fraction, double boundaryClearance) {
        double effectiveClearance = Math.min(
                boundaryClearance,
                this.horizontalRadius * 0.5D
        );
        return Mth.clamp(
                this.horizontalRadius * fraction,
                effectiveClearance,
                this.horizontalRadius - effectiveClearance
        );
    }

    private List<PatchSample> spiralPathSamples() {
        List<PatchSample> samples = new ArrayList<>();

        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double turnPerStep = -turnPerStepForPathRadius(pathRadius);
        double stepsPerTurn = FULL_TURN / Math.abs(turnPerStep);
        double requiredVerticalSeparation = this.verticalRadius * 2.0D
                + this.minFloorThickness
                + 1.0D
                + this.liquidDepth;
        double dropPerStep = Mth.clamp(requiredVerticalSeparation / stepsPerTurn, 0.01D, 0.85D);
        double finalCenterY = this.endY + this.verticalRadius + this.liquidDepth;
        int maxSteps = Math.max(1, Mth.ceil((this.origin.getY() - finalCenterY) / dropPerStep) + 1);

        double yaw = YAW_START;
        double centerY = this.origin.getY();

        for (int step = 0; step < maxSteps; step++) {
            samples.add(new PatchSample(yaw, centerY));

            centerY -= dropPerStep;
            yaw += turnPerStep;
        }

        return samples;
    }

    private List<PatchSample> soulSandValleyPathSamples(List<PatchSample> allSamples) {
        List<PatchSample> samples = new ArrayList<>();

        for (PatchSample sample : allSamples) {
            double floorAirY = sample.centerY() - this.verticalRadius;
            if (floorAirY <= this.maxSoulSandValleyY && floorAirY > this.maxWarpedForestY) {
                samples.add(sample);
            }
        }

        return samples;
    }

    private PatchAnchor soulSandValleyPatchAnchor(PatchSample sample, double distanceInsideInnerBank) {
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double patchRadius = pathRadius - this.liquidRadius - distanceInsideInnerBank;

        int x = Mth.floor(this.pillarCenterX + Math.cos(sample.yaw()) * patchRadius + 0.5D);
        int z = Mth.floor(this.pillarCenterZ + Math.sin(sample.yaw()) * patchRadius + 0.5D);
        return new PatchAnchor(sample, x, z);
    }

    private int soulSandValleyFloorYAt(
            List<PatchSample> allSamples,
            PatchSample anchorSample,
            int x,
            int z
    ) {
        double pathRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        );
        double tunnelRadius = getTunnelHorizontalRadius();
        int lowestFloorY = Integer.MAX_VALUE;

        for (PatchSample sample : allSamples) {
            if (Math.abs(sample.centerY() - anchorSample.centerY()) > this.verticalRadius + 1.0D) {
                continue;
            }

            double centerX = this.pillarCenterX + Math.cos(sample.yaw()) * pathRadius;
            double centerZ = this.pillarCenterZ + Math.sin(sample.yaw()) * pathRadius;
            double relativeX = (x + 0.5D - centerX) / tunnelRadius;
            double relativeZ = (z + 0.5D - centerZ) / tunnelRadius;
            double remainingVerticalShape = 1.0D - relativeX * relativeX - relativeZ * relativeZ;

            if (remainingVerticalShape <= 0.0D) {
                continue;
            }

            double floorScale = Math.pow(remainingVerticalShape, 0.25D);
            double lowerBoundaryY = sample.centerY() - this.verticalRadius * floorScale;
            lowestFloorY = Math.min(lowestFloorY, Mth.floor(lowerBoundaryY - 0.5D));
        }

        if (lowestFloorY != Integer.MAX_VALUE) {
            return lowestFloorY;
        }

        return Mth.floor(anchorSample.centerY() - this.verticalRadius - 0.5D);
    }

    private boolean isNearBiomeLayerCenter(BlockPos pos, int upperFloorAirY, int lowerFloorAirY) {
        double centerY = (upperFloorAirY + lowerFloorAirY) * 0.5D;
        return Math.abs(pos.getY() - centerY) <= NETHER_MOB_BIOME_CENTER_HALF_HEIGHT;
    }

    private boolean isNearNetherMobRadius(BlockPos pos, int upperFloorAirY, int lowerFloorAirY) {
        if (!isNearBiomeLayerCenter(pos, upperFloorAirY, lowerFloorAirY)) {
            return false;
        }

        double dx = pos.getX() + 0.5D - this.pillarCenterX;
        double dz = pos.getZ() + 0.5D - this.pillarCenterZ;
        double distanceFromPillar = scaledInnerRingDistance(
                NETHER_MOB_INNER_RING_FRACTION,
                NETHER_MOB_RADIAL_HALF_WIDTH + 1.0D
        );
        double targetRadius = this.centralPillarDiameter * 0.5D + distanceFromPillar;
        double radialDistance = Math.sqrt(dx * dx + dz * dz);
        return Math.abs(radialDistance - targetRadius) <= NETHER_MOB_RADIAL_HALF_WIDTH;
    }

    @Override
    protected void configureGuaranteedMob(Mob mob, int structureSpawnIndex) {
        if (mob.getType() != EntityType.STRIDER) {
            return;
        }

        for (var passenger : List.copyOf(mob.getPassengers())) {
            if (passenger.getType() == EntityType.STRIDER
                    && passenger instanceof AgeableMob ageableMob
                    && ageableMob.isBaby()) {
                // Bypass LivingEntity.stopRiding(), whose safe-dismount search
                // can synchronously request another chunk during worldgen.
                passenger.removeVehicle();
                passenger.discard();
            }
        }
    }

    @Override
    protected void afterPostProcess(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos
    ) {
        placeGuaranteedSugarCaneLine(level, box);
        placeGuaranteedMushroomPatches(level, box);
        placeGuaranteedSoulSandValleyPatches(level, box);
        placeGuaranteedPaleOaks(level, box, chunkPos);
        double fullUpperRadiusTurn = SOUTHBOUND_UPWARD_SPIRAL_TURNS
                + UPPER_VERTICAL_RADIUS_INCREASE / upperSpiralRisePerTurn();
        double camelGroupUpwardTurns = Math.min(
                UPPER_SPIRAL_TURNS,
                fullUpperRadiusTurn + CAMEL_GROUP_TURNS_AFTER_FULL_RADIUS
        );
        double rabbitGroupUpwardTurns = Math.min(
                UPPER_SPIRAL_TURNS,
                fullUpperRadiusTurn + RABBIT_GROUP_TURNS_AFTER_FULL_RADIUS
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level,
                random,
                box,
                EntityType.CAMEL,
                4,
                state -> state.is(Blocks.SAND),
                pos -> isNearSkyExposedSandyAnimalGroup(
                        level,
                        pos,
                        camelGroupUpwardTurns
                )
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level,
                random,
                box,
                EntityType.RABBIT,
                8,
                state -> state.is(Blocks.SAND),
                pos -> isNearSkyExposedSandyAnimalGroup(
                        level,
                        pos,
                        rabbitGroupUpwardTurns
                )
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, EntityType.HOGLIN, 4,
                state -> state.is(Blocks.CRIMSON_NYLIUM),
                pos -> isNearNetherMobRadius(pos, this.maxCrimsonForestY, this.maxBasaltDeltasY)
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, EntityType.STRIDER, 4,
                state -> state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL),
                pos -> isNearNetherMobRadius(pos, this.maxSoulSandValleyY, this.maxWarpedForestY)
        );
        spawnGuaranteedMobsStrictlyOncePerStructure(
                level, random, box, AMEntityRegistry.WARPED_TOAD.get(), 4,
                state -> state.is(Blocks.WARPED_NYLIUM),
                pos -> isNearNetherMobRadius(pos, this.maxWarpedForestY, this.maxCrimsonForestY)
        );
    }

    private Section sectionFor(BlockPos pos) {
        return sectionFor(pos.getX(), pos.getY(), pos.getZ());
    }

    private Section sectionFor(int x, int y, int z) {
        return NetherCaveSectionClassifier.sectionForExactCave(
                x,
                y,
                z,
                this.origin.getX(),
                this.origin.getY(),
                this.origin.getZ(),
                this.horizontalRadius,
                this.verticalRadius,
                this.centralPillarDiameter,
                this.minFloorThickness,
                this.liquidDepth,
                this.liquidRadius,
                this.endX,
                this.maxDeepDarkY,
                this.maxSoulSandValleyY,
                this.maxWarpedForestY,
                this.maxCrimsonForestY,
                this.maxBasaltDeltasY
        );
    }

    private boolean isWaterLavaBuffer(double caveCenterY) {
        int caveFloorY = Mth.floor(caveCenterY - this.verticalRadius);

        return caveFloorY >= this.maxSoulSandValleyY - WATER_LAVA_BUFFER_SOUL_SAND_FLOORS + 1
                && caveFloorY <= this.maxSoulSandValleyY + WATER_LAVA_BUFFER_DEEP_DARK_FLOORS;
    }

    private record PatchAnchor(PatchSample sample, int x, int z) {
    }

    private record FlatPatchAnchor(int x, int z, int groundY) {
    }

    private record FlatPatchSearchKey(
            long seed,
            long origin,
            int nominalX,
            int nominalZ,
            long sampleYaw,
            long sampleCenterY,
            int offsetsHash
    ) {
    }

    private record PatchFloorCell(int groundY, boolean soulSandValley) {
    }

    private record TangentialFlatSpan(int negative, int positive) {
    }

    private record PaleOakLayoutKey(long seed, long origin) {
    }

    private record PaleOakAnchor(BlockPos root, boolean outerRing) {
    }

    private record PatchSample(double yaw, double centerY) {
    }

}
