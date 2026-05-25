package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.worldgen.feature.AllSurfacesFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class LongNetherLayerCaveStructure extends Structure {

    public record BiomeCutoffs(
            int maxDeepDarkY,
            int maxSoulSludgeY,
            int maxSoulSoilY,
            int maxSoulSandY,
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
                                        .fieldOf("max_soul_sludge_y")
                                        .forGetter(BiomeCutoffs::maxSoulSludgeY),

                                Codec.INT
                                        .fieldOf("max_soul_soil_y")
                                        .forGetter(BiomeCutoffs::maxSoulSoilY),

                                Codec.INT
                                        .fieldOf("max_soul_sand_y")
                                        .forGetter(BiomeCutoffs::maxSoulSandY),

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

    public static final MapCodec<LongNetherLayerCaveStructure> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            settingsCodec(instance),

                            Codec.INT.fieldOf("start_y")
                                    .forGetter(s -> s.startY),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("horizontal_radius_multiplier")
                                    .forGetter(s -> s.horizontalRadiusMultiplier),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("vertical_radius_multiplier")
                                    .forGetter(s -> s.verticalRadiusMultiplier),

                            Codec.INT
                                    .fieldOf("lava_level")
                                    .forGetter(s -> s.lavaLevel),

                            Codec.floatRange(0.0F, Float.MAX_VALUE)
                                    .fieldOf("central_pillar_diameter")
                                    .forGetter(s -> s.centralPillarDiameter),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("min_floor_thickness")
                                    .forGetter(s -> s.minFloorThickness),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("pitch_lower")
                                    .forGetter(s -> s.pitchLower),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("pitch_upper")
                                    .forGetter(s -> s.pitchUpper),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("liquid_depth")
                                    .forGetter(s -> s.liquidDepth),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("liquid_radius")
                                    .forGetter(s -> s.liquidRadius),

                            BiomeCutoffs.CODEC
                                    .forGetter(s -> new BiomeCutoffs(
                                            s.maxDeepDarkY,
                                            s.maxSoulSludgeY,
                                            s.maxSoulSoilY,
                                            s.maxSoulSandY,
                                            s.maxWarpedForestY,
                                            s.maxCrimsonForestY,
                                            s.maxBasaltDeltasY
                                    )),

                            AllSurfacesFeatureConfig.CODEC.listOf()
                                    .optionalFieldOf("all_surface_features", List.of())
                                    .forGetter(s -> s.allSurfaceFeatures),

                            ExactSurfaceFeatureConfig.CODEC.listOf()
                                    .optionalFieldOf("exact_surface_features", List.of())
                                    .forGetter(s -> s.exactSurfaceFeatures),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("vertical_radius_upper")
                                    .forGetter(s -> s.verticalRadiusUpper)

                            ).apply(instance, LongNetherLayerCaveStructure::new)
            );

    private final int startY;
    private final float horizontalRadiusMultiplier;
    private final float verticalRadiusMultiplier;
    private final int lavaLevel;
    private final float centralPillarDiameter;
    private final float minFloorThickness;
    private final float pitchLower;
    private final float pitchUpper;
    private final float liquidDepth;
    private final float liquidRadius;

    private final int maxDeepDarkY;
    private final int maxSoulSludgeY;
    private final int maxSoulSoilY;
    private final int maxSoulSandY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltasY;

    private final float verticalRadiusUpper;

    private final List<AllSurfacesFeatureConfig> allSurfaceFeatures;
    private final List<ExactSurfaceFeatureConfig> exactSurfaceFeatures;

    public LongNetherLayerCaveStructure(
            StructureSettings settings,
            int startY,
            float horizontalRadiusMultiplier,
            float verticalRadiusMultiplier,
            int lavaLevel,
            float centralPillarDiameter,
            float minFloorThickness,
            float pitchLower,
            float pitchUpper,
            float liquidDepth,
            float liquidRadius,
            BiomeCutoffs biomeCutoffs,
            List<AllSurfacesFeatureConfig> allSurfaceFeatures,
            List<ExactSurfaceFeatureConfig> exactSurfaceFeatures,
            float verticalRadiusUpper
    ) {
        super(settings);

        this.startY = startY;
        this.horizontalRadiusMultiplier = horizontalRadiusMultiplier;
        this.verticalRadiusMultiplier = verticalRadiusMultiplier;
        this.lavaLevel = lavaLevel;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.pitchLower = pitchLower;
        this.pitchUpper = pitchUpper;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;
        this.verticalRadiusUpper = verticalRadiusUpper;

        this.maxDeepDarkY = biomeCutoffs.maxDeepDarkY();
        this.maxSoulSludgeY = biomeCutoffs.maxSoulSludgeY();
        this.maxSoulSoilY = biomeCutoffs.maxSoulSoilY();
        this.maxSoulSandY = biomeCutoffs.maxSoulSandY();
        this.maxWarpedForestY = biomeCutoffs.maxWarpedForestY();
        this.maxCrimsonForestY = biomeCutoffs.maxCrimsonForestY();
        this.maxBasaltDeltasY = biomeCutoffs.maxBasaltDeltasY();

        this.allSurfaceFeatures = allSurfaceFeatures;
        this.exactSurfaceFeatures = exactSurfaceFeatures;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        BlockPos origin = new BlockPos(chunkPos.getMiddleBlockX(), this.startY, chunkPos.getMiddleBlockZ());

        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(
                new LongNetherLayerCavePiece(
                        origin,
                        context.random().nextLong(),
                        this.horizontalRadiusMultiplier,
                        this.verticalRadiusMultiplier,
                        this.lavaLevel,
                        this.centralPillarDiameter,
                        this.minFloorThickness,
                        this.pitchLower,
                        this.pitchUpper,
                        this.liquidDepth,
                        this.liquidRadius,
                        this.maxDeepDarkY,
                        this.maxSoulSludgeY,
                        this.maxSoulSoilY,
                        this.maxSoulSandY,
                        this.maxWarpedForestY,
                        this.maxCrimsonForestY,
                        this.maxBasaltDeltasY,
                        this.verticalRadiusUpper,
                        this.allSurfaceFeatures,
                        this.exactSurfaceFeatures
                )
        )));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.NETHER_CAVE.get();
    }
}