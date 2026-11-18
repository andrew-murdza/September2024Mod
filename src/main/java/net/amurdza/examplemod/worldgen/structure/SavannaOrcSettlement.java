package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Ground contact for the single, precomposed curved settlement template. */
public final class SavannaOrcSettlement {
    public static final int MIN_X = 0, MAX_X = 89, MIN_Z = 0, MAX_Z = 77;
    // Site selection admits an eight-block height range across the camp. Allow
    // another four steps for the uninterrupted grade beyond the measured sites.
    public static final int RAMP_MARGIN = 12;
    private static final int[][] CORE_DISTANCES = distancesToCore();
    public static final BlockPos[] HOUSE_CENTERS = {
            new BlockPos(45, 3, 20), new BlockPos(17, 3, 21), new BlockPos(73, 3, 21),
            new BlockPos(17, 3, 57), new BlockPos(30, 3, 57), new BlockPos(60, 3, 57), new BlockPos(73, 3, 57)};

    private SavannaOrcSettlement() { }

    public static BoundingBox bounds(BlockPos origin, int templateTop, boolean rebuilt) {
        return bounds(origin, templateTop, rebuilt, rebuilt);
    }

    static BoundingBox bounds(BlockPos origin, int templateTop, boolean rebuilt, boolean ramps) {
        if (!rebuilt) return Legacy.bounds(origin, templateTop);
        int margin = ramps ? RAMP_MARGIN : 0;
        return new BoundingBox(origin.getX() + MIN_X - margin, origin.getY() - (ramps ? 13 : 8), origin.getZ() + MIN_Z - margin,
                origin.getX() + MAX_X + margin, Math.max(templateTop, origin.getY() + 17), origin.getZ() + MAX_Z + margin);
    }

    /** Same rounded ground mask as scripts/generate-orc-settlement.py. */
    public static boolean containsGround(int x, int z) {
        double dx = (x - 45) / 44.0, dz = (z - 39) / 37.0;
        return dx * dx + dz * dz <= 1.0;
    }

    public static boolean containsEntrance(int x, int z) {
        return x >= 42 && x <= 48 && z >= 69 && z <= 77;
    }

    public static void place(WorldGenLevel level, BoundingBox chunk, BlockPos origin, boolean rebuilt) {
        place(level, chunk, origin, rebuilt, rebuilt);
    }

    static void place(WorldGenLevel level, BoundingBox chunk, BlockPos origin, boolean rebuilt, boolean ramps) {
        if (!rebuilt) {
            Legacy.place(level, chunk, origin);
            return;
        }
        // The template includes the paths and fields. Plant the four strips using
        // the same durable camp selection that determines the livestock and chest food.
        placeCultivatedPlots(level, chunk, origin);
        if (ramps) {
            placeRamps(level, chunk, origin);
            return;
        }
        // Previously saved settlements retain the original seven-wide approach.
        // The seven-wide southern opening joins the natural surface in steps.
        // Native houses, crop plots, both stock pens, and the one curved wall
        // already live in the template; no rectangular side yards are appended.
        if (chunk.maxZ() < origin.getZ() + 69 || chunk.minZ() > origin.getZ() + 77) return;
        for (int x = Math.max(42, chunk.minX() - origin.getX()); x <= Math.min(48, chunk.maxX() - origin.getX()); x++) {
            int outside = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    origin.getX() + x, origin.getZ() + 78) - 1 - origin.getY();
            outside = Math.max(-6, Math.min(2, outside));
            for (int z = 69; z <= 77; z++) {
                int floor = Math.max(outside, 2 - (z - 69));
                BlockPos top = origin.offset(x, floor, z);
                if (!chunk.isInside(top)) continue;
                level.setBlock(top, Blocks.DIRT_PATH.defaultBlockState(), 2);
                for (int y = floor + 1; y <= 7; y++) {
                    BlockPos clear = origin.offset(x, y, z);
                    if (chunk.isInside(clear)) level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
                }
                supportEntrance(level, chunk, top.below(), origin.getY());
            }
        }
    }

    private static int[][] distancesToCore() {
        int width = MAX_X + 1 + 2 * RAMP_MARGIN;
        int depth = MAX_Z + 1 + 2 * RAMP_MARGIN;
        int[][] distances = new int[width][depth];
        // A forward/backward Manhattan distance transform gives the shortest number
        // of horizontal steps to the actual curved lawn, including its jagged corners.
        for (int x = 0; x < width; x++) for (int z = 0; z < depth; z++) {
            int localX = x - RAMP_MARGIN, localZ = z - RAMP_MARGIN;
            int distance = containsGround(localX, localZ) && !containsEntrance(localX, localZ) ? 0 : width + depth;
            if (x > 0) distance = Math.min(distance, distances[x - 1][z] + 1);
            if (z > 0) distance = Math.min(distance, distances[x][z - 1] + 1);
            distances[x][z] = distance;
        }
        for (int x = width - 1; x >= 0; x--) for (int z = depth - 1; z >= 0; z--) {
            if (x + 1 < width) distances[x][z] = Math.min(distances[x][z], distances[x + 1][z] + 1);
            if (z + 1 < depth) distances[x][z] = Math.min(distances[x][z], distances[x][z + 1] + 1);
        }
        return distances;
    }

    private static void placeRamps(WorldGenLevel level, BoundingBox chunk, BlockPos origin) {
        for (int x = Math.max(-RAMP_MARGIN, chunk.minX() - origin.getX()); x <= Math.min(MAX_X + RAMP_MARGIN, chunk.maxX() - origin.getX()); x++) {
            for (int z = Math.max(-RAMP_MARGIN, chunk.minZ() - origin.getZ()); z <= Math.min(MAX_Z + RAMP_MARGIN, chunk.maxZ() - origin.getZ()); z++) {
                int coreDistance = CORE_DISTANCES[x + RAMP_MARGIN][z + RAMP_MARGIN];
                if (coreDistance == 0) continue;
                if (containsEntrance(x, z)) {
                    BlockPos path = origin.offset(x, 1, z);
                    if (!chunk.isInside(path)) continue;
                    level.setBlock(path, Blocks.DIRT_PATH.defaultBlockState(), 2);
                    // Clear only the former floor and doorway, retaining the gate beam above it.
                    for (int dy = 1; dy <= 5; dy++) if (chunk.isInside(path.above(dy))) {
                        level.setBlock(path.above(dy), Blocks.AIR.defaultBlockState(), 2);
                    }
                    supportEntrance(level, chunk, path.below(), origin.getY());
                    continue;
                }
                int entryDistance = Math.max(0, Math.max(42 - x, x - 48)) + Math.max(0, Math.max(69 - z, z - 77));
                if (Math.min(coreDistance, entryDistance) > RAMP_MARGIN) continue;
                // As soon as the unmodified terrain lies between the possible cut/fill
                // grades it is already reachable. Never sample or write another chunk.
                int lower = Math.max(2 - coreDistance, 1 - entryDistance);
                int upper = Math.min(2 + coreDistance, 1 + entryDistance);
                BlockPos surface = findGround(level, chunk, origin, x, z);
                if (surface == null) continue;
                int nativeFloor = surface.getY() - origin.getY();
                int floor = Math.max(lower, Math.min(upper, nativeFloor));
                if (floor == nativeFloor) continue;
                BlockPos top = origin.offset(x, floor, z);
                if (!chunk.isInside(top)) continue;
                if (nativeFloor < floor) {
                    for (int y = surface.getY() + 1; y < top.getY(); y++) {
                        level.setBlock(new BlockPos(top.getX(), y, top.getZ()), Blocks.DIRT.defaultBlockState(), 2);
                    }
                } else {
                    for (int y = top.getY() + 1; y <= surface.getY(); y++) {
                        level.setBlock(new BlockPos(top.getX(), y, top.getZ()), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                boolean path = x >= 42 && x <= 48 && z > 77;
                level.setBlock(top, (path ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
            }
        }
    }

    private static BlockPos findGround(WorldGenLevel level, BoundingBox chunk, BlockPos origin, int x, int z) {
        int top = Math.min(origin.getY() + RAMP_MARGIN + 2, Math.min(level.getMaxBuildHeight() - 1, chunk.maxY()));
        int bottom = Math.max(origin.getY() - RAMP_MARGIN, Math.max(chunk.minY(), level.getMinBuildHeight()));
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
            var state = level.getBlockState(pos);
            if (state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND)
                    || state.is(Blocks.MUD) || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT_PATH)) return pos;
        }
        return null;
    }

    private static void placeCultivatedPlots(WorldGenLevel level, BoundingBox chunk, BlockPos origin) {
        int stock = OrcCampStock.selection(level.getSeed(), origin);
        for (int plot = 0; plot < 2; plot++) {
            int minX = plot == 0 ? 30 : 52;
            for (int x = minX; x <= minX + 8; x++) for (int z = 28; z <= 50; z++) {
                BlockPos soil = origin.offset(x, 2, z);
                if (!chunk.isInside(soil)) continue;
                if (x == minX + 4) {
                    level.setBlock(soil, Blocks.WATER.defaultBlockState(), 18);
                } else {
                    level.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7), 18);
                    int strip = plot * 2 + (x < minX + 4 ? 0 : 1);
                    level.setBlock(soil.above(), OrcCampStock.matureCrop(stock, strip), 18);
                }
            }
        }
    }

    private static void supportEntrance(WorldGenLevel level, BoundingBox chunk, BlockPos top, int templateBottom) {
        for (int y = top.getY(); y >= Math.max(templateBottom - 8, level.getMinBuildHeight()); y--) {
            BlockPos pos = new BlockPos(top.getX(), y, top.getZ());
            if (!chunk.isInside(pos)) break;
            var state = level.getBlockState(pos);
            if (state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && state.isCollisionShapeFullBlock(level, pos)) {
                // Authored soil can hang above a hillside. Follow it to the first solid terrain below the template.
                if (y < templateBottom) break;
                continue;
            }
            level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
        }
    }

    // Saved pieces keep their original footprint and chunk behavior. New settlements
    // never enter this compatibility implementation.
    private static final class Legacy {
        public static final int MIN_X = -18, MAX_X = 51, MIN_Z = 0, MAX_Z = 41;
        public static final BlockPos[] HOUSE_CENTERS = {new BlockPos(-11, 3, 10), new BlockPos(-11, 3, 31),
                new BlockPos(44, 3, 10), new BlockPos(44, 3, 31)};
    
        private Legacy() { }
    
        public static BoundingBox bounds(BlockPos origin, int nativeTop) {
            return new BoundingBox(origin.getX() + MIN_X, origin.getY() - 8, origin.getZ(),
                    origin.getX() + MAX_X, Math.max(nativeTop, origin.getY() + 10), origin.getZ() + MAX_Z);
        }
    
        public static void place(WorldGenLevel level, BoundingBox chunk, BlockPos origin) {
            for (int x = Math.max(MIN_X, chunk.minX() - origin.getX()); x <= Math.min(MAX_X, chunk.maxX() - origin.getX()); x++) {
                for (int z = Math.max(MIN_Z, chunk.minZ() - origin.getZ()); z <= Math.min(MAX_Z, chunk.maxZ() - origin.getZ()); z++) {
                    if (x >= 0 && x <= 33) {
                        // Open both original side palisades into the added yards.
                        if (x <= 6 && z >= 23 && z <= 25 || x >= 27 && z >= 7 && z <= 9) {
                            for (int y = 3; y <= 7; y++) put(level, chunk, origin, x, y, z, Blocks.AIR.defaultBlockState());
                            put(level, chunk, origin, x, 2, z, Blocks.DIRT_PATH.defaultBlockState());
                        }
                        continue;
                    }
                    for (int y = 0; y <= 2; y++) put(level, chunk, origin, x, y, z,
                            (y == 2 ? Blocks.GRASS_BLOCK : y == 1 ? Blocks.DIRT : Blocks.MUD).defaultBlockState());
                    for (int y = 3; y <= 10; y++) put(level, chunk, origin, x, y, z, Blocks.AIR.defaultBlockState());
                    support(level, chunk, origin.offset(x, -1, z));
                    boolean path = (x >= -4 && x < 0 || x > 33 && x <= 37) && z >= 8 && z <= 33;
                    if (path) put(level, chunk, origin, x, 2, z, Blocks.DIRT_PATH.defaultBlockState());
                    boolean alongZ = x == MIN_X || x == MAX_X;
                    boolean alongX = z == MIN_Z || z == MAX_Z;
                    boolean entrance = x == MIN_X && z >= 19 && z <= 23;
                    if ((alongZ || alongX) && !entrance) {
                        BlockState fence = ModBlocks.ACACIA_PALISADE.get().defaultBlockState()
                                .setValue(WallBlock.UP, true)
                                .setValue(WallBlock.NORTH_WALL, alongZ && z > MIN_Z ? WallSide.LOW : WallSide.NONE)
                                .setValue(WallBlock.SOUTH_WALL, alongZ && z < MAX_Z ? WallSide.LOW : WallSide.NONE)
                                .setValue(WallBlock.WEST_WALL, alongX && x > MIN_X ? WallSide.LOW : WallSide.NONE)
                                .setValue(WallBlock.EAST_WALL, alongX && x < MAX_X ? WallSide.LOW : WallSide.NONE);
                        put(level, chunk, origin, x, 3, z, SavannaOrcCampProcessor.normalPalisade(fence));
                    }
                }
            }
            for (int i = 0; i < HOUSE_CENTERS.length; i++) placeHouse(level, chunk, origin, HOUSE_CENTERS[i], i);
            // Five-wide western entry joins untouched outside ground with at most
            // one block per step, even when the camp levels a four-block hillside.
            for (int z = 19; z <= 23; z++) {
                int outside = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        origin.getX() + MIN_X - 1, origin.getZ() + z) - 1 - origin.getY();
                outside = Math.max(-2, Math.min(2, outside));
                for (int x = MIN_X; x <= MIN_X + 4; x++) {
                    int floor = Math.min(2, outside + x - MIN_X);
                    put(level, chunk, origin, x, floor, z, Blocks.DIRT_PATH.defaultBlockState());
                    for (int y = floor + 1; y <= 7; y++) put(level, chunk, origin, x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    
        private static void placeHouse(WorldGenLevel level, BoundingBox chunk, BlockPos origin, BlockPos center, int index) {
            int door = center.getX() < 0 ? 5 : -5;
            for (int dx = -5; dx <= 5; dx++) for (int dz = -6; dz <= 6; dz++) {
                int x = center.getX() + dx, z = center.getZ() + dz;
                put(level, chunk, origin, x, 2, z, Blocks.ACACIA_PLANKS.defaultBlockState());
                int roof = 7 + (5 - Math.abs(dx)) / 2;
                boolean wall = Math.abs(dx) == 5 || Math.abs(dz) == 6;
                boolean doorway = dx == door && Math.abs(dz) <= 1;
                for (int y = 3; y <= roof; y++) {
                    BlockState state = y == roof ? Blocks.BLACK_WOOL.defaultBlockState()
                            : wall && !(doorway && y <= 5) ? (Math.abs(dx) == 5 && Math.abs(dz) == 6
                            ? Blocks.STRIPPED_ACACIA_LOG : Blocks.ACACIA_PLANKS).defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                    put(level, chunk, origin, x, y, z, state);
                }
            }
            // Unobstructed three-wide doors meet the continuous side-yard paths.
            for (int dx = door; Math.abs(dx) <= 7; dx += Integer.signum(door)) for (int dz = -1; dz <= 1; dz++) {
                put(level, chunk, origin, center.getX() + dx, 2, center.getZ() + dz, Blocks.ACACIA_PLANKS.defaultBlockState());
            }
            BlockPos chestPos = origin.offset(center.getX() - Integer.signum(door) * 3, 3, center.getZ() - 4);
            if (chunk.isInside(chestPos)) {
                level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,
                        door > 0 ? Direction.EAST : Direction.WEST), 2);
                if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                    chest.setLootTable(SavannaOrcCampProcessor.LOOT_TABLE, chestPos.asLong() ^ index * 937L);
                }
            }
            // Bone end caps continue the native camp palette without introducing new materials.
            for (int dz : new int[]{-6, 6}) put(level, chunk, origin, center.getX(), 9, center.getZ() + dz,
                    Blocks.BONE_BLOCK.defaultBlockState());
        }
    
        private static void support(WorldGenLevel level, BoundingBox chunk, BlockPos top) {
            for (int y = top.getY(); y >= Math.max(level.getMinBuildHeight(), chunk.minY()); y--) {
                BlockPos pos = new BlockPos(top.getX(), y, top.getZ());
                var state = level.getBlockState(pos);
                if (state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && state.isCollisionShapeFullBlock(level, pos)) break;
                if (chunk.isInside(pos)) level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
            }
        }
    
        private static void put(WorldGenLevel level, BoundingBox chunk, BlockPos origin, int x, int y, int z, BlockState state) {
            BlockPos pos = origin.offset(x, y, z);
            if (chunk.isInside(pos)) level.setBlock(pos, state, 2);
        }
    }
}
