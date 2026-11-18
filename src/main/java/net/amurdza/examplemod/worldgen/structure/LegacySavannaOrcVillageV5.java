package net.amurdza.examplemod.worldgen.structure;

import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.BitSet;
import java.util.function.IntBinaryOperator;

/** Circular version-five village; older saved pieces retain their archived geometry. */
public final class LegacySavannaOrcVillageV5 {
    public static final int CENTER = 62, RADIUS = 60, SIZE = 125, MARGIN = 20;
    private static final int[][] DISTANCES = distances();
    private static final int[][] SEATS = {{-3,-4},{-3,4},{3,4},{0,-2},{-2,0},{0,2},{2,0},{3,-4}};
    private static final String[] ANIMALS = {"alexsmobs:bison", "minecraft:sheep", "alexsmobs:gazelle", "biodiverse:oryx",
            "alexsmobs:emu", "alexsmobs:kangaroo", "twilightforest:boar", "biodiverse:wildebeest"};

    private LegacySavannaOrcVillageV5() { }

    public static boolean contains(int x, int z) {
        long dx = Math.abs((long) x - CENTER), dz = Math.abs((long) z - CENTER);
        if (dx > RADIUS || dz > RADIUS) return false;
        return dx * dx + dz * dz <= (long) RADIUS * RADIUS;
    }

    public static BoundingBox bounds(BlockPos origin) {
        return new BoundingBox(origin.getX() - MARGIN, origin.getY() - 14, origin.getZ() - MARGIN,
                origin.getX() + SIZE - 1 + MARGIN, origin.getY() + 17, origin.getZ() + SIZE - 1 + MARGIN);
    }

    private static int[][] distances() {
        int side = SIZE + 2 * MARGIN;
        int[][] result = new int[side][side];
        for (int x = 0; x < side; x++) for (int z = 0; z < side; z++) {
            int d = contains(x - MARGIN, z - MARGIN) ? 0 : side * 2;
            if (x > 0) d = Math.min(d, result[x - 1][z] + 1);
            if (z > 0) d = Math.min(d, result[x][z - 1] + 1);
            if (x > 0 && z > 0) d = Math.min(d, result[x - 1][z - 1] + 1);
            if (x > 0 && z + 1 < side) d = Math.min(d, result[x - 1][z + 1] + 1);
            result[x][z] = d;
        }
        for (int x = side - 1; x >= 0; x--) for (int z = side - 1; z >= 0; z--) {
            if (x + 1 < side) result[x][z] = Math.min(result[x][z], result[x + 1][z] + 1);
            if (z + 1 < side) result[x][z] = Math.min(result[x][z], result[x][z + 1] + 1);
            if (x + 1 < side && z + 1 < side) result[x][z] = Math.min(result[x][z], result[x + 1][z + 1] + 1);
            if (x + 1 < side && z > 0) result[x][z] = Math.min(result[x][z], result[x + 1][z - 1] + 1);
        }
        return result;
    }

    public static boolean onFlatMound(int x, int z) {
        if (x < -MARGIN || x >= SIZE + MARGIN || z < -MARGIN || z >= SIZE + MARGIN) return false;
        return DISTANCES[x + MARGIN][z + MARGIN] <= 1;
    }

    /** One shared plan keeps the seven lanes and their endpoint identical across chunk seams. */
    public static final class GroundPlan {
        private final int floor;
        private final int[][][] natural = new int[4][MARGIN + 1][7];
        private final int[] lengths = new int[4];
        private final boolean[] meetsGround = new boolean[4];
        private final int[][] terrain = new int[SIZE + 2 * MARGIN][SIZE + 2 * MARGIN];

        private GroundPlan(int floor, IntBinaryOperator height) {
            this.floor = floor;
            for (int[] row : terrain) java.util.Arrays.fill(row, Integer.MIN_VALUE);
            for (int direction = 0; direction < 4; direction++) {
                lengths[direction] = MARGIN;
                for (int step = 1; step <= MARGIN; step++) {
                    int lowest = Integer.MAX_VALUE;
                    for (int across = -3; across <= 3; across++) {
                        int x = rampX(direction, step, across), z = rampZ(direction, step, across);
                        int surface = height.applyAsInt(x, z);
                        terrain[x + MARGIN][z + MARGIN] = surface;
                        natural[direction][step][across + 3] = surface;
                        lowest = Math.min(lowest, surface);
                    }
                    if (floor - step <= lowest) {
                        lengths[direction] = step;
                        meetsGround[direction] = true;
                        break;
                    }
                }
            }
        }

        public int length(int direction) { return lengths[direction]; }

        private int naturalHeight(int x, int z, IntBinaryOperator height) {
            int value = terrain[x + MARGIN][z + MARGIN];
            if (value == Integer.MIN_VALUE) terrain[x + MARGIN][z + MARGIN] = value = height.applyAsInt(x, z);
            return value;
        }

        public int top(int direction, int step, int across) {
            if (step == 0) return floor;
            if (step < 0 || step > lengths[direction]) throw new IllegalArgumentException("Outside the entrance ramp");
            // At the first complete ground intersection, all seven lanes land
            // on their own terrain column. No path is painted farther away.
            if (step == lengths[direction] && meetsGround[direction]) return natural[direction][step][across + 3];
            int top = floor - step;
            for (int surface : natural[direction][step]) top = Math.max(top, surface);
            return top;
        }
    }

    public static int rampX(int direction, int step, int across) {
        int distance = RADIUS + 1 + step;
        return CENTER + (direction < 2 ? across : direction == 2 ? -distance : distance);
    }

    public static int rampZ(int direction, int step, int across) {
        int distance = RADIUS + 1 + step;
        return CENTER + (direction >= 2 ? across : direction == 0 ? -distance : distance);
    }

    public static GroundPlan planGround(int floor, IntBinaryOperator height) {
        return new GroundPlan(floor, height);
    }

    /** Valid after the column's ground pass; no live level or region is retained by the plan. */
    public static boolean hasMoundColumn(int x, int z, GroundPlan plan) {
        if (plan == null || x < -MARGIN || x >= SIZE + MARGIN || z < -MARGIN || z >= SIZE + MARGIN) return false;
        int distance = DISTANCES[x + MARGIN][z + MARGIN];
        if (distance <= 1) return true;
        int dx = x - CENTER, dz = z - CENTER;
        if (Math.abs(dx) <= 3 || Math.abs(dz) <= 3) {
            int direction = Math.abs(dx) <= 3 ? (dz < 0 ? 0 : 1) : (dx < 0 ? 2 : 3);
            int step = Math.max(0, Math.max(Math.abs(dx), Math.abs(dz)) - (RADIUS + 1));
            if (step <= plan.length(direction)) return true;
        }
        int natural = plan.terrain[x + MARGIN][z + MARGIN];
        return natural != Integer.MIN_VALUE && distance <= MARGIN + 1 && plan.floor - (distance - 1) > natural;
    }

    public static GroundPlan planGround(WorldGenLevel level, BlockPos origin) {
        var generator = level.getLevel().getChunkSource().getGenerator();
        var randomState = level.getLevel().getChunkSource().randomState();
        return planGround(origin.getY() + 2, (x, z) -> generator.getBaseHeight(origin.getX() + x, origin.getZ() + z,
                Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1);
    }

    public static void placeGround(WorldGenLevel level, BoundingBox chunk, BlockPos origin) {
        placeGround(level, chunk, origin, planGround(level, origin));
    }

    public static void placeGround(WorldGenLevel level, BoundingBox chunk, BlockPos origin, GroundPlan plan) {
        int floor = origin.getY() + 2;
        var generator = level.getLevel().getChunkSource().getGenerator();
        var randomState = level.getLevel().getChunkSource().randomState();
        for (int x = Math.max(-MARGIN, chunk.minX() - origin.getX()); x < Math.min(SIZE + MARGIN, chunk.maxX() - origin.getX() + 1); x++) {
            for (int z = Math.max(-MARGIN, chunk.minZ() - origin.getZ()); z < Math.min(SIZE + MARGIN, chunk.maxZ() - origin.getZ() + 1); z++) {
                int distance = DISTANCES[x + MARGIN][z + MARGIN];
                int dx = x - CENTER, dz = z - CENTER;
                boolean path = Math.abs(dx) <= 3 || Math.abs(dz) <= 3;
                int direction = Math.abs(dx) <= 3 ? (dz < 0 ? 0 : 1) : (dx < 0 ? 2 : 3);
                int rampStep = Math.max(0, Math.max(Math.abs(dx), Math.abs(dz)) - (RADIUS + 1));
                if (path && rampStep > plan.length(direction)) path = false;
                if (distance > MARGIN + 1) continue;
                int wx = origin.getX() + x, wz = origin.getZ() + z;
                int natural = plan.naturalHeight(x, z, (lx, lz) -> generator.getBaseHeight(origin.getX() + lx,
                        origin.getZ() + lz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1);
                int top = path ? plan.top(direction, rampStep, direction < 2 ? dx : dz)
                        : distance <= 1 ? floor : Math.max(natural, floor - (distance - 1));
                if (distance > 1 && !path && top <= natural) continue;
                level.setBlock(new BlockPos(wx, top, wz), (path ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
                for (int y = top + 1; y <= floor + 3; y++) level.setBlock(new BlockPos(wx, y, wz), Blocks.AIR.defaultBlockState(), 2);
                boolean wet = level.getFluidState(new BlockPos(wx, top, wz)).is(net.minecraft.tags.FluidTags.WATER);
                for (int depth = 1; depth <= 14; depth++) {
                    var material = wet || depth > 4 ? Blocks.MUD : depth <= 2 ? Blocks.ROOTED_DIRT : Blocks.DIRT;
                    level.setBlock(new BlockPos(wx, top - depth, wz), material.defaultBlockState(), 2);
                }
            }
        }
        // Authored crops, ponds and tents are placed after this ground pass;
        // the native biome vegetation pass then decorates the remaining fields.
    }

    public static void populate(WorldGenLevel level, BoundingBox chunk, BlockPos origin,
                                BitSet residents, BitSet cushions, BitSet animals) {
        int house = 0;
        for (int cx : new int[]{-21, 0, 21}) for (int cz : new int[]{-21, 0, 21}) {
            for (int i = 0; i < 4; i++) {
                int index = house * 4 + i;
                BlockPos pos = origin.offset(CENTER + cx + (i % 2 == 0 ? -2 : 2), 3, CENTER + cz + (i < 2 ? -2 : 2));
                String id = house == 4 && i == 0 ? "orcz:bigorcboss" : i % 2 == 0 ? "orcz:orcshield" : "orcz:orcsavagemace";
                if (!residents.get(index) && chunk.isInside(pos) && BossLairPiece.spawn(level, id, pos)) residents.set(index);
            }
            for (int i = 0; i < SEATS.length; i++) {
                int index = house * SEATS.length + i;
                BlockPos pos = origin.offset(CENTER + cx + SEATS[i][0], 3, CENTER + cz + SEATS[i][1]);
                if (cushions.get(index) || !chunk.isInside(pos)) continue;
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft:cushion"));
                if (type == null || !(type.create(level.getLevel()) instanceof Cushion cushion)) continue;
                var shape = level.getBlockState(pos.below()).getShape(level, pos.below());
                if (shape.isEmpty()) continue;
                cushion.setColor(DyeColor.RED);
                cushion.setPos(pos.getX() + .5, pos.getY() - 1 + shape.max(Direction.Axis.Y), pos.getZ() + .5);
                if (level.addFreshEntity(cushion)) cushions.set(index);
            }
            house++;
        }
        int herd = 0;
        for (int sz : new int[]{-1, 1}) for (int sx : new int[]{-1, 1}) for (boolean swap : new boolean[]{false, true}) {
            for (int i = 0; i < 4; i++) {
                int a = i % 2 == 0 ? 46 : 50;
                int b = i < 2 ? 12 : 15;
                BlockPos pos = origin.offset(CENTER + sx * (swap ? b : a), 3, CENTER + sz * (swap ? a : b));
                int index = herd * 4 + i;
                if (!animals.get(index) && chunk.isInside(pos) && BossLairPiece.spawn(level, ANIMALS[herd], pos)) animals.set(index);
            }
            herd++;
        }
    }
}
