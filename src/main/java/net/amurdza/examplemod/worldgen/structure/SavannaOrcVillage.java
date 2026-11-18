package net.amurdza.examplemod.worldgen.structure;

import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.IntBinaryOperator;

/** Version-eleven camp; saved older pieces retain their archived population and geometry. */
public final class SavannaOrcVillage {
    public static final int CENTER = 98, RADIUS = 96, SIZE = 197, MARGIN = 20;
    public static final int RESIDENT_COUNT = 127;
    private static final int[][] DISTANCES = distances();
    private static final int[][] SEATS = {{-3,-1},{-3,0},{-1,-3},{0,-3},{2,0},{2,-1},{-1,2},{0,2}};
    private static final float[] SEAT_YAWS = {0, 0, 270, 270, 0, 0, 90, 90};
    private static final String[] ANIMALS = {"alexsmobs:bison", "minecraft:sheep", "alexsmobs:gazelle", "biodiverse:oryx",
            "alexsmobs:emu", "alexsmobs:kangaroo", "twilightforest:boar", "biodiverse:wildebeest"};
    private static final List<Resident> RESIDENTS = createResidents();

    private SavannaOrcVillage() { }

    private static boolean lane(int coordinate) { return coordinate >= -3 && coordinate <= 2; }

    private static int mirror(int coordinate, int sign) { return sign > 0 ? coordinate : -coordinate - 1; }

    public static boolean contains(int x, int z) {
        long dx = Math.abs((long) x - CENTER), dz = Math.abs((long) z - CENTER);
        if (dx > RADIUS || dz > RADIUS) return false;
        return true;
    }

    public static BoundingBox bounds(BlockPos origin) {
        return new BoundingBox(origin.getX() - MARGIN, 0, origin.getZ() - MARGIN,
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

    /** One shared plan keeps the six lanes and their endpoint identical across chunk seams. */
    public static final class GroundPlan {
        private final int floor;
        private final int[][][] natural = new int[4][MARGIN + 1][6];
        private final int[] lengths = new int[4];
        private final int[][] terrain = new int[SIZE + 2 * MARGIN][SIZE + 2 * MARGIN];

        private GroundPlan(int floor, IntBinaryOperator height) {
            this.floor = floor;
            for (int[] row : terrain) java.util.Arrays.fill(row, Integer.MIN_VALUE);
            for (int direction = 0; direction < 4; direction++) {
                lengths[direction] = MARGIN;
                int previousTop = floor;
                for (int step = 1; step <= MARGIN; step++) {
                    int lowest = Integer.MAX_VALUE;
                    int highest = Integer.MIN_VALUE;
                    for (int across = -3; across <= 2; across++) {
                        int x = rampX(direction, step, across), z = rampZ(direction, step, across);
                        int surface = height.applyAsInt(x, z);
                        terrain[x + MARGIN][z + MARGIN] = surface;
                        natural[direction][step][across + 3] = surface;
                        lowest = Math.min(lowest, surface);
                        highest = Math.max(highest, surface);
                    }
                    int top = Math.max(floor - step, highest);
                    if (floor - step <= lowest || top >= previousTop) {
                        // Stop before either a complete terrain intersection
                        // or a level/rising tread on an uneven cross-slope.
                        // The grass mound still fills the lower lanes beyond it.
                        lengths[direction] = step - 1;
                        break;
                    }
                    previousTop = top;
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
        if (lane(dx) || lane(dz)) {
            int direction = lane(dx) ? (dz < 0 ? 0 : 1) : (dx < 0 ? 2 : 3);
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
        IntBinaryOperator naturalHeight = (x, z) -> {
            var source = level.getLevel().getChunkSource();
            return source.getGenerator().getBaseHeight(origin.getX() + x, origin.getZ() + z,
                    Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState()) - 1;
        };
        for (int x = Math.max(-MARGIN, chunk.minX() - origin.getX()); x < Math.min(SIZE + MARGIN, chunk.maxX() - origin.getX() + 1); x++) {
            for (int z = Math.max(-MARGIN, chunk.minZ() - origin.getZ()); z < Math.min(SIZE + MARGIN, chunk.maxZ() - origin.getZ() + 1); z++) {
                int distance = DISTANCES[x + MARGIN][z + MARGIN];
                int dx = x - CENTER, dz = z - CENTER;
                boolean path = lane(dx) || lane(dz);
                int direction = lane(dx) ? (dz < 0 ? 0 : 1) : (dx < 0 ? 2 : 3);
                int rampStep = Math.max(0, Math.max(Math.abs(dx), Math.abs(dz)) - (RADIUS + 1));
                if (path && rampStep > plan.length(direction)) path = false;
                if (distance > MARGIN + 1) continue;
                int wx = origin.getX() + x, wz = origin.getZ() + z;
                // Flat columns always use the authored floor and are unconditionally
                // mound columns; evaluating their original noise surface adds no information.
                int natural = distance <= 1 ? floor : plan.naturalHeight(x, z, naturalHeight);
                int top = path ? plan.top(direction, rampStep, direction < 2 ? dx : dz)
                        : distance <= 1 ? floor : Math.max(natural, floor - (distance - 1));
                if (distance > 1 && !path && top <= natural) continue;
                level.setBlock(new BlockPos(wx, top, wz), (path ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
                int clearTop = distance <= 1 ? origin.getY() + 17 : floor + 3;
                for (int y = top + 1; y <= clearTop; y++) level.setBlock(new BlockPos(wx, y, wz), Blocks.AIR.defaultBlockState(), 2);
                boolean wet = level.getFluidState(new BlockPos(wx, top, wz)).is(net.minecraft.tags.FluidTags.WATER);
                for (int depth = 1; depth <= 14; depth++) {
                    var material = wet || depth > 4 ? Blocks.MUD : depth <= 2 ? Blocks.ROOTED_DIRT : Blocks.DIRT;
                    level.setBlock(new BlockPos(wx, top - depth, wz), material.defaultBlockState(), 2);
                }
                if (net.amurdza.examplemod.worldgen.SavannaCampVegetation.isMudPond(x, z)) {
                    for (int y = floor - 3; y >= level.getMinBuildHeight(); y--) {
                        BlockPos pos = new BlockPos(wx, y, wz);
                        if (level.getBlockState(pos).is(Blocks.BEDROCK)) break;
                        level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Authored crops, ponds and tents are placed after this ground pass;
        // the native biome vegetation pass then decorates the remaining fields.
    }

    public static void populate(WorldGenLevel level, BoundingBox chunk, BlockPos origin,
                                BitSet residents, BitSet cushions, BitSet animals, UUID encounterId) {
        for (List<BlockPos> patch : mushroomPatches(origin)) {
            for (int i = 0; i < patch.size(); i++) {
                BlockPos pos = patch.get(i);
                if (chunk.isInside(pos)) level.setBlock(pos,
                        (i < 5 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState(), 2);
            }
        }
        for (Resident resident : RESIDENTS) {
            BlockPos pos = origin.offset(resident.local());
            if (!residents.get(resident.index()) && chunk.isInside(pos)
                    && spawnResident(level, resident, pos, origin, encounterId)) residents.set(resident.index());
        }
        int house = 0;
        for (int cx : new int[]{-27, -9, 9, 27}) for (int cz : new int[]{-27, -9, 9, 27}) {
            for (int i = 0; i < SEATS.length; i++) {
                int index = house * SEATS.length + i;
                BlockPos pos = origin.offset(CENTER + cx + SEATS[i][0], 3, CENTER + cz + SEATS[i][1]);
                if (cushions.get(index) || !chunk.isInside(pos)) continue;
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft:cushion"));
                if (type == null || !(type.create(level.getLevel()) instanceof Cushion cushion)) continue;
                var shape = level.getBlockState(pos.below()).getShape(level, pos.below());
                if (shape.isEmpty()) continue;
                cushion.setColor(DyeColor.RED);
                cushion.setYRot(SEAT_YAWS[i]);
                cushion.setPos(pos.getX() + .5, pos.getY() - 1 + shape.max(Direction.Axis.Y), pos.getZ() + .5);
                if (level.addFreshEntity(cushion)) cushions.set(index);
            }
            house++;
        }
        int herd = 0;
        for (int sz : new int[]{-1, 1}) for (int sx : new int[]{-1, 1}) for (boolean swap : new boolean[]{false, true}) {
            for (int i = 0; i < 8; i++) {
                // Eight positions leave adult-bison clearance around all four
                // hay stacks and the seven-by-seven central pond.
                int a = new int[]{68, 77, 64, 82, 68, 77, 68, 78}[i];
                int b = new int[]{8, 8, 17, 17, 26, 26, 12, 22}[i];
                BlockPos pos = origin.offset(CENTER + mirror(swap ? b : a, sx), 3, CENTER + mirror(swap ? a : b, sz));
                int index = herd * 8 + i;
                if (!animals.get(index) && chunk.isInside(pos) && BossLairPiece.spawn(level, ANIMALS[herd], pos,
                        mob -> SavannaCampProgress.markLivestock(mob, encounterId, origin))) animals.set(index);
            }
            herd++;
        }
    }

    public record Resident(int index, String type, BlockPos local, boolean forest) { }

    public static List<Resident> residentPlan() { return RESIDENTS; }

    private static List<Resident> createResidents() {
        List<Resident> result = new ArrayList<>();
        int house = 0;
        for (int cx : new int[]{-27, -9, 9, 27}) for (int cz : new int[]{-27, -9, 9, 27}) {
            for (int i = 0; i < (house == 5 ? 3 : 4); i++) {
                String type = house == 5 && i == 0 ? "orcz:bigorcboss"
                        : i % 2 == 0 ? "orcz:orcshield" : "orcz:orcsavagemace";
                result.add(new Resident(result.size(), type,
                        new BlockPos(CENTER + cx + (i % 2 == 0 ? -2 : 1), 3, CENTER + cz + (i < 2 ? -4 : 3)), false));
            }
            house++;
        }
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            int member = 0;
            for (int a : new int[]{36, 48, 60, 72}) for (int b : new int[]{60, 68, 76, 84}) {
                result.add(new Resident(result.size(), member++ % 2 == 0 ? "orcz:orcshield" : "orcz:orcsavagemace",
                        new BlockPos(CENTER + mirror(a, sx), 3, CENTER + mirror(b, sz)), true));
            }
        }
        return List.copyOf(result);
    }

    private static boolean spawnResident(WorldGenLevel level, Resident resident, BlockPos pos, BlockPos origin, UUID encounterId) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            var progress = AuthoredEncounterProgress.get(server);
            if (progress.find(encounterId) != null && !progress.canPlaceDefender(encounterId, resident.index())) return true;
            if (server.getEntity(residentId(encounterId, resident.index())) instanceof Mob existing) {
                progress.track(existing);
                return true;
            }
        }
        // Never seek a heightmap position: it can move a resident onto a roof,
        // tree, crop plot or pen. The authored cells are at the camp floor.
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !level.getFluidState(pos).isEmpty()) return false;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(resident.type()));
        if (type == null || !(type.create(level.getLevel()) instanceof Mob mob)) return false;
        mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        mob.setUUID(residentId(encounterId, resident.index()));
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        if (!net.amurdza.examplemod.worldgen.SpawnCollisionSafety.canInspect(level, mob.getBoundingBox())
                || !level.noCollision(mob, mob.getBoundingBox())) return false;
        net.amurdza.examplemod.util.StructureSpawnedMobData.mark(mob, pos);
        SavannaCampProgress.markResident(mob, encounterId, origin, resident.index());
        if (!level.addFreshEntity(mob)) return false;
        if (level instanceof net.minecraft.server.level.ServerLevel server) AuthoredEncounterProgress.get(server).track(mob);
        for (var passenger : mob.getPassengers()) level.addFreshEntityWithPassengers(passenger);
        return true;
    }

    public static UUID residentId(UUID encounterId, int index) {
        return UUID.nameUUIDFromBytes((encounterId + ":defender:" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Retry only at the authored floor inside the same hut or forest corner. */
    public static boolean retryResident(net.minecraft.server.level.ServerLevel level, BlockPos origin, UUID encounterId, int index) {
        if (index < 0 || index >= RESIDENTS.size()
                || !AuthoredEncounterProgress.get(level).canPlaceDefender(encounterId, index)) return false;
        var resident = RESIDENTS.get(index);
        BlockPos preferred = origin.offset(resident.local());
        for (int radius = 0; radius <= 4; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
            BlockPos candidate = preferred.offset(dx, 0, dz);
            if (!loadedForRetry(level, candidate)) continue;
            int x = candidate.getX() - origin.getX() - CENTER, z = candidate.getZ() - origin.getZ() - CENTER;
            if (resident.forest()) {
                if (Math.min(Math.abs(x), Math.abs(z)) <= 31 || Math.max(Math.abs(x), Math.abs(z)) < 59
                        || Math.max(Math.abs(x), Math.abs(z)) > 87) continue;
            } else {
                int cx = Math.floorDiv(resident.local().getX() - CENTER + 33, 18) * 18 - 27;
                int cz = Math.floorDiv(resident.local().getZ() - CENTER + 33, 18) * 18 - 27;
                if (Math.abs(x - cx) > 4 || Math.abs(z - cz) > 4) continue;
            }
            if (spawnResident(level, resident, candidate, origin, encounterId)) return true;
        }
        return false;
    }

    private static boolean loadedForRetry(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        // Include the boss width and neighbor-dependent collision-shape halo.
        for (int x = (pos.getX() - 4) >> 4; x <= (pos.getX() + 4) >> 4; x++)
            for (int z = (pos.getZ() - 4) >> 4; z <= (pos.getZ() + 4) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return true;
    }

    /** Each forest corner has two patches, each wholly inside its own world chunk. */
    public static List<List<BlockPos>> mushroomPatches(BlockPos origin) {
        List<List<BlockPos>> result = new ArrayList<>();
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            for (int start : new int[]{63, 79}) {
                var chunks = new TreeMap<Long, List<BlockPos>>();
                for (int a = start; a < start + 7; a++) for (int b = start; b < start + 7; b++) {
                    BlockPos pos = origin.offset(CENTER + mirror(a, sx), 3, CENTER + mirror(b, sz));
                    long key = net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
                    chunks.computeIfAbsent(key, ignored -> new ArrayList<>()).add(pos);
                }
                List<BlockPos> selected = List.of();
                for (List<BlockPos> candidates : chunks.values()) {
                    if (candidates.size() > selected.size()) selected = candidates;
                }
                // Even a corner-aligned seven-square zone has a 4x4 majority.
                if (selected.size() < 10) throw new IllegalStateException("Mushroom patch has no owner chunk");
                Collections.shuffle(selected, new Random(317L + sx * 31L + sz * 107L + start * 7919L));
                result.add(List.copyOf(selected.subList(0, 10)));
            }
        }
        return List.copyOf(result);
    }
}
