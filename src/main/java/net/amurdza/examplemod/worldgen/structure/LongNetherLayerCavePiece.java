package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public class LongNetherLayerCavePiece extends StructurePiece {

    private static final ResourceLocation DEEP_DARK =
            new ResourceLocation("aoemod", "deep_dark");

    private static final ResourceLocation CRIMSON_FOREST =
            new ResourceLocation("aoemod", "crimson_forest");

    private static final ResourceLocation WARPED_FOREST =
            new ResourceLocation("aoemod", "warped_forest");

    private static final ResourceLocation MUSHROOM_CAVES =
            new ResourceLocation("aoemod", "mushroom_caves");

    private static final TagKey<Biome> NETHER_BIOMES =
            TagKey.create(
                    Registries.BIOME,
                    new ResourceLocation("aoemod", "nether_biomes")
            );

    private static final int TARGET_Y = -127;

    private final BlockPos origin;
    private final long seed;

    private final float horizontalRadius;
    private final float verticalRadius;
    private final float floorLevel;
    private final int lavaLevel;

    private final ResourceLocation replaceable;

    private final float centralPillarDiameterExtra;
    private final float minFloorThickness;

    private final float pitchLower;

    private final float liquidRadius;
    private final float liquidDepth;

    public LongNetherLayerCavePiece(
            BlockPos origin,
            long seed,
            float horizontalRadius,
            float verticalRadius,
            float floorLevel,
            int lavaLevel,
            ResourceLocation replaceable,
            float centralPillarDiameterExtra,
            float minFloorThickness,
            float pitchLower,
            float liquidRadius,
            float liquidDepth
    ) {
        super(
                ModStructures.NETHER_CAVE_PIECE.get(),
                0,
                makeBoundingBox(
                        origin,
                        horizontalRadius,
                        verticalRadius,
                        lavaLevel,
                        centralPillarDiameterExtra,
                        minFloorThickness,
                        pitchLower,
                        liquidRadius,
                        liquidDepth
                )
        );

        this.origin = origin;
        this.seed = seed;

        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.floorLevel = floorLevel;
        this.lavaLevel = lavaLevel;

        this.replaceable = replaceable;

        this.centralPillarDiameterExtra = centralPillarDiameterExtra;
        this.minFloorThickness = minFloorThickness;

        this.pitchLower = pitchLower;

        this.liquidRadius = liquidRadius;
        this.liquidDepth = liquidDepth;
    }

    @SuppressWarnings("unused")
    public LongNetherLayerCavePiece(
            StructurePieceSerializationContext ctx,
            CompoundTag tag
    ) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), tag);

        this.origin = new BlockPos(
                tag.getInt("OriginX"),
                tag.getInt("OriginY"),
                tag.getInt("OriginZ")
        );

        this.seed = tag.getLong("Seed");

        this.horizontalRadius = tag.getFloat("HorizontalRadius");
        this.verticalRadius = tag.getFloat("VerticalRadius");
        this.floorLevel = tag.getFloat("FloorLevel");

        this.lavaLevel = tag.getInt("LavaLevel");

        this.replaceable =
                ResourceLocation.tryParse(tag.getString("Replaceable"));

        this.centralPillarDiameterExtra =
                tag.getFloat("CentralPillarDiameterExtra");

        this.minFloorThickness =
                tag.getFloat("MinFloorThickness");

        this.pitchLower =
                tag.getFloat("PitchLower");

        this.liquidRadius =
                tag.getFloat("LiquidRadius");

        this.liquidDepth =
                tag.getFloat("LiquidDepth");
    }

    private static BoundingBox makeBoundingBox(
            BlockPos origin,
            float horizontalRadius,
            float verticalRadius,
            int lavaLevel,
            float centralPillarDiameterExtra,
            float minFloorThickness,
            float pitchLower,
            float liquidRadius,
            float liquidDepth
    ) {
        float centralPillarDiameter =
                actualCentralPillarDiameter(
                        horizontalRadius,
                        centralPillarDiameterExtra,
                        minFloorThickness
                );

        float pathRadius =
                centralPillarDiameter * 0.5F
                        + horizontalRadius
                        + liquidRadius;

        double lowerVerticalDrop =
                Math.max(0.0D, lavaLevel - TARGET_Y);

        double lowerHorizontalTravel =
                lowerVerticalDrop * pitchLower;

        int totalRadius = Mth.ceil(
                pathRadius
                        + horizontalRadius
                        + liquidRadius
                        + lowerHorizontalTravel
                        + 64.0D
        );

        int minY =TARGET_Y;

        int maxY =
                origin.getY()
                        + Mth.ceil(verticalRadius)
                        + 128;

        return new BoundingBox(
                origin.getX() - totalRadius,
                minY,
                origin.getZ() - totalRadius,
                origin.getX() + totalRadius,
                maxY,
                origin.getZ() + totalRadius
        );
    }

    @Override
    protected void addAdditionalSaveData(
            @NotNull StructurePieceSerializationContext context,
            @NotNull CompoundTag tag
    ) {
        tag.putInt("OriginX", this.origin.getX());
        tag.putInt("OriginY", this.origin.getY());
        tag.putInt("OriginZ", this.origin.getZ());

        tag.putLong("Seed", this.seed);

        tag.putFloat("HorizontalRadius", this.horizontalRadius);
        tag.putFloat("VerticalRadius", this.verticalRadius);
        tag.putFloat("FloorLevel", this.floorLevel);

        tag.putInt("LavaLevel", this.lavaLevel);

        tag.putString("Replaceable", this.replaceable.toString());

        tag.putFloat(
                "CentralPillarDiameterExtra",
                this.centralPillarDiameterExtra
        );

        tag.putFloat(
                "MinFloorThickness",
                this.minFloorThickness
        );

        tag.putFloat(
                "PitchLower",
                this.pitchLower
        );

        tag.putFloat(
                "LiquidRadius",
                this.liquidRadius
        );

        tag.putFloat(
                "LiquidDepth",
                this.liquidDepth
        );
    }

    @Override
    public void postProcess(
            @NotNull WorldGenLevel level,
            @NotNull StructureManager structureManager,
            @NotNull ChunkGenerator generator,
            @NotNull RandomSource random,
            @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos,
            @NotNull BlockPos pivot
    ) {
        RandomSource pathRandom =
                RandomSource.create(this.seed);

        float centralPillarDiameter =
                actualCentralPillarDiameter(
                        this.horizontalRadius,
                        this.centralPillarDiameterExtra,
                        this.minFloorThickness
                );

        double pathRadius =
                centralPillarDiameter * 0.5D
                        + this.horizontalRadius;

        double turnMagnitude =
                turnPerStepForPathRadius(pathRadius);

        double turnPerStep =
                pathRandom.nextBoolean()
                        ? turnMagnitude
                        : -turnMagnitude;

        double stepsPerTurn =
                (Math.PI * 2.0D)
                        / Math.abs(turnPerStep);

        double carvedHeight =
                (1.0D - this.floorLevel)
                        * this.verticalRadius;

        double requiredVerticalSeparation =
                carvedHeight * 1.1D
                        + this.minFloorThickness
                        + this.liquidDepth;

        double minDropPerStep =
                requiredVerticalSeparation
                        / stepsPerTurn;

        minDropPerStep =
                Mth.clamp(minDropPerStep, 0.01D, 0.85D);

        float pitch =
                (float)-Math.asin(minDropPerStep);

        double yaw =
                pathRandom.nextDouble()
                        * Math.PI
                        * 2.0D;

        double x = this.origin.getX();
        double y = this.origin.getY();
        double z = this.origin.getZ();

        int maxSteps = 8192;

        for (int step = 0; step < maxSteps && y > TARGET_Y; step++) {

            double floorY =
                    y + this.floorLevel * this.verticalRadius;

            boolean lowerMode =
                    floorY <= this.lavaLevel;

            carveEllipsoid(
                    level,
                    box,
                    x,
                    y,
                    z,
                    yaw,
                    lowerMode
            );

            if (lowerMode) {
                double horizontalStep = 1.0D;

                double verticalDrop =
                        horizontalStep / this.pitchLower;

                x += Math.cos(yaw) * horizontalStep;
                y -= verticalDrop;
                z += Math.sin(yaw) * horizontalStep;
            }
            else {
                double horizontalStep =
                        Mth.cos(pitch);

                x += Math.cos(yaw) * horizontalStep;
                y += Mth.sin(pitch);
                z += Math.sin(yaw) * horizontalStep;

                yaw += turnPerStep;
            }
        }

        replaceLavaFloorsWithBlackstone(level, box);
    }

    private void carveEllipsoid(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double centerY,
            double centerZ,
            double yaw,
            boolean lowerMode
    ) {
        double localVerticalRadius =
                this.verticalRadius;

        if (lowerMode) {
            double minCeilingY =
                    this.lavaLevel + 7.0D;

            if (centerY + localVerticalRadius < minCeilingY) {
                localVerticalRadius =
                        minCeilingY - centerY;
            }
        }

        double expandedHorizontalRadius =
                this.horizontalRadius
                        + this.liquidRadius;

        double upperHeight =
                localVerticalRadius
                        / Math.sqrt(
                        1.0D - Mth.square(
                                this.liquidRadius
                                        / expandedHorizontalRadius
                        )
                );

        int minX =
                Mth.floor(centerX - expandedHorizontalRadius) - 1;

        int maxX =
                Mth.floor(centerX + expandedHorizontalRadius) + 1;

        int minY =
                Mth.floor(
                        centerY
                                - localVerticalRadius
                                - this.liquidDepth
                                - 2.0D
                ) - 1;

        int maxY =
                Mth.floor(centerY + upperHeight) + 1;

        int minZ =
                Mth.floor(centerZ - expandedHorizontalRadius) - 1;

        int maxZ =
                Mth.floor(centerZ + expandedHorizontalRadius) + 1;

        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    double dx =
                            x + 0.5D - centerX;

                    double dz =
                            z + 0.5D - centerZ;

                    double forward =
                            dx * cosYaw
                                    + dz * sinYaw;

                    double side =
                            -dx * sinYaw
                                    + dz * cosYaw;

                    double dy =
                            y + 0.5D - centerY;

                    if (Math.abs(forward)
                            > this.horizontalRadius) {
                        continue;
                    }

                    double absSide =
                            Math.abs(side);

                    double effectiveSide;

                    if (absSide <= this.liquidRadius) {
                        effectiveSide = 0.0D;
                    }
                    else {
                        effectiveSide =
                                absSide - this.liquidRadius;
                    }

                    double floorY =
                            centerY
                                    + this.floorLevel
                                    * localVerticalRadius;

                    double riverRimY =
                            floorY - 1.0D;

                    double bottomY = floorY;

                    if (absSide <= this.liquidRadius) {
                        double channelT =
                                absSide / this.liquidRadius;

                        bottomY =
                                riverRimY
                                        - this.liquidDepth
                                        * Math.sqrt(
                                        1.0D - channelT * channelT
                                );
                    }

                    if (y + 0.5D <= bottomY) {
                        continue;
                    }

                    double verticalTerm;

                    if (dy >= 0.0D) {

                        verticalTerm =
                                dy / upperHeight;

                        double sideTerm =
                                side / expandedHorizontalRadius;

                        double forwardTerm =
                                forward / this.horizontalRadius;

                        if (
                                sideTerm * sideTerm
                                        + forwardTerm * forwardTerm
                                        + verticalTerm * verticalTerm
                                        >= 1.0D
                        ) {
                            continue;
                        }
                    }
                    else {

                        verticalTerm =
                                dy / localVerticalRadius;

                        double sideTerm =
                                effectiveSide / this.horizontalRadius;

                        double forwardTerm =
                                forward / this.horizontalRadius;

                        if (
                                sideTerm * sideTerm
                                        + forwardTerm * forwardTerm
                                        + verticalTerm * verticalTerm
                                        >= 1.0D
                        ) {
                            continue;
                        }
                    }

                    boolean inLiquidChannel =
                            absSide <= this.liquidRadius
                                    && y + 0.5D > bottomY
                                    && y + 0.5D <= riverRimY;

                    if (!inLiquidChannel && y + 0.5D <= bottomY) {
                        continue;
                    }

// ellipse boundary tests stay here

                    BlockState carvedState;

                    if (inLiquidChannel) {
                        carvedState = getLiquidState(level, pos);
                    }
                    else if (pos.getY() <= this.lavaLevel) {
                        carvedState = Blocks.LAVA.defaultBlockState();
                    }
                    else {
                        carvedState = Blocks.CAVE_AIR.defaultBlockState();
                    }

                    carveBlock(level, pos, carvedState);
                }
            }
        }
    }

    private BlockState getLiquidState(
            WorldGenLevel level,
            BlockPos pos
    ) {
        boolean netherHere =
                level.getBiome(pos)
                        .is(NETHER_BIOMES);

        if (netherHere) {
            return Blocks.LAVA.defaultBlockState();
        }

        boolean netherBelow =
                level.getBiome(pos.below())
                        .is(NETHER_BIOMES);

        if (netherBelow) {
            return Blocks.OBSIDIAN.defaultBlockState();
        }

        return Blocks.WATER.defaultBlockState();
    }

    private void carveBlock(
            WorldGenLevel level,
            BlockPos.MutableBlockPos pos,
            BlockState carvedState
    ) {
        BlockState oldState =
                level.getBlockState(pos);

        if (!canReplace(oldState)
                || pos.getY() < -126) {
            return;
        }

        level.setBlock(
                pos,
                carvedState,
                Block.UPDATE_CLIENTS
        );

        decorateCaveSurface(
                level,
                pos,
                carvedState
        );
    }

    private boolean canReplace(BlockState state) {
        return !state.is(Blocks.BEDROCK);
    }

    private static float actualCentralPillarDiameter(
            float horizontalRadius,
            float centralPillarDiameterExtra,
            float minFloorThickness
    ) {
        return 2.0F * horizontalRadius
                + minFloorThickness
                + centralPillarDiameterExtra;
    }

    private static double turnPerStepForPathRadius(
            double pathRadius
    ) {
        return 1.0D / Math.max(pathRadius, 1.0D);
    }

    private void decorateCaveSurface(
            WorldGenLevel level,
            BlockPos carvedPos,
            BlockState carvedState
    ) {
        ResourceLocation biomeId =
                level.getBiome(carvedPos)
                        .unwrapKey()
                        .map(ResourceKey::location)
                        .orElse(null);

        ResourceLocation belowBiomeId =
                level.getBiome(carvedPos.below())
                        .unwrapKey()
                        .map(ResourceKey::location)
                        .orElse(null);

        if (biomeId == null
                || belowBiomeId == null) {
            return;
        }

        FluidState fluidState =
                carvedState.getFluidState();

        if (biomeId.equals(DEEP_DARK)) {

            replaceAdjacentSolids(
                    level,
                    carvedPos,
                    Blocks.SCULK.defaultBlockState()
            );

            return;
        }

        BlockState floorState =
                getFloorState(
                        belowBiomeId,
                        fluidState
                );

        if (floorState != null) {

            BlockPos below =
                    carvedPos.below();

            BlockState belowState =
                    level.getBlockState(below);

            if (
                    isNaturalReplaceableSurface(
                            level,
                            below,
                            belowState
                    )
            ) {
                level.setBlock(
                        below,
                        floorState,
                        Block.UPDATE_CLIENTS
                );
            }
        }
    }

    private static BlockState getFloorState(
            ResourceLocation biomeId,
            FluidState fluidState
    ) {
        if (biomeId.equals(CRIMSON_FOREST)) {
            return Blocks.CRIMSON_NYLIUM.defaultBlockState();
        }

        if (biomeId.equals(WARPED_FOREST)) {
            return Blocks.WARPED_NYLIUM.defaultBlockState();
        }

        if (biomeId.equals(MUSHROOM_CAVES)) {
            return fluidState.is(Fluids.EMPTY)
                    ? Blocks.MYCELIUM.defaultBlockState()
                    : Blocks.GRAVEL.defaultBlockState();
        }

        return null;
    }

    private static void replaceAdjacentSolids(
            WorldGenLevel level,
            BlockPos carvedPos,
            BlockState replacement
    ) {
        for (Direction direction : Direction.values()) {

            BlockPos sidePos =
                    carvedPos.relative(direction);

            BlockState sideState =
                    level.getBlockState(sidePos);

            if (
                    isNaturalReplaceableSurface(
                            level,
                            sidePos,
                            sideState
                    )
            ) {
                level.setBlock(
                        sidePos,
                        replacement,
                        Block.UPDATE_CLIENTS
                );
            }
        }
    }

    private static boolean isNaturalReplaceableSurface(
            WorldGenLevel level,
            BlockPos pos,
            BlockState state
    ) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(Blocks.CHORUS_PLANT)
                && !state.is(Blocks.CHORUS_FLOWER)
                && !state.is(Blocks.BEDROCK)
                && state.isFaceSturdy(
                level,
                pos,
                Direction.UP
        );
    }

    private void replaceLavaFloorsWithBlackstone(
            WorldGenLevel level,
            BoundingBox box
    ) {
        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {

                    pos.set(x, y, z);

                    BlockState state =
                            level.getBlockState(pos);

                    if (!state.is(Blocks.LAVA)) {
                        continue;
                    }

                    BlockPos below =
                            pos.below();

                    BlockState belowState =
                            level.getBlockState(below);

                    if (shouldBecomeBlackstoneFloor(belowState)) {
                        level.setBlock(
                                below,
                                Blocks.BLACKSTONE.defaultBlockState(),
                                Block.UPDATE_CLIENTS
                        );
                    }
                }
            }
        }
    }

    private static boolean shouldBecomeBlackstoneFloor(
            BlockState state
    ) {
        return state.is(Blocks.GRAVEL)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT);
    }
}