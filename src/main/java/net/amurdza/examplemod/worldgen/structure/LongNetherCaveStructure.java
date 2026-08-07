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

public class LongNetherCaveStructure extends Structure {
    public static final MapCodec<LongNetherCaveStructure> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            settingsCodec(instance),
                            PlacedFeature.LIST_CODEC
                                    .optionalFieldOf("placed_features", HolderSet.direct(List.of()))
                                    .forGetter(s -> s.placedFeatures)
                    ).apply(instance, LongNetherCaveStructure::new)
            );

    private final HolderSet<PlacedFeature> placedFeatures;

    public LongNetherCaveStructure(
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
                CaveStructureConfig.NETHER_CAVE_START_Y,
                CaveStructureConfig.NETHER_CAVE_VERTICAL_RADIUS
        );
        BlockPos origin = new BlockPos(
                chunkPos.getMinBlockX(),
                tunnelStartY,
                chunkPos.getMinBlockZ()
        );

        return Optional.of(new GenerationStub(
                origin,
                builder -> builder.addPiece(new NetherCavePiece(
                        origin,
                        context.random().nextLong(),
                        CaveStructureConfig.NETHER_CAVE_END_Y,
                        CaveStructureConfig.NETHER_CAVE_END_X,
                        CaveStructureConfig.NETHER_CAVE_HORIZONTAL_RADIUS,
                        CaveStructureConfig.NETHER_CAVE_VERTICAL_RADIUS,
                        CaveStructureConfig.NETHER_CAVE_CENTRAL_PILLAR_DIAMETER,
                        CaveStructureConfig.NETHER_CAVE_MIN_FLOOR_THICKNESS,
                        CaveStructureConfig.NETHER_CAVE_LIQUID_DEPTH,
                        CaveStructureConfig.NETHER_CAVE_LIQUID_RADIUS,
                        CaveStructureConfig.NETHER_CAVE_MAX_DEEP_DARK_Y,
                        CaveStructureConfig.NETHER_CAVE_MAX_SOUL_SAND_VALLEY_Y,
                        CaveStructureConfig.NETHER_CAVE_MAX_WARPED_FOREST_Y,
                        CaveStructureConfig.NETHER_CAVE_MAX_CRIMSON_FOREST_Y,
                        CaveStructureConfig.NETHER_CAVE_MAX_BASALT_DELTAS_Y,
                        this.placedFeatures
                ))
        ));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.NETHER_CAVE.get();
    }
}
