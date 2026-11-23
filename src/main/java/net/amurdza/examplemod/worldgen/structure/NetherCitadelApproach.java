package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Fits each approach to the actual outside cavern floor without loading a chunk.
 * All edits remain within the citadel's original radius; rejected mouths are sealed.
 */
public final class NetherCitadelApproach {
    public static final int INNER_RADIUS = 96;
    public static final int OUTER_RADIUS = NetherCitadelLayout.RADIUS;
    public static final int WALK_HALF_WIDTH = 6;
    private static final int[] MAIN_FLOORS = {14, 38, 70, 102};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState TRIM = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private final int[][][] outsideFloors = new int[SIDES.length][MAIN_FLOORS.length][2 * WALK_HALF_WIDTH + 1];
    private final boolean[][] open = new boolean[SIDES.length][MAIN_FLOORS.length];

    private NetherCitadelApproach(BlockPos origin, ChunkGenerator generator, RandomState random,
                                 LevelHeightAccessor height) {
        for (int sideIndex = 0; sideIndex < SIDES.length; sideIndex++) {
            var side = SIDES[sideIndex];
            for (int cross = -WALK_HALF_WIDTH; cross <= WALK_HALF_WIDTH; cross++) {
                int x = origin.getX() + (side.getAxis() == Direction.Axis.X
                        ? side.getStepX() * (OUTER_RADIUS + 1) : cross);
                int z = origin.getZ() + (side.getAxis() == Direction.Axis.Z
                        ? side.getStepZ() * (OUTER_RADIUS + 1) : cross);
                // getBaseColumn evaluates seeded terrain only. It never waits for
                // a FEATURES neighbour or observes the citadel being generated.
                var column = generator.getBaseColumn(x, z, height, random);
                for (int layer = 0; layer < MAIN_FLOORS.length; layer++)
                    outsideFloors[sideIndex][layer][cross + WALK_HALF_WIDTH] = dryFloor(column, layer);
            }
            for (int layer = 0; layer < MAIN_FLOORS.length; layer++) {
                boolean usable = true;
                for (int cross = 0; cross <= 2 * WALK_HALF_WIDTH; cross++) {
                    int floor = outsideFloors[sideIndex][layer][cross];
                    if (floor < 0 || cross > 0
                            && Math.abs(floor - outsideFloors[sideIndex][layer][cross - 1]) > 1) usable = false;
                }
                open[sideIndex][layer] = usable;
            }
        }
    }

    public static NetherCitadelApproach sample(BlockPos origin, ChunkGenerator generator, RandomState random,
                                               LevelHeightAccessor height) {
        return new NetherCitadelApproach(origin, generator, random, height);
    }

    public boolean hasUpperEntrance() {
        for (Direction side : SIDES) if (isOpen(side, 3)) return true;
        return false;
    }

    public boolean isOpen(Direction side, int layer) { return open[sideIndex(side)][layer]; }

    /** Native ground one block beyond the boundary; -1 means solid bank or an unsafe wet exit. */
    public int edgeFloor(Direction side, int layer, int cross) {
        return outsideFloors[sideIndex(side)][layer][Math.max(-WALK_HALF_WIDTH,
                Math.min(WALK_HALF_WIDTH, cross)) + WALK_HALF_WIDTH];
    }

    /** The public profile is also used to audit every step and the unedited terrain seam. */
    public int floorAt(Direction side, int layer, int radius, int cross) {
        int base = MAIN_FLOORS[layer];
        if (!isOpen(side, layer)) return base;
        int distance = Math.max(0, Math.min(OUTER_RADIUS - INNER_RADIUS, radius - INNER_RADIUS));
        return base + Math.floorDiv((edgeFloor(side, layer, cross) - base) * distance,
                OUTER_RADIUS - INNER_RADIUS);
    }

    /** Null defers to the fixed layout. Override cells are only within the existing apron. */
    public BlockState stateAt(int dx, int y, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        Direction side;
        int radius, cross;
        if (ax > INNER_RADIUS && ax <= OUTER_RADIUS && az <= WALK_HALF_WIDTH + 1) {
            side = dx < 0 ? Direction.WEST : Direction.EAST;
            radius = ax; cross = dz;
        } else if (az > INNER_RADIUS && az <= OUTER_RADIUS && ax <= WALK_HALF_WIDTH + 1) {
            side = dz < 0 ? Direction.NORTH : Direction.SOUTH;
            radius = az; cross = dx;
        } else return null;
        for (int layer = 0; layer < MAIN_FLOORS.length; layer++) {
            int base = MAIN_FLOORS[layer];
            if (y < Math.max(NetherCitadelLayout.MIN_Y, base - 10) || y > base + 10) continue;
            if (!isOpen(side, layer)) {
                // A tall solid gate replaces the former open mouth. Its apron may
                // remain outside, but players cannot emerge into rock or lava.
                if (radius == INNER_RADIUS + 1 && y >= base && y <= base + 8) return STONE;
                return null;
            }
            int floor = floorAt(side, layer, radius, cross);
            if (y < floor) return STONE;
            if (y == floor) {
                int inner = floorAt(side, layer, radius - 1, cross);
                int outer = floorAt(side, layer, radius + 1, cross);
                if (Math.abs(cross) <= WALK_HALF_WIDTH && (floor > inner || floor > outer))
                    return Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, floor > inner ? side : side.getOpposite());
                return Math.abs(cross) == 3 ? TRIM : STONE;
            }
            if (Math.abs(cross) == WALK_HALF_WIDTH + 1) {
                if (y == floor + 1) return TRIM;
                if (y == floor + 2 && radius % 6 == 0) return Blocks.GLOWSTONE.defaultBlockState();
            }
            return AIR;
        }
        return null;
    }

    private static int dryFloor(NoiseColumn column, int layer) {
        int desired = MAIN_FLOORS[layer], chosen = -1, bestDistance = Integer.MAX_VALUE;
        int min = Math.max(layer * 32 + 2, desired - 8);
        int max = Math.min(layer * 32 + 27, desired + 8);
        for (int y = min; y <= max; y++) {
            var ground = column.getBlock(y);
            if (ground.isAir() || !ground.getFluidState().isEmpty()) continue;
            if (!column.getBlock(y + 1).isAir() || !column.getBlock(y + 2).isAir()
                    || !column.getBlock(y + 3).isAir()) continue;
            int distance = Math.abs(y - desired);
            if (distance < bestDistance) { chosen = y; bestDistance = distance; }
        }
        return chosen;
    }

    private static int sideIndex(Direction side) {
        return switch (side) {
            case NORTH -> 0; case SOUTH -> 1; case WEST -> 2; case EAST -> 3;
            default -> throw new IllegalArgumentException("A citadel entrance must be horizontal");
        };
    }
}
