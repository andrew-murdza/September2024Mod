package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.config.CaveStructureConfig;
import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
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
                            PlacedFeature.LIST_CODEC
                                    .optionalFieldOf("placed_features", HolderSet.direct(List.of()))
                                    .forGetter(s -> s.placedFeatures)
                    ).apply(instance, LongMushroomCaveStructure::new)
            );

    private final HolderSet<PlacedFeature> placedFeatures;

    public LongMushroomCaveStructure(
            StructureSettings settings,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(settings);
        this.placedFeatures = placedFeatures;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int tunnelStartY = CaveStructureConfig.tunnelOriginY(
                CaveStructureConfig.MUSHROOM_CAVE_START_Y,
                CaveStructureConfig.MUSHROOM_CAVE_VERTICAL_RADIUS
        );
        BlockPos origin = new BlockPos(
                chunkPos.getMinBlockX(),
                tunnelStartY,
                chunkPos.getMinBlockZ()
        );

        return Optional.of(new GenerationStub(
                origin,
                builder -> builder.addPiece(new MushroomCavePiece(
                        origin,
                        context.random().nextLong(),
                        CaveStructureConfig.MUSHROOM_CAVE_END_Y,
                        CaveStructureConfig.MUSHROOM_CAVE_END_X,
                        CaveStructureConfig.MUSHROOM_CAVE_HORIZONTAL_RADIUS,
                        CaveStructureConfig.MUSHROOM_CAVE_VERTICAL_RADIUS,
                        CaveStructureConfig.MUSHROOM_CAVE_CENTRAL_PILLAR_DIAMETER,
                        CaveStructureConfig.MUSHROOM_CAVE_MIN_FLOOR_THICKNESS,
                        CaveStructureConfig.MUSHROOM_CAVE_LIQUID_DEPTH,
                        CaveStructureConfig.MUSHROOM_CAVE_LIQUID_RADIUS,
                        CaveStructureConfig.MUSHROOM_CAVE_MAX_MUSHROOM_CAVES,
                        CaveStructureConfig.MUSHROOM_CAVE_MIN_MUSHROOM_CAVES,
                        this.placedFeatures
                ))
        ));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.MUSHROOM_CAVE.get();
    }
}
