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

public class LongMushroomCaveStructure extends Structure {

    public static final MapCodec<LongMushroomCaveStructure> CODEC =
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

                            Codec.INT
                                    .fieldOf("max_mushroom_caves")
                                    .forGetter(s -> s.maxMushroomCaves),

                            Codec.INT
                                    .fieldOf("min_mushroom_caves")
                                    .forGetter(s -> s.minMushroomCaves),

                            PlacedFeature.LIST_CODEC
                                    .optionalFieldOf("placed_features", HolderSet.direct(List.of()))
                                    .forGetter(s -> s.placedFeatures)
                    ).apply(instance, LongMushroomCaveStructure::new)
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

    private final int maxMushroomCaves;
    private final int minMushroomCaves;

    private final HolderSet<PlacedFeature> placedFeatures;

    public LongMushroomCaveStructure(
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
            int maxMushroomCaves,
            int minMushroomCaves,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(settings);

        this.startY = startY;
        this.endY = endY;
        this.endX = endX;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;
        this.maxMushroomCaves = maxMushroomCaves;
        this.minMushroomCaves = minMushroomCaves;
        this.placedFeatures = placedFeatures;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        int tunnelStartY = this.startY + Mth.ceil(this.verticalRadius + 1.5F);

        BlockPos origin = new BlockPos(
                chunkPos.getMiddleBlockX(),
                tunnelStartY,
                chunkPos.getMiddleBlockZ()
        );

        return Optional.of(new GenerationStub(
                new BlockPos(chunkPos.getMiddleBlockX(), tunnelStartY, chunkPos.getMiddleBlockZ()),
                builder -> builder.addPiece(new MushroomCavePiece(
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
                        this.maxMushroomCaves,
                        this.minMushroomCaves,
                        this.placedFeatures
                ))
        ));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.MUSHROOM_CAVE.get();
    }
}