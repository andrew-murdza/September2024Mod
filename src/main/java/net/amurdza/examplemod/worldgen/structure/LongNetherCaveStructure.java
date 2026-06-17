package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class LongNetherCaveStructure extends Structure {

    public record BiomeCutoffs(
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY
    ) {
        private static final MapCodec<BiomeCutoffs> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                Codec.INT
                                        .fieldOf("max_deep_dark_y")
                                        .forGetter(BiomeCutoffs::maxDeepDarkY),

                                Codec.INT
                                        .fieldOf("max_soul_sand_valley_y")
                                        .forGetter(BiomeCutoffs::maxSoulSandValleyY),

                                Codec.INT
                                        .fieldOf("max_warped_forest_y")
                                        .forGetter(BiomeCutoffs::maxWarpedForestY),

                                Codec.INT
                                        .fieldOf("max_crimson_forest_y")
                                        .forGetter(BiomeCutoffs::maxCrimsonForestY),

                                Codec.INT
                                        .fieldOf("max_basalt_deltas_y")
                                        .forGetter(BiomeCutoffs::maxBasaltDeltasY)
                        ).apply(instance, BiomeCutoffs::new)
                );
    }

    private record SpiralExit(
            double x,
            double y,
            double z,
            double fluidCenterY
    ) {
    }

    public static final MapCodec<LongNetherCaveStructure> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            settingsCodec(instance),

                            Codec.INT
                                    .fieldOf("start_y")
                                    .forGetter(s -> s.startY),

                            Codec.INT
                                    .fieldOf("end_y")
                                    .forGetter(s -> s.endY),

                            Codec.INT
                                    .fieldOf("end_x")
                                    .forGetter(s -> s.endX),

                            Codec.INT
                                    .optionalFieldOf("river_end_offset_x", 0)
                                    .forGetter(s -> s.riverEndOffsetX),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("horizontal_radius")
                                    .forGetter(s -> s.horizontalRadius),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("vertical_radius")
                                    .forGetter(s -> s.verticalRadius),

                            Codec.floatRange(0.0F, Float.MAX_VALUE)
                                    .fieldOf("central_pillar_diameter")
                                    .forGetter(s -> s.centralPillarDiameter),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("min_floor_thickness")
                                    .forGetter(s -> s.minFloorThickness),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("liquid_depth")
                                    .forGetter(s -> s.liquidDepth),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("liquid_radius")
                                    .forGetter(s -> s.liquidRadius),

                            BiomeCutoffs.CODEC
                                    .forGetter(s -> new BiomeCutoffs(
                                            s.maxDeepDarkY,
                                            s.maxSoulSandValleyY,
                                            s.maxWarpedForestY,
                                            s.maxCrimsonForestY,
                                            s.maxBasaltDeltasY
                                    )),

                            PlacedFeature.LIST_CODEC
                                    .optionalFieldOf("placed_features", HolderSet.direct(List.of()))
                                    .forGetter(s -> s.placedFeatures)
                    ).apply(instance, LongNetherCaveStructure::new)
            );

    private final int startY;
    private final int endY;
    private final int endX;
    private final int riverEndOffsetX;

    private final float horizontalRadius;
    private final float verticalRadius;

    private final float centralPillarDiameter;
    private final float minFloorThickness;

    private final float liquidDepth;
    private final float liquidRadius;

    private final int maxDeepDarkY;
    private final int maxSoulSandValleyY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltasY;

    private final HolderSet<PlacedFeature> placedFeatures;

    public LongNetherCaveStructure(
            StructureSettings settings,
            int startY,
            int endY,
            int endX,
            int riverEndOffsetX,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius,
            BiomeCutoffs biomeCutoffs,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(settings);

        this.startY = startY;
        this.endY = endY;
        this.endX = endX;
        this.riverEndOffsetX = riverEndOffsetX;

        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;

        this.maxDeepDarkY = biomeCutoffs.maxDeepDarkY();
        this.maxSoulSandValleyY = biomeCutoffs.maxSoulSandValleyY();
        this.maxWarpedForestY = biomeCutoffs.maxWarpedForestY();
        this.maxCrimsonForestY = biomeCutoffs.maxCrimsonForestY();
        this.maxBasaltDeltasY = biomeCutoffs.maxBasaltDeltasY();

        this.placedFeatures = placedFeatures;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        int tunnelStartY = this.startY + Mth.ceil(this.verticalRadius + 1.5F);

        int riverEndX = chunkPos.getMiddleBlockX() + this.riverEndOffsetX;

        double exitOffsetX = computeSpiralExitOffsetX(
                tunnelStartY,
                this.endY,
                this.horizontalRadius,
                this.verticalRadius,
                this.centralPillarDiameter,
                this.minFloorThickness,
                this.liquidDepth,
                this.liquidRadius
        );

        int originX = Mth.floor(riverEndX - this.endX - exitOffsetX);

        BlockPos origin = new BlockPos(
                originX,
                tunnelStartY,
                chunkPos.getMiddleBlockZ()
        );

        SpiralExit exit = computeSpiralExit(
                origin,
                this.endY,
                this.horizontalRadius,
                this.verticalRadius,
                this.centralPillarDiameter,
                this.minFloorThickness,
                this.liquidDepth,
                this.liquidRadius
        );

        double riverStartX = exit.x();
        double riverEnd = riverStartX + this.endX;

        return Optional.of(new GenerationStub(
                new BlockPos(chunkPos.getMiddleBlockX(), tunnelStartY, chunkPos.getMiddleBlockZ()),
                builder -> {
                    builder.addPiece(new NetherCavePiece(
                            origin,
                            context.random().nextLong(),
                            this.endY,
                            this.endX,
                            this.horizontalRadius,
                            this.verticalRadius,
                            this.centralPillarDiameter,
                            this.minFloorThickness,
                            this.liquidDepth,
                            this.liquidRadius,
                            this.maxDeepDarkY,
                            this.maxSoulSandValleyY,
                            this.maxWarpedForestY,
                            this.maxCrimsonForestY,
                            this.maxBasaltDeltasY,
                            this.placedFeatures
                    ));

                    builder.addPiece(new NetherCaveStraightRiverPiece(
                            riverStartX,
                            riverEnd,
                            exit.fluidCenterY(),
                            exit.z(),
                            origin.getY(),
                            this.liquidDepth,
                            this.liquidRadius,
                            this.maxSoulSandValleyY,
                            this.maxDeepDarkY,
                            this.maxWarpedForestY,
                            this.maxCrimsonForestY,
                            this.maxBasaltDeltasY
                    ));
                }
        ));
    }

    private static double computeSpiralExitOffsetX(
            int startY,
            int endY,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius
    ) {
        SpiralExit exit = computeSpiralExit(
                new BlockPos(0, startY, 0),
                endY,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius
        );

        return exit.x();
    }

    private static SpiralExit computeSpiralExit(
            BlockPos origin,
            int endY,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius
    ) {
        float tunnelHorizontalRadius = horizontalRadius + liquidRadius;

        double pathRadius = centralPillarDiameter * 0.5D + tunnelHorizontalRadius;
        double turnPerStep = -AbstractSpiralCavePiece.turnPerStepForPathRadius(pathRadius);

        double stepsPerTurn = (Math.PI * 2.0D) / Math.abs(turnPerStep);

        double carvedHeight = 2.0D * verticalRadius;
        double requiredVerticalSeparation = carvedHeight + minFloorThickness + 1.0D + liquidDepth;

        double minDropPerStep = requiredVerticalSeparation / stepsPerTurn;
        minDropPerStep = Mth.clamp(minDropPerStep, 0.01D, 0.85D);

        double pillarCenterX = origin.getX();
        double pillarCenterZ = origin.getZ() + pathRadius;

        double yaw = 3.0D * Math.PI * 0.5D;
        double y = origin.getY();

        double lastX = pillarCenterX + Math.cos(yaw) * pathRadius;
        double lastY = y;
        double lastZ = pillarCenterZ + Math.sin(yaw) * pathRadius;

        int maxSteps = 4096;

        for (int step = 0; step < maxSteps; step++) {
            double x = pillarCenterX + Math.cos(yaw) * pathRadius;
            double z = pillarCenterZ + Math.sin(yaw) * pathRadius;

            double bottomOfEllipsoid = y - verticalRadius;

            if (bottomOfEllipsoid <= endY + 1) {
                break;
            }

            lastX = x;
            lastY = y;
            lastZ = z;

            y -= minDropPerStep;
            yaw += turnPerStep;
        }

        double finalFluidCenterY = lastY - verticalRadius - 1.0D;

        return new SpiralExit(
                lastX,
                lastY,
                lastZ,
                finalFluidCenterY
        );
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.NETHER_CAVE.get();
    }
}