package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** Column-local grading and seam-safe oak/birch crowns around the native mansion footprint. */
final class MansionGrounds {
    static final int FLAT_MARGIN = 64;
    static final int OUTER_MARGIN = FLAT_MARGIN + 12;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private MansionGrounds() { }

    static void place(WorldGenLevel level, ChunkGenerator generator, BoundingBox clip, List<BoundingBox> rooms, MansionBasementPiece basement, int floor) {
        var house = BoundingBox.encapsulatingBoxes(rooms).orElseThrow();
        var shape = new MansionEstateShape(house, basement.layoutVersion());
        var cursor = new BlockPos.MutableBlockPos();
        var overlapping = rooms.stream().filter(box -> box.intersects(clip)).toList();
        for (int x = clip.minX(); x <= clip.maxX(); x++) for (int z = clip.minZ(); z <= clip.maxZ(); z++) {
            double distance = shape.distanceBeyondFlat(x, z) + FLAT_MARGIN;
            cursor.set(x, floor, z);
            if ((!basement.gentleRamp() && distance >= OUTER_MARGIN) || basement.isStairwell(cursor)) continue;
            // Native wall templates include an air padding column; only actual building blocks
            // may preserve an ungraded column, otherwise that padding leaves a trench or cliff.
            // The plain variant sets its room floor at lawn height, while the
            // decorative-wall variant keeps the room floor one block higher.
            var atFloor = level.getBlockState(cursor.set(x, basement.mainFloorY(), z));
            if (overlapping.stream().anyMatch(box -> covers(box, cursor)) && builtMaterial(atFloor)) continue;
            var columnPieces = overlapping.stream().filter(box -> covers(box, cursor)).toList();
            var surface = level instanceof net.minecraft.server.level.ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG;
            int top = level.getHeight(surface, x, z) - 1;
            int natural = top;
            int waterTop = level.getMinBuildHeight() - 1;
            // A canopy from a neighboring feature may already overhang this column.
            while (natural > level.getMinBuildHeight()) {
                var state = level.getBlockState(cursor.set(x, natural, z));
                if (state.getFluidState().is(FluidTags.WATER)) waterTop = Math.max(waterTop, natural);
                if (state.blocksMotion() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) break;
                natural--;
            }
            int target = distance <= FLAT_MARGIN ? floor : basement.gentleRamp()
                    ? MansionRampTerrain.targetHeight(floor, distance - FLAT_MARGIN, natural)
                    : floor + (int) Math.round((natural - floor) * ((distance - FLAT_MARGIN) / 12.0));
            if (basement.gentleRamp() && distance > FLAT_MARGIN && target == natural) continue;
            for (int y = top; y > target; y--) {
                cursor.set(x, y, z);
                // A native foyer can have floor padding directly below a
                // completed upper room or roof. Grade the exposed ground,
                // but do not erase authored blocks in those higher pieces.
                if (columnPieces.stream().anyMatch(box -> box.isInside(cursor))) continue;
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), FLAGS);
            }
            int soilBottom = Math.min(natural, target - 3);
            var cellar = basement.getBoundingBox();
            if (x >= cellar.minX() && x <= cellar.maxX() && z >= cellar.minZ() && z <= cellar.maxZ()) {
                // Fill immediately above the actual authored roof. The mines have
                // a lower ceiling than the rooms, and empty corners of the cellar's
                // bounding box do not supply a roof at all.
                for (int y = Math.min(target, cellar.maxY()); y >= Math.max(soilBottom, cellar.minY()); y--) {
                    cursor.set(x, y, z);
                    if (basement.containsAuthoredSpace(cursor)) { soilBottom = y + 1; break; }
                }
            }
            for (int y = soilBottom; y <= target; y++) {
                cursor.set(x, y, z);
                if (basement.containsAuthoredSpace(cursor)) continue;
                var material = y == target ? Blocks.GRASS_BLOCK : y >= target - 3 ? Blocks.DIRT : Blocks.STONE;
                level.setBlock(cursor, material.defaultBlockState(), FLAGS);
            }
            if (basement.gentleRamp() && distance > FLAT_MARGIN) {
                for (int y = target + 1; y <= waterTop; y++)
                    level.setBlock(cursor.set(x, y, z), Blocks.WATER.defaultBlockState(), FLAGS);
            } else if (distance > FLAT_MARGIN && target < generator.getSeaLevel() - 1 && top < generator.getSeaLevel()) {
                for (int y = target + 1; y < generator.getSeaLevel(); y++) level.setBlock(cursor.set(x, y, z), Blocks.WATER.defaultBlockState(), FLAGS);
            }
        }
        placeTrees(level, clip, house, floor, basement.layoutVersion());
    }

    static void placeTrees(WorldGenLevel level, BoundingBox clip, BoundingBox house, int floor, int version) {
        var shape = new MansionEstateShape(house, version);
        var cursor = new BlockPos.MutableBlockPos();
        // All chunks derive identical nearby centers; each writes only the part of a crown in its own clip.
        for (int gx = Math.floorDiv(clip.minX() - 3, 7); gx <= Math.floorDiv(clip.maxX() + 3, 7); gx++) {
            for (int gz = Math.floorDiv(clip.minZ() - 3, 7); gz <= Math.floorDiv(clip.maxZ() + 3, 7); gz++) {
                var random = RandomSource.create(level.getSeed() ^ net.minecraft.util.Mth.getSeed(gx, 71, gz));
                int tx = gx * 7 + random.nextInt(5), tz = gz * 7 + random.nextInt(5);
                int houseDistance = distance(house, tx, tz);
                if (houseDistance < 4 || shape.distanceBeyondFlat(tx, tz) > -2 || random.nextInt(5) == 0) continue;
                boolean birch = random.nextBoolean();
                int height = 6 + random.nextInt(3);
                var trunk = (birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).defaultBlockState();
                var leaves = (birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState();
                for (int dy = 1; dy <= height + 1; dy++) {
                    int radius = dy < height - 2 ? 0 : dy < height ? 2 : 1;
                    for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                        boolean stem = dx == 0 && dz == 0 && dy <= height;
                        if (!stem && Math.abs(dx) == radius && Math.abs(dz) == radius && (dy >= height || random.nextBoolean())) continue;
                        cursor.set(tx + dx, floor + dy, tz + dz);
                        if (!clip.isInside(cursor)) continue;
                        var state = level.getBlockState(cursor);
                        if (state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced()) level.setBlock(cursor,
                                stem ? trunk : leaves.setValue(LeavesBlock.DISTANCE, Math.abs(dx) + Math.abs(dz) + Math.max(0, dy - height)), FLAGS);
                    }
                }
            }
        }
    }

    private static boolean builtMaterial(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLESTONE_STAIRS) || state.is(Blocks.COBBLESTONE_SLAB)
                || state.is(Blocks.COBBLESTONE_WALL) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.GLASS) || state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS);
    }

    private static boolean covers(BoundingBox box, BlockPos column) {
        return column.getX() >= box.minX() && column.getX() <= box.maxX() && column.getZ() >= box.minZ() && column.getZ() <= box.maxZ();
    }

    private static int distance(BoundingBox house, int x, int z) {
        return Math.max(0, Math.max(Math.max(house.minX() - x, x - house.maxX()), Math.max(house.minZ() - z, z - house.maxZ())));
    }
}
