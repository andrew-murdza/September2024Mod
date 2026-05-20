package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class LongNetherLayerCaveStructure extends Structure {

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

                            Codec.FLOAT
                                    .fieldOf("floor_level")
                                    .forGetter(s -> s.floorLevel),

                            Codec.INT
                                    .fieldOf("lava_level")
                                    .forGetter(s -> s.lavaLevel),

                            ResourceLocation.CODEC
                                    .fieldOf("replaceable")
                                    .forGetter(s -> s.replaceable),

                            Codec.floatRange(0.0F, Float.MAX_VALUE)
                                    .fieldOf("central_pillar_diameter_extra")
                                    .forGetter(s -> s.centralPillarDiameterExtra),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("min_floor_thickness")
                                    .forGetter(s -> s.minFloorThickness),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("pitch_lower")
                                    .forGetter(s -> s.pitchLower),

                            Codec.INT
                                    .fieldOf("max_deep_dark_y")
                                    .forGetter(s -> s.maxDeepDarkY),

                            Codec.INT
                                    .fieldOf("max_soul_sand_valley_y")
                                    .forGetter(s -> s.maxSoulSandValleyY),

                            Codec.INT
                                    .fieldOf("max_warped_forest_y")
                                    .forGetter(s -> s.maxWarpedForestY),

                            Codec.INT
                                    .fieldOf("max_crimson_forest_y")
                                    .forGetter(s -> s.maxCrimsonForestY),

                            Codec.INT
                                    .fieldOf("max_basalt_deltas_y")
                                    .forGetter(s -> s.maxCrimsonForestY)

                    ).apply(instance, LongNetherLayerCaveStructure::new)
            );

    private final int startY;
    private final float horizontalRadiusMultiplier;
    private final float verticalRadiusMultiplier;
    private final float floorLevel;
    private final int lavaLevel;
    private final ResourceLocation replaceable;
    private final float centralPillarDiameterExtra;
    private final float minFloorThickness;
    private final float pitchLower;

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
            float floorLevel,
            int lavaLevel,
            ResourceLocation replaceable,
            float centralPillarDiameterExtra,
            float minFloorThickness,
            float pitchLower,
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY
    ) {
        super(settings);

        this.startY = startY;
        this.horizontalRadiusMultiplier = horizontalRadiusMultiplier;
        this.verticalRadiusMultiplier = verticalRadiusMultiplier;
        this.floorLevel = floorLevel;
        this.lavaLevel = lavaLevel;
        this.replaceable = replaceable;
        this.centralPillarDiameterExtra = centralPillarDiameterExtra;
        this.minFloorThickness = minFloorThickness;
        this.pitchLower = pitchLower;

        this.maxDeepDarkY = maxDeepDarkY;
        this.maxSoulSandValleyY = maxSoulSandValleyY;
        this.maxWarpedForestY = maxWarpedForestY;
        this.maxCrimsonForestY = maxCrimsonForestY;
        this.maxBasaltDeltasY = maxBasaltDeltasY;
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
                        this.floorLevel,
                        this.lavaLevel,
                        this.replaceable,
                        this.centralPillarDiameterExtra,
                        this.minFloorThickness,
                        this.pitchLower,
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