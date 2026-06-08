package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
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

                            ExtraCodecs.POSITIVE_INT
                                    .fieldOf("end_x")
                                    .forGetter(s -> s.endX),


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
        this.endX= endX;
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

        BlockPos origin = new BlockPos(
                chunkPos.getMiddleBlockX()+this.endX,
                this.startY,
                chunkPos.getMiddleBlockZ()
        );

        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(
                new NetherCavePiece(
                        origin,
                        context.random().nextLong(),
                        endY,
                        endX,
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
                )
        )));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.NETHER_CAVE.get();
    }
}