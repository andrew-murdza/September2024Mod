package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class MountainLakePiece extends StructurePiece {

    private final BlockPos center;
    private final int waterY;
    private final int radius;
    private final int depth;
    private final List<BlockState> seafloorBlocks;


    // Normal constructor you create from the Structure
    public MountainLakePiece(BlockPos center, int waterY, int radius, int depth,
                             List<BlockState> seafloorBlocks,
                             int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        super(ModStructures.MOUNTAIN_LAKE_PIECE.get(), 0, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));

        this.center = center;
        this.waterY = waterY;
        this.radius = radius;
        this.depth  = depth;
        this.seafloorBlocks = seafloorBlocks;
    }

    // ✅ NBT/codec constructor REQUIRED by StructurePieceType in 1.20.1
    public MountainLakePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.MOUNTAIN_LAKE_PIECE.get(), tag);

        this.center = new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz"));

        this.waterY = tag.getInt("waterY");
        this.radius = tag.getInt("radius");
        this.depth  = tag.getInt("depth");

        HolderGetter<Block> blocks = ctx.registryAccess().lookupOrThrow(Registries.BLOCK);

        // Read seafloor list
        ListTag list = tag.getList("seafloorBlocks", Tag.TAG_COMPOUND);
        List<BlockState> tmp = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            tmp.add(NbtUtils.readBlockState(blocks, list.getCompound(i)));
        }
        this.seafloorBlocks = tmp;
    }


    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("cx", center.getX());
        tag.putInt("cy", center.getY());
        tag.putInt("cz", center.getZ());

        tag.putInt("waterY", waterY);
        tag.putInt("radius", radius);
        tag.putInt("depth", depth);
        ListTag list = new ListTag();
        for (BlockState s : seafloorBlocks) {
            list.add(NbtUtils.writeBlockState(s));
        }
        tag.put("seafloorBlocks", list);
    }

    @Override
    public void postProcess(@NotNull WorldGenLevel level,
                            @NotNull StructureManager structureManager,
                            @NotNull ChunkGenerator generator,
                            @NotNull RandomSource rand,
                            @NotNull BoundingBox box,
                            @NotNull ChunkPos chunkPos,
                            @NotNull BlockPos pos1) {

        int cx = center.getX();
        int cz= center.getZ();

        final double lakeR2 = (double) radius * radius;

        final int minX = cx - radius;
        final int maxX = cx + radius;
        final int minZ = cz - radius;
        final int maxZ = cz + radius;

        final int minBuild = level.getMinBuildHeight() + 1;
        final int maxBuild = level.getMaxBuildHeight() - 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                double dx = x - cx;
                double dz = z - cz;
                double d2 = dx * dx + dz * dz;

                if (d2 > lakeR2) {
                    continue;
                }

                double edge = Mth.clamp((float) Math.sqrt(d2 / lakeR2), 0f, 1f);

                final int localDepth = (int) Math.round(depth * (1.0 - Math.pow(edge, 1.6)));
                final int localMinY = waterY - localDepth;

                // Fill the lake water column.
                for (int y = localMinY; y <= waterY; y++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    if (y <= minBuild || y >= maxBuild) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (cantReplaceForLake(state)) {
                        continue;
                    }

                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                }

                // Apply seafloor layers directly below the water column.
                if (this.seafloorBlocks == null || this.seafloorBlocks.isEmpty()) {
                    continue;
                }

                for (int i = 0; i < this.seafloorBlocks.size(); i++) {
                    int y = localMinY - 1 - i;

                    if (y < minBuild) {
                        break;
                    }

                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (cantReplaceForLake(state)) {
                        continue;
                    }

                    level.setBlock(pos, this.seafloorBlocks.get(i), 2);
                }
            }
        }
    }

    private boolean cantReplaceForLake(BlockState state) {
        return state.is(Blocks.BEDROCK);
    }
}
