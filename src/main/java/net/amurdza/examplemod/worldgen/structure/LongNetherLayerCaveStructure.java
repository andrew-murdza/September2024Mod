package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.AOEMod;
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

                            ExtraCodecs.POSITIVE_FLOAT.fieldOf("liquid_radius").forGetter(s -> s.liquidRadius),
                            ExtraCodecs.POSITIVE_FLOAT.fieldOf("liquid_depth").forGetter(s -> s.liquidDepth)

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
    private final float liquidRadius;
    private final float liquidDepth;

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
            float liquidRadius,
            float liquidDepth
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
        this.liquidRadius = liquidRadius;
        this.liquidDepth = liquidDepth;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        AOEMod.LOGGER.info("NETHER CAVE findGenerationPoint {}", chunkPos);


        BlockPos origin = new BlockPos(
                chunkPos.getMiddleBlockX(),
                this.startY,
                chunkPos.getMiddleBlockZ()
        );

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
                        this.liquidRadius,
                        this.liquidDepth
                )
        )));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.NETHER_CAVE.get();
    }
}