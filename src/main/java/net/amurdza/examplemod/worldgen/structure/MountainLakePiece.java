package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.NotNull;
import net.minecraft.nbt.NbtUtils;

import java.util.ArrayList;
import java.util.List;


public class MountainLakePiece extends StructurePiece {

    private final BlockPos center;

    private final int waterY;

    private final int radius;
    private final int depth;
    private final int rimH;

    private final int rampW;

    private final List<BlockState> seafloorBlocks;

    private final BlockState rampFill;

    // Normal constructor you create from the Structure
    public MountainLakePiece(BlockPos center, int waterY, int radius, int depth, int rimH,
                             int rampW,
                             List<BlockState> seafloorBlocks,
                             BlockState rampFill,
                             int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        super(ModStructures.MOUNTAIN_LAKE_PIECE.get(), 0, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));

        this.center = center;
        this.waterY = waterY;
        this.radius = radius;
        this.depth  = depth;
        this.rimH   = rimH;

        this.rampW = rampW;
        this.seafloorBlocks = seafloorBlocks;
        this.rampFill = rampFill;
    }

    // ✅ NBT/codec constructor REQUIRED by StructurePieceType in 1.20.1
    public MountainLakePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.MOUNTAIN_LAKE_PIECE.get(), tag);

        this.center = new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz"));

        this.waterY = tag.getInt("waterY");
        this.radius = tag.getInt("radius");
        this.depth  = tag.getInt("depth");
        this.rimH   = tag.getInt("rimH");

        this.rampW = tag.getInt("rampW");

        HolderGetter<Block> blocks = ctx.registryAccess().lookupOrThrow(Registries.BLOCK);

        // Read seafloor list
        ListTag list = tag.getList("seafloorBlocks", Tag.TAG_COMPOUND);
        List<BlockState> tmp = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            tmp.add(NbtUtils.readBlockState(blocks, list.getCompound(i)));
        }
        this.seafloorBlocks = tmp;

        this.rampFill = NbtUtils.readBlockState(blocks, tag.getCompound("rampFill"));
    }


    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("cx", center.getX());
        tag.putInt("cy", center.getY());
        tag.putInt("cz", center.getZ());

        tag.putInt("waterY", waterY);
        tag.putInt("radius", radius);
        tag.putInt("depth", depth);
        tag.putInt("rimH", rimH);

        tag.putInt("rampW", rampW);

// save seafloor list
        ListTag list = new ListTag();
        for (BlockState s : seafloorBlocks) {
            list.add(NbtUtils.writeBlockState(s));
        }
        tag.put("seafloorBlocks", list);

        tag.put("rampFill", NbtUtils.writeBlockState(rampFill));
    }

    @Override
    public void postProcess(@NotNull WorldGenLevel level,
                            @NotNull StructureManager structureManager,
                            @NotNull ChunkGenerator generator,
                            @NotNull RandomSource rand,
                            @NotNull BoundingBox box,
                            @NotNull ChunkPos chunkPos,
                            @NotNull BlockPos pos) {

        carveAndFill(level, center.getX(), center.getZ(), this.waterY, radius, depth, rimH, rand, box);
    }

    private void fillAirBubbles(WorldGenLevel level, int cx, int cz, int waterY, int radius, int depth, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinBuildHeight() + 1, waterY - depth - 2);

        double rr = (double) radius * (double) radius;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx, dz = z - cz;
                if (dx*dx + dz*dz > rr) continue;

                for (int y = minY; y <= waterY; y++) {
                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;

                    BlockState s = level.getBlockState(pos);
                    if (s.isAir()) {
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void carveAndFill(WorldGenLevel level, int cx, int cz, int waterY, int radius, int depth, int rimH,
                              RandomSource rand, BoundingBox box) {

        // NOTE: DO NOT recompute waterY here anymore.
        // It must be stable for all chunks -> computed once in postProcess.

        final int lakeRPlus = radius + 3;
        final double lakeR2 = (double) radius * radius;
        final double lakeRPlus2 = (double) lakeRPlus * lakeRPlus;

        final double rx = radius + rand.nextInt(3);
        final double rz = radius + rand.nextInt(3);

        final int minX = cx - (int)Math.ceil(rx) - 2;
        final int maxX = cx + (int)Math.ceil(rx) + 2;
        final int minZ = cz - (int)Math.ceil(rz) - 2;
        final int maxZ = cz + (int)Math.ceil(rz) + 2;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                double dx = x - cx, dz = z - cz;
                double d2 = dx*dx + dz*dz;
                if (d2 > lakeRPlus2) continue;

                double edge = Mth.clamp((float)Math.sqrt(d2 / lakeR2), 0f, 1f);

                final int localDepth = (int)Math.round(depth * (1.0 - Math.pow(edge, 1.6)));
                final int localRim   = (int)Math.round(rimH * Math.pow(1.0 - edge, 0.7));

                final int localMinY = waterY - localDepth;
                final int localMaxY = waterY + localRim;

                for (int y = localMinY; y <= localMaxY; y++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) continue;
                    if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1) continue;

                    BlockState state = level.getBlockState(pos);
                    if (cantReplaceForLake(state)) continue;

                    if (y <= waterY) level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                    else level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        solidifyFoundation(level, cx, cz, waterY, radius, depth, box);

        cleanupWaterAboveLine(level, cx, cz, waterY, radius, rimH + 10, box);
        clearFloatingLand(level, cx, cz, waterY, radius, this.rampW, box);
        fillAirBubbles(level, cx, cz, waterY, radius, depth, box);
        sealLakeSidesAndBottom(level, cx, cz, waterY, radius, depth, box);
        buildStoneRamp(level, cx, cz, waterY, radius, depth, box);
        normalizeWaterVolume(level, cx, cz, waterY, radius, depth, box);

// ✅ variable-depth seafloor layers
        applySeafloorLayers(level, cx, cz, waterY, radius, this.seafloorBlocks, box);

// ✅ if you're using the raised rim function, keep it last
        buildStoneRim(level, cx, cz, waterY, radius, box);
    }

    private void clearFloatingLand(WorldGenLevel level, int cx, int cz, int waterY, int radius, int rampW, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int maxClearY = Math.min(level.getMaxBuildHeight() - 2, waterY + 90);

        int r = radius + rampW + 6; // ✅ include ramp area
        double rr = (double) r * (double) r;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {

                double dx = x - cx;
                double dz = z - cz;
                if (dx * dx + dz * dz > rr) continue;

                for (int y = waterY + 1; y <= maxClearY; y++) {
                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;

                    BlockState s = level.getBlockState(pos);
                    if (s.isAir()) continue;
                    if (!s.getFluidState().isEmpty()) continue;
                    if (s.is(Blocks.BEDROCK)) continue;

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }



    private void buildStoneRim(WorldGenLevel level, int cx, int cz, int waterY, int radius, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        double r0 = radius * 0.92;
        double r1 = radius * 1.10;
        double r0sq = r0 * r0;
        double r1sq = r1 * r1;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx, dz = z - cz;
                double d2 = dx*dx + dz*dz;
                if (d2 < r0sq || d2 > r1sq) continue;

                // require water at waterY, air at waterY+1, then place rim at waterY+2
                pos.set(x, waterY, z);
                if (!box.isInside(pos) || !level.getBlockState(pos).is(Blocks.WATER)) continue;

                pos.set(x, waterY +1, z);
                if (!box.isInside(pos) || !level.getBlockState(pos).isAir()) continue;

                pos.set(x, waterY, z);
                if (!box.isInside(pos) || !level.getBlockState(pos).isAir()) continue;

                level.setBlock(pos, this.rampFill, 2);
            }
        }
    }


    private void cleanupWaterAboveLine(WorldGenLevel level, int cx, int cz, int waterY, int radius, int up, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = radius + 4;
        double rr = (double) r * (double) r;

        int maxY = Math.min(level.getMaxBuildHeight() - 2, waterY + up);

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx, dz = z - cz;
                if (dx*dx + dz*dz > rr) continue;

                for (int y = waterY + 1; y <= maxY; y++) {
                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;
                    if (level.getBlockState(pos).is(Blocks.WATER)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void sealLakeSidesAndBottom(WorldGenLevel level, int cx, int cz, int waterY, int radius, int depth, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int minY = Math.max(level.getMinBuildHeight() + 1, waterY - depth - 2);
        double rr = (double) radius * (double) radius;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx, dz = z - cz;
                double d2 = dx*dx + dz*dz;
                if (d2 > rr) continue;

                for (int y = minY; y <= waterY; y++) {
                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;

                    if (!level.getBlockState(pos).is(Blocks.WATER)) continue;

                    for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
                        BlockPos n = pos.relative(dir);
                        if (!box.isInside(n)) continue;

                        BlockState ns = level.getBlockState(n);

                        if (ns.isAir()) {
                            level.setBlock(n, Blocks.STONE.defaultBlockState(), 2);
                            continue;
                        }

                        double ndx = n.getX() - cx, ndz = n.getZ() - cz;
                        if (ndx*ndx + ndz*ndz > rr) {
                            if (isAllowedSurface(ns)) {
                                level.setBlock(n, Blocks.STONE.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private int findColumnSurfaceY(WorldGenLevel level, int x, int z, int startY, int minY, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = startY; y >= minY; y--) {
            pos.set(x, y, z);
            if (!box.isInside(pos)) continue;

            BlockState s = level.getBlockState(pos);
            if (s.isAir()) continue;

            if (isAllowedSurface(s)) continue;
            if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)) continue;
            if (s.is(Blocks.SNOW)) continue;

            if (s.getFluidState().isEmpty()) return y;
        }
        return minY;
    }

    private void buildStoneRamp(WorldGenLevel level, int cx, int cz, int waterY, int radius, int depth, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int rampW = this.rampW;
        int outerR = radius + rampW;

        int minY = Math.max(level.getMinBuildHeight() + 1, waterY - depth - 4);
        int topScanStart = Math.min(level.getMaxBuildHeight() - 2, waterY + 90);

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx;
                double dz = z - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > radius && dist <= outerR) {
                    double t = (dist - radius) / (double) rampW;
                    t = Mth.clamp((float) t, 0f, 1f);

                    int colSurface = findColumnSurfaceY(level, x, z, topScanStart, minY, box);

                    int desiredTop = (int) Math.round(Mth.lerp(t, waterY, colSurface));
                    int targetTop = Math.min(colSurface, desiredTop);

                    for (int y = minY; y <= targetTop; y++) {
                        pos.set(x, y, z);
                        if (!box.isInside(pos)) continue;

                        BlockState s = level.getBlockState(pos);
                        boolean shouldFill = s.isAir() || !s.getFluidState().isEmpty() || isAllowedSurface(s);
                        if (shouldFill) level.setBlock(pos, this.rampFill, 2);
                    }

                    for (int y = targetTop + 1; y <= Math.min(topScanStart, targetTop + 30); y++) {
                        pos.set(x, y, z);
                        if (!box.isInside(pos)) continue;

                        BlockState s = level.getBlockState(pos);
                        if (s.isAir()) continue;
                        if (!s.getFluidState().isEmpty()) continue;
                        if (s.is(Blocks.BEDROCK)) continue;

                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void applySeafloorLayers(WorldGenLevel level, int cx, int cz, int waterY, int radius,
                                     List<BlockState> layers, BoundingBox box) {

        if (layers == null || layers.isEmpty()) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double rr = (double) radius * (double) radius;

        int minBuild = level.getMinBuildHeight() + 1;
        int minScanY = Math.max(minBuild, waterY - depth - 6); // scan a bit below expected bottom

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {

                double dx = x - cx, dz = z - cz;
                if (dx*dx + dz*dz > rr) continue;

                // Only columns that have water at the waterline
                pos.set(x, waterY, z);
                if (!box.isInside(pos)) continue;
                if (!level.getBlockState(pos).is(Blocks.WATER)) continue;

                // Find bed top: first non-water block below the water column
                int bedTop = Integer.MIN_VALUE;
                for (int y = waterY; y >= minScanY; y--) {
                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;

                    BlockState s = level.getBlockState(pos);
                    if (s.is(Blocks.WATER)) continue;

                    bedTop = y;
                    break;
                }
                if (bedTop == Integer.MIN_VALUE) continue;

                // Apply layers starting at bedTop and going down
                for (int i = 0; i < layers.size(); i++) {
                    int y = bedTop - i;
                    if (y < minBuild) break;

                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;

                    BlockState cur = level.getBlockState(pos);
                    if (cantReplaceForLake(cur)) continue;

                    level.setBlock(pos, layers.get(i), 2);
                }
            }
        }
    }


    private void normalizeWaterVolume(WorldGenLevel level, int cx, int cz, int waterY, int radius, int depth, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double rr = (double) radius * (double) radius;

        int minY = Math.max(level.getMinBuildHeight() + 1, waterY - depth - 3);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, waterY + 12);

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx, dz = z - cz;
                boolean inside = (dx*dx + dz*dz) <= rr;

                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;

                    BlockState s = level.getBlockState(pos);

                    if (s.is(Blocks.WATER)) {
                        if (!inside || y > waterY) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    } else if (inside && y < waterY && s.isAir()) {
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void solidifyFoundation(WorldGenLevel level, int cx, int cz, int waterY, int radius, int depth, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double rr = (double) radius * (double) radius;

        int pad = 8;
        int yTop = waterY - depth - 1;
        int yBot = Math.max(level.getMinBuildHeight() + 1, yTop - pad);

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx, dz = z - cz;
                if (dx*dx + dz*dz > rr) continue;

                for (int y = yBot; y <= yTop; y++) {
                    pos.set(x, y, z);
                    if (!box.isInside(pos)) continue;

                    BlockState s = level.getBlockState(pos);
                    if (s.is(Blocks.BEDROCK)) continue;

                    if (s.isAir() || !s.getFluidState().isEmpty() || isAllowedSurface(s)) {
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private boolean isAllowedSurface(BlockState s) {
        if (s.is(Blocks.GRASS_BLOCK)) return true;
        if (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE)) return true;
        return s.is(BlockTags.LEAVES) || s.getBlock() instanceof BushBlock;
    }

    private boolean cantReplaceForLake(BlockState state) {
        return state.is(Blocks.BEDROCK)
                || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_PORTAL_FRAME);
    }
}
