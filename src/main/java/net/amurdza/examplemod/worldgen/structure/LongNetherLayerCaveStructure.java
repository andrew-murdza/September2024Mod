package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class LongNetherLayerCaveStructure extends Structure {

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
                                    .fieldOf("central_pillar_diameter_extra")
                                    .forGetter(s -> s.centralPillarDiameterExtra),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("min_floor_thickness")
                                    .forGetter(s -> s.minFloorThickness),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("pitch_lower")
                                    .forGetter(s -> s.pitchLower),

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
                                    ))

                    ).apply(instance, LongNetherLayerCaveStructure::new)
            );

    private final int startY;
    private final float horizontalRadiusMultiplier;
    private final float verticalRadiusMultiplier;
    private final int lavaLevel;
    private final float centralPillarDiameterExtra;
    private final float minFloorThickness;
    private final float pitchLower;
    private final float liquidDepth;
    private final float liquidRadius;

    private final int maxDeepDarkY;
    private final int maxSoulSandValleyY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltasY;

    public LongNetherLayerCaveStructure(
            StructureSettings settings,
            int startY,
            float horizontalRadiusMultiplier,
            float verticalRadiusMultiplier,
            int lavaLevel,
            float centralPillarDiameterExtra,
            float minFloorThickness,
            float pitchLower,
            float liquidDepth,
            float liquidRadius,
            BiomeCutoffs biomeCutoffs
    ) {
        super(settings);

        this.startY = startY;
        this.horizontalRadiusMultiplier = horizontalRadiusMultiplier;
        this.verticalRadiusMultiplier = verticalRadiusMultiplier;
        this.lavaLevel = lavaLevel;
        this.centralPillarDiameterExtra = centralPillarDiameterExtra;
        this.minFloorThickness = minFloorThickness;
        this.pitchLower = pitchLower;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;

        this.maxDeepDarkY = biomeCutoffs.maxDeepDarkY();
        this.maxSoulSandValleyY = biomeCutoffs.maxSoulSandValleyY();
        this.maxWarpedForestY = biomeCutoffs.maxWarpedForestY();
        this.maxCrimsonForestY = biomeCutoffs.maxCrimsonForestY();
        this.maxBasaltDeltasY = biomeCutoffs.maxBasaltDeltasY();
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
                        this.centralPillarDiameterExtra,
                        this.minFloorThickness,
                        this.pitchLower,
                        this.liquidDepth,
                        this.liquidRadius,
                        this.maxDeepDarkY,
                        this.maxSoulSandValleyY,
                        this.maxWarpedForestY,
                        this.maxCrimsonForestY,
                        this.maxBasaltDeltasY
                )
        )));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.NETHER_CAVE.get();
    }
}