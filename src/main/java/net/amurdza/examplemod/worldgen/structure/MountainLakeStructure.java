package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class MountainLakeStructure extends Structure {

    public static final Codec<MountainLakeStructure> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    settingsCodec(instance),

                    Codec.INT
                            .fieldOf("radius")
                            .forGetter(s -> s.radius),

                    Codec.INT
                            .fieldOf("depth")
                            .forGetter(s -> s.depth),

                    Codec.INT
                            .fieldOf("water_y")
                            .forGetter(s -> s.waterY),

                    BlockState.CODEC
                            .listOf()
                            .fieldOf("seafloor_blocks")
                            .forGetter(s -> s.seafloorBlocks)
            ).apply(instance, MountainLakeStructure::new)
    );

    private final int radius;
    private final int depth;
    private final int waterY;
    private final List<BlockState> seafloorBlocks;

    public MountainLakeStructure(
            StructureSettings settings,
            int radius,
            int depth,
            int waterY,
            List<BlockState> seafloorBlocks
    ) {
        super(settings);
        this.radius = radius;
        this.depth = depth;
        this.waterY = waterY;
        this.seafloorBlocks = seafloorBlocks;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {

        BlockPos chunkCenter = ctx.chunkPos().getWorldPosition().offset(8, 0, 8);

        return Optional.of(new GenerationStub(chunkCenter, (StructurePiecesBuilder builder) -> {
            BlockPos origin = new BlockPos(chunkCenter.getX(), this.waterY, chunkCenter.getZ());

            final int pad = 4;
            int rTotal = this.radius + pad;

            int minX = origin.getX() - rTotal;
            int maxX = origin.getX() + rTotal;
            int minZ = origin.getZ() - rTotal;
            int maxZ = origin.getZ() + rTotal;

            int minY = this.waterY - this.depth - 8;
            int maxY = this.waterY + 1;

            builder.addPiece(new MountainLakePiece(
                    origin,
                    this.waterY,
                    this.radius,
                    this.depth,
                    this.seafloorBlocks,
                    minX, minY, minZ, maxX, maxY, maxZ
            ));
        }));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.MOUNTAIN_LAKE_STRUCTURE.get();
    }
}