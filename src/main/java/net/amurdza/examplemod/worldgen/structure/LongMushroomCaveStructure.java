package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class LongMushroomCaveStructure extends Structure {

    public static final MapCodec<LongMushroomCaveStructure> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            settingsCodec(instance),

                            Codec.INT
                                    .fieldOf("start_y")
                                    .forGetter(s -> s.startY),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("lower_horizontal_radius")
                                    .forGetter(s -> s.lowerHorizontalRadius),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("lower_vertical_radius")
                                    .forGetter(s -> s.lowerVerticalRadius),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("upper_horizontal_radius")
                                    .forGetter(s -> s.upperHorizontalRadius),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("upper_vertical_radius")
                                    .forGetter(s -> s.upperVerticalRadius),

                            Codec.floatRange(0.0F, Float.MAX_VALUE)
                                    .fieldOf("central_pillar_diameter")
                                    .forGetter(s -> s.centralPillarDiameter),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("min_floor_thickness")
                                    .forGetter(s -> s.minFloorThickness),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("upper_pitch")
                                    .forGetter(s -> s.upperPitch),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("liquid_depth")
                                    .forGetter(s -> s.liquidDepth),

                            ExtraCodecs.POSITIVE_FLOAT
                                    .fieldOf("liquid_radius")
                                    .forGetter(s -> s.liquidRadius),

                            Codec.INT
                                    .fieldOf("max_mushroom_caves")
                                    .forGetter(s -> s.maxMushroomCaves),

                            PlacedFeature.LIST_CODEC
                                    .optionalFieldOf("placed_features", HolderSet.direct(List.of()))
                                    .forGetter(s -> s.placedFeatures)
                    ).apply(instance, LongMushroomCaveStructure::new)
            );

    private final int startY;

    private final float lowerHorizontalRadius;
    private final float lowerVerticalRadius;
    private final float upperHorizontalRadius;
    private final float upperVerticalRadius;

    private final float centralPillarDiameter;
    private final float minFloorThickness;
    private final float upperPitch;

    private final float liquidDepth;
    private final float liquidRadius;

    private final int maxMushroomCaves;

    private final HolderSet<PlacedFeature> placedFeatures;

    public LongMushroomCaveStructure(
            StructureSettings settings,
            int startY,
            float lowerHorizontalRadius,
            float lowerVerticalRadius,
            float upperHorizontalRadius,
            float upperVerticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float upperPitch,
            float liquidDepth,
            float liquidRadius,
            int maxMushroomCaves,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(settings);

        this.startY = startY;
        this.lowerHorizontalRadius = lowerHorizontalRadius;
        this.lowerVerticalRadius = lowerVerticalRadius;
        this.upperHorizontalRadius = upperHorizontalRadius;
        this.upperVerticalRadius = upperVerticalRadius;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.upperPitch = upperPitch;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;
        this.maxMushroomCaves = maxMushroomCaves;
        this.placedFeatures = placedFeatures;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        BlockPos origin = new BlockPos(
                chunkPos.getMiddleBlockX(),
                this.startY,
                chunkPos.getMiddleBlockZ()
        );

        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(
                new MushroomCavePiece(
                        origin,
                        context.random().nextLong(),
                        this.lowerHorizontalRadius,
                        this.lowerVerticalRadius,
                        this.upperHorizontalRadius,
                        this.upperVerticalRadius,
                        this.centralPillarDiameter,
                        this.minFloorThickness,
                        this.upperPitch,
                        this.liquidDepth,
                        this.liquidRadius,
                        this.maxMushroomCaves,
                        this.placedFeatures
                )
        )));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.MUSHROOM_CAVE.get();
    }
}