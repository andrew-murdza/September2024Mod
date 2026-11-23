package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The intact, eight-floor Nether citadel. Coordinates have relative X/Z and absolute Y.
 * Four double-height audience halls sit inside inhabited wings and corner bastions;
 * two enclosed switchback stairs connect every gallery without ladders or jumps.
 * The immutable block plan is shared by generation and the navigation/material audit.
 */
public final class NetherCitadelLayout {
    public static final int RADIUS = 112;
    public static final int MIN_Y = 4;
    public static final int MAX_Y = 125;
    public static final int GUARDS = 320;
    public static final int DEFENDERS = GUARDS + 1;
    private static final int WIDTH = RADIUS * 2 + 1;
    private static final int HEIGHT = MAX_Y - MIN_Y + 1;
    private static final int[] FLOORS = {14, 26, 38, 54, 70, 86, 102, 118};
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState TRIM = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState QUARTZ = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState LIGHT = Blocks.GLOWSTONE.defaultBlockState();
    private static final List<Room> ROOMS = rooms();
    private static final List<Resident> RESIDENTS = makeResidents();
    private static final List<Chest> CHESTS = makeChests();
    private static final List<BlockPos> ENTRANCES = makeEntrances();

    public record Resident(String id, BlockPos pos) { }
    public record Chest(BlockPos pos, Direction facing) { }
    private record Room(Direction outward, int center, int halfWidth, int index) {
        BlockPos at(int u, int y, int v) {
            return switch (outward) {
                case NORTH -> new BlockPos(center + u, y, -50 - v);
                case SOUTH -> new BlockPos(center + u, y, 50 + v);
                case WEST -> new BlockPos(-50 - v, y, center + u);
                case EAST -> new BlockPos(50 + v, y, center + u);
                default -> throw new IllegalStateException("A citadel wing must be horizontal");
            };
        }
    }

    private NetherCitadelLayout() { }

    public static List<Resident> residents() { return RESIDENTS; }
    public static List<Chest> chests() { return CHESTS; }
    public static List<BlockPos> entrances() { return ENTRANCES; }
    public static BlockPos entrance() { return new BlockPos(0, 105, RADIUS); }
    public static int[] floorHeights() { return FLOORS.clone(); }

    /** Null leaves natural terrain untouched; AIR is an intentionally excavated cell. */
    public static BlockState stateAt(int x, int y, int z) {
        return Math.abs(x) > RADIUS || Math.abs(z) > RADIUS || y < MIN_Y || y > MAX_Y
                ? null : Plan.STATES[index(x, y, z)];
    }

    public static boolean builtColumn(int x, int z) {
        int a = Math.abs(x), b = Math.abs(z);
        return a <= 96 && b <= 96 || a >= 82 && b >= 82 && a <= 104 && b <= 104;
    }

    private static int index(int x, int y, int z) {
        return ((x + RADIUS) * WIDTH + z + RADIUS) * HEIGHT + y - MIN_Y;
    }

    private static List<Room> rooms() {
        var result = new ArrayList<Room>();
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH))
            for (int center : new int[]{-72, -36, 0, 36, 72})
                result.add(new Room(direction, center, 18, result.size()));
        for (Direction direction : List.of(Direction.WEST, Direction.EAST))
            for (int center : new int[]{-32, 0, 32})
                result.add(new Room(direction, center, 16, result.size()));
        return List.copyOf(result);
    }

    private static List<Resident> makeResidents() {
        var result = new ArrayList<Resident>();
        for (int floorIndex = 0; floorIndex < FLOORS.length; floorIndex++) {
            int layer = floorIndex / 2;
            int ordinal = floorIndex % 2 * 40;
            int feet = FLOORS[floorIndex] + 1;
            for (Room room : ROOMS) for (int u : new int[]{-8, 8})
                result.add(new Resident(guard(layer, ordinal++), room.at(u, feet, 12)));
            for (int[] point : new int[][]{
                    {-40, -28}, {-40, 28}, {40, -28}, {40, 28},
                    {-16, -40}, {16, -40}, {-16, 40}, {16, 40}})
                result.add(new Resident(guard(layer, ordinal++), new BlockPos(point[0], feet, point[1])));
        }
        result.add(new Resident("minecraft:wither", new BlockPos(0, 15, 0)));
        if (result.size() != DEFENDERS) throw new IllegalStateException("Citadel roster changed");
        return List.copyOf(result);
    }

    private static String guard(int layer, int ordinal) {
        int selection = ordinal % 10;
        return switch (layer) {
            case 0 -> selection < 4 ? "minecraft:wither_skeleton"
                    : selection < 7 ? "minecraft:blaze" : "minecraft:magma_cube";
            // Hoglins are the documented Crimson Forest land population. Piglins are
            // deliberately not introduced into a biome whose authored roster lacks them.
            case 1 -> "minecraft:hoglin";
            case 2 -> selection < 5 ? "minecraft:zombified_piglin"
                    : selection < 8 ? "minecraft:zoglin" : "minecraft:creeper";
            default -> selection == 9 ? "minecraft:ghast" : "minecraft:skeleton";
        };
    }

    private static List<Chest> makeChests() {
        var result = new ArrayList<Chest>();
        for (int floor : FLOORS) for (Room room : ROOMS) if (room.index % 2 == 0)
            result.add(new Chest(room.at(-10, floor + 1, 43), room.outward.getOpposite()));
        return List.copyOf(result);
    }

    private static List<BlockPos> makeEntrances() {
        var result = new ArrayList<BlockPos>();
        for (int floorIndex = 0; floorIndex < FLOORS.length; floorIndex += 2)
            for (Direction direction : Direction.Plane.HORIZONTAL)
                result.add(new BlockPos(direction.getStepX() * RADIUS, FLOORS[floorIndex] + 3,
                        direction.getStepZ() * RADIUS));
        return List.copyOf(result);
    }

    private static final class Plan {
        private static final BlockState[] STATES = build();
    }

    private static BlockState[] build() {
        var plan = new Builder();
        // A continuous dry plinth seals the lower citadel against the basalt lava bed.
        for (int x = -104; x <= 104; x++) for (int z = -104; z <= 104; z++) {
            if (!builtColumn(x, z)) continue;
            boolean edge = !builtColumn(x - 1, z) || !builtColumn(x + 1, z)
                    || !builtColumn(x, z - 1) || !builtColumn(x, z + 1);
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                BlockState state = y < FLOORS[0] || y == MAX_Y ? STONE : AIR;
                if (edge) state = facade(x, y, z);
                plan.set(x, y, z, state);
            }
        }
        for (int floorIndex = 0; floorIndex < FLOORS.length; floorIndex++) {
            int floor = FLOORS[floorIndex];
            int ceiling = floorIndex + 1 < FLOORS.length ? FLOORS[floorIndex + 1] - 1 : MAX_Y;
            int layer = floorIndex / 2;
            for (int x = -103; x <= 103; x++) for (int z = -103; z <= 103; z++) {
                if (!builtColumn(x, z)) continue;
                boolean voidAtGallery = floorIndex % 2 == 1 && Math.max(Math.abs(x), Math.abs(z)) < 23;
                if (!voidAtGallery) {
                    plan.set(x, floor, z, floorTile(x, z, layer));
                    if (floorIndex > 0) plan.set(x, floor - 1, z, STONE);
                }
            }
            if (floorIndex % 2 == 1) galleryRail(plan, floor);
            for (Room room : ROOMS) furnishRoom(plan, room, floor, ceiling, layer, floorIndex);
            // Repeated piers, illuminated capitals and raised architraves distinguish the halls
            // from the lower-ceilinged private rooms without obstructing their circulation.
            for (int px : new int[]{-28, 28}) for (int pz : new int[]{-28, 28}) {
                for (int y = floor + 1; y <= ceiling; y++)
                    plan.box(px - 1, y, pz - 1, px + 1, y, pz + 1, pillar(Math.min(3, y / 32)));
                plan.box(px - 2, ceiling - 1, pz - 2, px + 2, ceiling, pz + 2, QUARTZ);
                plan.set(px, ceiling - 2, pz - 2, LIGHT);
                plan.set(px, ceiling - 2, pz + 2, LIGHT);
            }
            // A flush light rhythm marks the routes between the halls and each room entrance.
            for (int t = -84; t <= 84; t += 12) {
                plan.set(t, floor, 46, LIGHT);
                plan.set(t, floor, -46, LIGHT);
                plan.set(46, floor, t, LIGHT);
                plan.set(-46, floor, t, LIGHT);
            }
        }
        for (int i = 0; i < FLOORS.length; i += 2) chandelier(plan, i);
        stairs(plan, 1);
        stairs(plan, -1);
        for (int i = 0; i < FLOORS.length; i += 2)
            for (Direction side : Direction.Plane.HORIZONTAL) approach(plan, side, FLOORS[i]);
        for (Chest chest : CHESTS)
            plan.set(chest.pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, chest.facing));
        return plan.states;
    }

    private static BlockState facade(int x, int y, int z) {
        int layer = Math.min(3, Math.max(0, y / 32));
        int t = Math.abs(x) >= 96 ? z : x;
        if (y == MIN_Y || y == 13 || y == 31 || y == 63 || y == 95 || y == MAX_Y) return TRIM;
        if (Math.floorMod(t, 12) <= 1) return pillar(layer);
        int localY = y - FLOORS[layer * 2];
        if (Math.floorMod(t, 12) >= 5 && Math.floorMod(t, 12) <= 7 && localY >= 4 && localY <= 7)
            return LIGHT;
        if (localY == 3 || localY == 8) return QUARTZ;
        return STONE;
    }

    private static BlockState pillar(int layer) {
        return switch (layer) {
            case 1 -> Blocks.STRIPPED_CRIMSON_STEM.defaultBlockState();
            case 2 -> Blocks.STRIPPED_WARPED_STEM.defaultBlockState();
            case 3 -> Blocks.BONE_BLOCK.defaultBlockState();
            default -> Blocks.POLISHED_BASALT.defaultBlockState();
        };
    }

    private static BlockState floorTile(int x, int z, int layer) {
        int radius = Math.max(Math.abs(x), Math.abs(z));
        if (radius == 31 || radius == 43 || radius == 94 || Math.abs(x) <= 2 || Math.abs(z) <= 2)
            return QUARTZ;
        if (radius < 19 && (Math.abs(x + z) <= 1 || Math.abs(x - z) <= 1))
            return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        if (radius < 19) return Blocks.OBSIDIAN.defaultBlockState();
        if (radius >= 51 && radius <= 93) return switch (layer) {
            case 1 -> Blocks.CRIMSON_PLANKS.defaultBlockState();
            case 2 -> Blocks.WARPED_PLANKS.defaultBlockState();
            case 3 -> Blocks.QUARTZ_BRICKS.defaultBlockState();
            default -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        };
        return STONE;
    }

    private static void galleryRail(Builder plan, int floor) {
        for (int t = -23; t <= 23; t++) {
            plan.set(t, floor + 1, -23, rail(Direction.Axis.X));
            plan.set(t, floor + 1, 23, rail(Direction.Axis.X));
            plan.set(-23, floor + 1, t, rail(Direction.Axis.Z));
            plan.set(23, floor + 1, t, rail(Direction.Axis.Z));
        }
        for (int x : new int[]{-23, 23}) for (int z : new int[]{-23, 23}) plan.set(x, floor + 1, z, LIGHT);
    }

    private static BlockState rail(Direction.Axis direction) {
        var state = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        return direction == Direction.Axis.X
                ? state.setValue(WallBlock.EAST_WALL, WallSide.LOW).setValue(WallBlock.WEST_WALL, WallSide.LOW)
                : state.setValue(WallBlock.NORTH_WALL, WallSide.LOW).setValue(WallBlock.SOUTH_WALL, WallSide.LOW);
    }

    private static void furnishRoom(Builder plan, Room room, int floor, int ceiling, int layer, int floorIndex) {
        int doorwayHeight = Math.min(8, ceiling - floor - 1);
        for (int y = floor + 1; y <= ceiling; y++) {
            for (int u = -room.halfWidth; u <= room.halfWidth; u++) {
                boolean opening = Math.abs(u) <= 3 && y <= floor + doorwayHeight - 2
                        || Math.abs(u) <= 2 && y <= floor + doorwayHeight - 1
                        || u == 0 && y <= floor + doorwayHeight;
                BlockState face = y == floor + 1 || y == ceiling - 1 ? QUARTZ : STONE;
                plan.set(room.at(u, y, 0), opening ? AIR : face);
                plan.set(room.at(u, y, 44), face);
            }
            for (int v = 0; v <= 44; v++) {
                BlockState side = y == floor + 1 || y == ceiling - 1 ? QUARTZ : STONE;
                plan.set(room.at(-room.halfWidth, y, v), side);
                plan.set(room.at(room.halfWidth, y, v), side);
            }
        }
        for (int u : new int[]{-room.halfWidth + 1, room.halfWidth - 1}) {
            plan.set(room.at(u, floor + 4, 10), LIGHT);
            plan.set(room.at(u, floor + 4, 32), LIGHT);
        }
        int theme = (room.index + floorIndex) % 4;
        if (theme == 0) {
            Block forge = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("quark", "blackstone_furnace"));
            BlockState furnace = forge == null || forge == Blocks.AIR ? STONE : forge.defaultBlockState()
                    .trySetValue(FurnaceBlock.FACING, room.outward.getOpposite());
            for (int u : new int[]{-12, -10, -8, 8, 10, 12})
                plan.set(room.at(u, floor + 1, 40), furnace);
            plan.set(room.at(-3, floor + 1, 40), Blocks.CRAFTING_TABLE.defaultBlockState());
            plan.set(room.at(3, floor + 1, 40), Blocks.CRAFTING_TABLE.defaultBlockState());
            for (int u = -8; u <= 8; u++) plan.set(room.at(u, floor + 1, 31), QUARTZ);
            for (int u : new int[]{-8, 0, 8}) plan.set(room.at(u, floor + 2, 31), Blocks.BREWING_STAND.defaultBlockState());
        } else if (theme == 1) {
            garden(plan, room, floor, layer, floorIndex);
        } else if (theme == 2) {
            // Refectories and meeting rooms preserve the fortress's inhabited character.
            for (int u = -10; u <= 10; u++) plan.set(room.at(u, floor + 1, 32), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
            for (int u = -10; u <= 10; u += 5) {
                plan.set(room.at(u, floor + 1, 29), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, room.outward));
                plan.set(room.at(u, floor + 1, 35), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, room.outward.getOpposite()));
            }
            for (int u : new int[]{-12, 12}) plan.set(room.at(u, floor + 1, 40), Blocks.LOOM.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, room.outward.getOpposite()));
        } else {
            for (int u : new int[]{-10, 10}) {
                plan.set(room.at(u, floor + 1, 32), TRIM);
                plan.set(room.at(u, floor + 2, 32), Blocks.GOLD_BLOCK.defaultBlockState());
                plan.set(room.at(u, floor + 3, 32), Blocks.QUARTZ_SLAB.defaultBlockState());
            }
            for (int u = -6; u <= 6; u++) plan.set(room.at(u, floor + 1, 39), QUARTZ);
            plan.set(room.at(0, floor + 2, 39), Blocks.BREWING_STAND.defaultBlockState());
        }
    }

    private static void garden(Builder plan, Room room, int floor, int layer, int floorIndex) {
        for (int side : new int[]{-1, 1}) for (int u = 4; u <= 13; u++) for (int v = 25; v <= 38; v++) {
            BlockState ground = switch (layer) {
                case 1 -> Blocks.CRIMSON_NYLIUM.defaultBlockState();
                case 2 -> Blocks.WARPED_NYLIUM.defaultBlockState();
                case 3 -> Blocks.SOUL_SAND.defaultBlockState();
                default -> Blocks.BLACKSTONE.defaultBlockState();
            };
            plan.set(room.at(u * side, floor, v), ground);
            if (u == 4 || u == 13 || v == 25 || v == 38) {
                plan.set(room.at(u * side, floor + 1, v), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
                continue;
            }
            if (layer == 3) plan.set(room.at(u * side, floor + 1, v), Blocks.NETHER_WART.defaultBlockState()
                    .setValue(BlockStateProperties.AGE_3, 3));
            if (layer == 1 && (u + v) % 3 == 0) plan.set(room.at(u * side, floor + 1, v),
                    (v % 4 == 0 ? Blocks.CRIMSON_FUNGUS : Blocks.CRIMSON_ROOTS).defaultBlockState());
            if (layer == 2 && (u + v) % 3 == 0) plan.set(room.at(u * side, floor + 1, v),
                    (v % 4 == 0 ? Blocks.WARPED_FUNGUS : v % 2 == 0 ? Blocks.NETHER_SPROUTS : Blocks.WARPED_ROOTS).defaultBlockState());
        }
        if (layer == 0 && floorIndex == 0) {
            Block cane = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight:powdery_cane"));
            Block cap = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight:bullet_pepper"));
            if (cane != null && cane != Blocks.AIR && cap != null && cap != Blocks.AIR)
                for (int u : new int[]{-8, 8}) for (int v : new int[]{29, 34}) {
                    for (int h = 1; h <= 6; h++) plan.set(room.at(u, floor + h, v), cane.defaultBlockState()
                            .trySetValue(BlockStateProperties.AGE_3, 3)
                            .trySetValue(BlockStateProperties.BAMBOO_LEAVES,
                                    net.amurdza.examplemod.worldgen.PowderCaneShape.leaves(7 - h))
                            .trySetValue(BlockStateProperties.LIT, false));
                    plan.set(room.at(u, floor + 7, v), cap.defaultBlockState().trySetValue(BlockStateProperties.AGE_3, 3)
                            .trySetValue(BlockStateProperties.LIT, true));
                }
        }
        if (layer == 0 && floorIndex == 1) {
            // The upper basalt gallery is a mineral nursery; tall powder cane remains
            // downstairs so no stem or pepper cap crosses into the Crimson Forest.
            for (int u : new int[]{-8, 8}) for (int v : new int[]{29, 34})
                plan.set(room.at(u, floor + 1, v), Blocks.GILDED_BLACKSTONE.defaultBlockState());
        }
    }

    private static void chandelier(Builder plan, int floorIndex) {
        int ceiling = floorIndex + 2 < FLOORS.length ? FLOORS[floorIndex + 2] - 2 : MAX_Y - 1;
        int bottom = ceiling - 4;
        for (int y = bottom; y <= ceiling; y++) plan.set(0, y, 0, TRIM);
        for (int t = -6; t <= 6; t++) {
            plan.set(t, bottom, 0, QUARTZ); plan.set(0, bottom, t, QUARTZ);
            if (Math.abs(t) == 3 || Math.abs(t) == 6) {
                plan.set(t, bottom - 1, 0, LIGHT); plan.set(0, bottom - 1, t, LIGHT);
            }
        }
    }

    private static void stairs(Builder plan, int mirror) {
        // Flights alternate lanes; placing both directions in a single lane would
        // leave insufficient headroom at a reversal. Landings are four blocks deep.
        plan.boxXMirrored(mirror, 33, FLOORS[0], -12, 47, MAX_Y - 1, 12, AIR);
        for (int x : new int[]{32, 48}) plan.boxXMirrored(mirror, x, FLOORS[0], -12, x, MAX_Y, 12, STONE);
        for (int floor : FLOORS) {
            plan.boxXMirrored(mirror, 33, floor, -12, 47, floor, -9, QUARTZ);
            plan.boxXMirrored(mirror, 33, floor, 9, 47, floor, 12, QUARTZ);
            for (int z : new int[]{-11, 11}) plan.set(40 * mirror, floor, z, LIGHT);
        }
        for (int flight = 0; flight < FLOORS.length - 1; flight++) {
            int bottom = FLOORS[flight], top = FLOORS[flight + 1], rise = top - bottom;
            boolean forward = flight % 2 == 0;
            int minX = forward ? 34 : 42;
            Direction facing = forward ? Direction.SOUTH : Direction.NORTH;
            for (int step = 1; step <= rise; step++) {
                int z = forward ? -9 + step : 9 - step, y = bottom + step;
                plan.boxXMirrored(mirror, minX, y - 2, z, minX + 4, y - 1, z, STONE);
                plan.boxXMirrored(mirror, minX, y, z, minX + 4, y, z,
                        Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
                for (int x : new int[]{minX - 1, minX + 5}) {
                    plan.set(x * mirror, y, z, QUARTZ);
                    plan.set(x * mirror, y + 1, z, rail(Direction.Axis.Z));
                }
            }
            int end = forward ? rise - 9 : 9 - rise;
            plan.boxXMirrored(mirror, minX - 1, top, forward ? end + 1 : -12,
                    minX + 5, top, forward ? 12 : end - 1, QUARTZ);
        }
    }

    private static void approach(Builder plan, Direction side, int floor) {
        // The first 16 blocks outside the facade form a lit bridge. Two shallow
        // steps meet the upper bound of the real +/-2-block cavern-floor relief.
        for (int radius = 88; radius <= RADIUS; radius++) for (int u = -7; u <= 7; u++) {
            int surface = floor + (radius >= 108 ? 2 : radius >= 102 ? 1 : 0);
            int x = side.getAxis() == Direction.Axis.X ? side.getStepX() * radius : u;
            int z = side.getAxis() == Direction.Axis.Z ? side.getStepZ() * radius : u;
            for (int y = floor - 3; y <= surface; y++) plan.set(x, y, z, STONE);
            for (int y = surface + 1; y <= Math.min(surface + 8, MAX_Y - 1); y++) plan.set(x, y, z, AIR);
            if (Math.abs(u) == 7) {
                plan.set(x, surface + 1, z, QUARTZ);
                if (radius % 6 == 0) plan.set(x, surface + 2, z, LIGHT);
            }
            if (u == -3 || u == 3) plan.set(x, surface, z, QUARTZ);
            if ((radius == 102 || radius == 108) && Math.abs(u) < 7)
                plan.set(x, surface, z, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, side));
        }
        // The entry axis cuts through its room and reaches the central hall. Both
        // halves remain accessible even when the adjoining room is a greenhouse.
        for (int radius = 46; radius < 88; radius++) for (int u = -4; u <= 4; u++) {
            int x = side.getAxis() == Direction.Axis.X ? side.getStepX() * radius : u;
            int z = side.getAxis() == Direction.Axis.Z ? side.getStepZ() * radius : u;
            plan.set(x, floor, z, QUARTZ);
            for (int y = floor + 1; y <= floor + 6; y++) plan.set(x, y, z, AIR);
        }
    }

    private static final class Builder {
        private final BlockState[] states = new BlockState[WIDTH * WIDTH * HEIGHT];
        void set(int x, int y, int z, BlockState state) {
            if (Math.abs(x) <= RADIUS && Math.abs(z) <= RADIUS && y >= MIN_Y && y <= MAX_Y)
                states[index(x, y, z)] = state;
        }
        void set(BlockPos pos, BlockState state) { set(pos.getX(), pos.getY(), pos.getZ(), state); }
        void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = y1; y <= y2; y++) set(x, y, z, state);
        }
        void boxXMirrored(int mirror, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            box(mirror == 1 ? x1 : -x2, y1, z1, mirror == 1 ? x2 : -x1, y2, z2, state);
        }
    }
}
