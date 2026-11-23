package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** An intact mountain settlement: an agricultural forecourt, five inhabited levels and an ice sanctuary. */
public final class GoblinFortressLayout {
    public static final int MIN_X = -80, MAX_X = 80, MIN_Z = -92, MAX_Z = 92;
    public static final int MIN_Y = -51, MAX_Y = 48;
    public static final int[] FLOORS = {0, -12, -24, -36, -48};
    public record Room(String name, int minX, int minZ, int maxX, int maxZ, int floor) { }
    private static final String[][] THEMES = {
            {"guard_hall", "dining_hall", "forge", "enchanting", "yeti_gallery", "ice_store"},
            {"barracks", "dormitory", "armory", "archives", "weaving", "kitchen"},
            {"mushroom_nursery", "mushroom_garden", "mushroom_hall", "mooshrooms", "brewing", "cold_store"},
            {"stonecutters", "miners_hall", "iron_mine", "copper_mine", "ore_sorting", "toolmakers"},
            {"deep_forge", "ore_vault", "crystal_gallery", "survey_hall", "deep_mine", "miners_refectory"}
    };
    private static final List<Room> ROOMS = makeRooms();
    private static final List<Room> PENS = makePens();
    private static final List<BlockPos> LOOT = makeLoot();

    private GoblinFortressLayout() { }

    public static List<Room> rooms() { return ROOMS; }
    public static List<Room> pens() { return PENS; }
    public static List<BlockPos> lootPositions() { return LOOT; }
    public static BlockPos entrance() { return new BlockPos(0, 1, -92); }
    public static BlockPos boss() { return new BlockPos(0, 1, 76); }

    private static List<Room> makeRooms() {
        var result = new ArrayList<Room>();
        int[] starts = {-12, 15, 42}, ends = {10, 37, 63};
        for (int level = 0; level < FLOORS.length; level++) for (int row = 0; row < 3; row++) {
            result.add(new Room(THEMES[level][row * 2], -72, starts[row], -11, ends[row], FLOORS[level]));
            result.add(new Room(THEMES[level][row * 2 + 1], 11, starts[row], 72, ends[row], FLOORS[level]));
        }
        return List.copyOf(result);
    }

    private static List<Room> makePens() {
        return List.of(new Room("dappled_yaks", 43, -49, 71, -39, 0),
                new Room("dappled_foxes", 43, -37, 71, -28, 0),
                new Room("dappled_rabbits", 43, -26, 71, -16, 0),
                new Room("snowy_goats", -71, 12, -43, 22, 12),
                new Room("snowy_reindeer", -40, 12, -12, 22, 12),
                new Room("snowy_polar_bears", 12, 12, 40, 22, 12),
                new Room("snowy_foxes", 43, 12, 71, 22, 12),
                new Room("snowy_rabbits", -71, 25, -12, 35, 12),
                new Room("mooshrooms", 14, 17, 69, 35, -24));
    }

    private static List<BlockPos> makeLoot() {
        var result = new ArrayList<BlockPos>();
        for (Room room : ROOMS) {
            if (room.name.equals("mooshrooms")) continue;
            result.add(new BlockPos(room.minX < 0 ? room.minX : room.maxX,
                    room.floor + 1, room.minZ + 3));
        }
        result.add(new BlockPos(-13, 1, 79));
        result.add(new BlockPos(13, 1, 79));
        return List.copyOf(result);
    }

    /** Positions are reserved away from furniture, walls, stairs, gates and each other. */
    public static List<BlockPos> spawnPositions(Room room) {
        var result = new ArrayList<BlockPos>();
        for (int x = room.minX + 4; x <= room.maxX - 3; x += 6) {
            for (int z = room.minZ + 4; z <= room.maxZ - 3; z += 6) {
                int y = room.floor + 1;
                var floor = stateAt(x, y - 1, z);
                if (floor == null || floor.isAir() || !floor.getFluidState().isEmpty()) continue;
                if (clear(x, y, z) && clear(x, y + 1, z) && clear(x, y + 2, z)) result.add(new BlockPos(x, y, z));
            }
        }
        return List.copyOf(result);
    }

    private static boolean clear(int x, int y, int z) {
        var state = stateAt(x, y, z);
        return state != null && state.isAir();
    }

    public static List<BlockPos> defenders() {
        var result = new ArrayList<BlockPos>();
        for (Room room : ROOMS) if (!room.name.equals("mooshrooms")) result.addAll(spawnPositions(room));
        return List.copyOf(result);
    }

    /** Conservative traversable floor count; excludes walls, furniture, water and vertical stair shafts. */
    public static int floorArea() {
        int result = 0;
        for (int floor : FLOORS) for (int x = -73; x <= 73; x++) for (int z = -13; z <= 64; z++) {
            var ground = stateAt(x, floor, z);
            if (ground != null && !ground.isAir() && ground.getFluidState().isEmpty()
                    && clear(x, floor + 1, z) && clear(x, floor + 2, z)) result++;
        }
        for (int x = -73; x <= 73; x++) for (int z = -50; z <= -15; z++)
            if (clear(x, 1, z) && clear(x, 2, z)) result++;
        return result;
    }

    public static BlockPos transform(BlockPos origin, Direction inward, BlockPos local) {
        int forwardX = inward.getStepX(), forwardZ = inward.getStepZ();
        return origin.offset(local.getX() * forwardZ + local.getZ() * forwardX, local.getY(),
                -local.getX() * forwardX + local.getZ() * forwardZ);
    }

    public static BoundingBox bounds(BlockPos origin, Direction inward) {
        var first = transform(origin, inward, new BlockPos(MIN_X, MIN_Y, MIN_Z));
        var second = transform(origin, inward, new BlockPos(MAX_X, MAX_Y, MAX_Z));
        return new BoundingBox(Math.min(first.getX(), second.getX()), first.getY(), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()), second.getY(), Math.max(first.getZ(), second.getZ()));
    }

    private static Rotation rotation(Direction inward) {
        return switch (inward) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Only this chunk's columns are written; every choice depends on local coordinates, never chunk order. */
    public static void placeColumn(WorldGenLevel level, BoundingBox clip, BlockPos origin, Direction inward, int x, int z) {
        int low = z >= -14 && z <= 65 && Math.abs(x) <= 74 ? MIN_Y : -6;
        int high = z >= -53 && z <= 65 && Math.abs(x) <= 77 || towerColumn(x, z) ? MAX_Y : 17;
        var rotation = rotation(inward);
        for (int y = low; y <= high; y++) {
            var state = stateAt(x, y, z);
            if (state == null) continue;
            var pos = transform(origin, inward, new BlockPos(x, y, z));
            if (clip.isInside(pos) && pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight())
                GoblinFortressBlocks.set(level, pos, state.rotate(rotation));
        }
        // Exposed terraces and fishing decks have retaining foundations, including over a sloping shore.
        // Stop at the first natural solid block; never scan or write a neighboring chunk.
        if (low == -6 && stateAt(x, -6, z) != null) {
            for (int y = origin.getY() - 7; y >= Math.max(origin.getY() + MIN_Y, level.getMinBuildHeight()); y--) {
                var pos = transform(origin, inward, new BlockPos(x, y - origin.getY(), z));
                if (!clip.isInside(pos)) continue;
                var existing = level.getBlockState(pos);
                if (existing.getFluidState().isEmpty() && existing.isCollisionShapeFullBlock(level, pos)) break;
                level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
        }
    }

    /** Null leaves the natural mountain intact; only authored rooms and their shell are excavated. */
    public static BlockState stateAt(int x, int y, int z) {
        if (x < MIN_X || x > MAX_X || z < MIN_Z || z > MAX_Z || y < MIN_Y || y > MAX_Y) return null;
        if (z >= 60 && Math.abs(x) <= 17) {
            var ice = iceSanctuary(x, y, z);
            if (ice != null) return ice;
        }
        if (Math.abs(x) <= 74 && z >= -14 && z <= 65) return interior(x, y, z);
        if (towerColumn(x, z)) return forecourt(x, y, z);
        if (z >= -53 && z <= -15 && Math.abs(x) <= 77) return forecourt(x, y, z);
        if (z >= -92 && z <= -54 && Math.abs(x) <= 40) return fishery(x, y, z);
        return null;
    }

    private static BlockState interior(int x, int y, int z) {
        if (y > 10) return roofTerrace(x, y, z);
        if (y < -48) return brick(y == -49 ? Blocks.DEEPSLATE_TILES : Blocks.STONE);
        int floor = Math.floorDiv(y, 12) * 12;
        if (floor < -48) floor = -48;
        int height = y - floor;
        // Four flights and the surface-to-snow-terrace flight share aligned landings.
        if (Math.abs(x) >= 3 && Math.abs(x) <= 7 && z >= -9 && z <= 2) {
            int stairY = z + 10;
            int phase = Math.floorMod(y - stairY, 12);
            if (phase == 0) return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
            if (phase == 11) return brick(Blocks.STONE_BRICKS);
            return brick(Blocks.CAVE_AIR);
        }
        boolean surfaceIce = floor == 0 && z >= 39;
        if (height == 0) {
            if (surfaceIce) return brick(Blocks.SNOW_BLOCK);
            if (floor == -24 && roomIndex(x, z) >= 0 && roomIndex(x, z) < 4) return brick(Blocks.MYCELIUM);
            return brick(Math.abs(x) <= 9 ? (Math.floorMod(z, 8) < 2 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS)
                    : floor <= -36 ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_STONE);
        }
        if (height >= 10) return brick(surfaceIce ? Blocks.BLUE_ICE : Blocks.STONE_BRICKS);
        if (Math.abs(x) >= 73 || z == -14 || z == 65) {
            if (z == -14 && floor == 0 && Math.abs(x) <= 6 && height <= 8) return brick(Blocks.CAVE_AIR);
            if (z == 65 && floor == 0 && Math.abs(x) <= 5 && height <= 8) return brick(Blocks.CAVE_AIR);
            if (z == -14 && floor == 0 && Math.abs(x) >= 18 && Math.floorMod(Math.abs(x), 12) <= 3 && height >= 3 && height <= 6)
                return grille(true);
            return surfaceIce ? brick(Blocks.BLUE_ICE) : masonry(x, y, z);
        }
        if (Math.abs(x) == 10) {
            boolean doorway = near(z, -1, 2) || near(z, 26, 2) || near(z, 52, 2) || z == 12 || z == 13 || z == 39 || z == 40;
            if (doorway && height <= (surfaceIce ? 7 : 5)) return brick(Blocks.CAVE_AIR);
            if (surfaceIce) return brick(Blocks.BLUE_ICE);
            return height == 8 && Math.floorMod(z, 7) == 0 ? brick(Blocks.SEA_LANTERN) : masonry(x, y, z);
        }
        if (Math.abs(x) > 10 && (z == 11 || z == 14 || z == 38 || z == 41)) {
            if (near(Math.abs(x), 41, 3) && height <= 5) return brick(Blocks.CAVE_AIR);
            return surfaceIce ? brick(Blocks.BLUE_ICE) : masonry(x, y, z);
        }
        if (Math.abs(x) == 2 && z >= -9 && z <= 1 && height == 1) return brick(Blocks.STONE_BRICK_WALL);
        if (Math.abs(x) <= 9) {
            if (height == 9 && x == 0 && Math.floorMod(z, 10) == 5) return Blocks.LANTERN.defaultBlockState()
                    .setValue(BlockStateProperties.HANGING, true);
            if (surfaceIce && height == 1 && Math.abs(x) >= 8) return brick(Blocks.BLUE_ICE);
            return brick(Blocks.CAVE_AIR);
        }
        int roomId = roomIndex(x, z);
        if (roomId < 0) return brick(Blocks.CAVE_AIR);
        Room room = ROOMS.get((-floor / 12) * 6 + roomId);
        if (isLoot(x, y, z)) return chest(x < 0 ? Direction.EAST : Direction.WEST);
        var furnishing = furnish(room, x, height, z);
        return furnishing == null ? brick(Blocks.CAVE_AIR) : furnishing;
    }

    private static int roomIndex(int x, int z) {
        if (Math.abs(x) < 11 || Math.abs(x) > 72) return -1;
        int row = z >= -12 && z <= 10 ? 0 : z >= 15 && z <= 37 ? 1 : z >= 42 && z <= 63 ? 2 : -1;
        return row < 0 ? -1 : row * 2 + (x > 0 ? 1 : 0);
    }

    private static BlockState furnish(Room room, int x, int h, int z) {
        int u = x - room.minX, v = z - room.minZ;
        int width = room.maxX - room.minX, depth = room.maxZ - room.minZ;
        boolean back = u == 1 || u == width - 1;
        // Repeating buttresses and ceiling lights give every hall an intentional stone rhythm.
        if ((u == 1 || u == width - 1) && (v == 1 || v == depth - 1) && h <= 8)
            return brick(room.name.contains("yeti") || room.name.equals("ice_store") ? Blocks.BLUE_ICE : Blocks.POLISHED_ANDESITE);
        if (h == 9 && (u == 10 || u == width - 10) && v == depth / 2) return brick(Blocks.SEA_LANTERN);
        // Four suspended edge lights complement the six workbench lanterns. Keep chains outside mushroom caps.
        if ((u == 6 || u == width - 6) && (v == 6 || v == depth - 6)) {
            if (h == 4) return Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
            if (h >= 5 && h <= 9) return brick(Blocks.CHAIN);
        }
        if ((u == 15 || u == 31 || u == 47) && (v == 6 || v == depth - 6)) {
            if (h == 1) return brick(Blocks.STONE_BRICK_WALL);
            if (h == 2) return brick(Blocks.LANTERN);
        }
        if (h == 1 && v == depth - 2 && u >= 8 && u <= width - 8 && u % 8 == 0) return brick(Blocks.CRAFTING_TABLE);
        String theme = room.name;
        if (theme.contains("mushroom") || theme.equals("mooshrooms")) {
            if (theme.equals("mooshrooms")) {
                if ((x == 13 || x == 70 || z == 16 || z == 36) && h == 1)
                    return z == 16 && x == 41 ? gate(Direction.SOUTH) : perimeterFence(x, z, 13, 70, 16, 36);
            }
            int[] centers = {room.minX + 10, room.maxX - 10};
            for (int centerX : centers) {
                int centerZ = room.minZ + 8;
                int distance = Math.max(Math.abs(x - centerX), Math.abs(z - centerZ));
                if (distance == 0 && h <= 5) return brick(Blocks.MUSHROOM_STEM);
                if (distance <= 3 && h == 6) return brick(centerX == centers[0] ? Blocks.RED_MUSHROOM_BLOCK : Blocks.BROWN_MUSHROOM_BLOCK);
                if (distance == 3 && h == 5 && centerX == centers[0]) return brick(Blocks.RED_MUSHROOM_BLOCK);
            }
            if (h == 1 && v >= 2 && v <= depth - 3 && (u == 4 || u == width - 4) && v % 3 == 0)
                return brick(v % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM);
        } else if (theme.contains("forge") || theme.equals("toolmakers") || theme.equals("stonecutters")) {
            if (h == 1 && back && v >= 4 && v <= depth - 4 && v % 3 == 0) return brick(Blocks.BLAST_FURNACE);
            if (h == 1 && (u == 10 || u == width - 10) && v == depth / 2) return brick(Blocks.ANVIL);
            if (h == 1 && u == width / 2 && v == depth - 4) return brick(Blocks.SMITHING_TABLE);
            if (h == 1 && (u == 16 || u == width - 16) && v == depth / 2) return brick(Blocks.GRINDSTONE);
            if (h == 1 && u == width / 2 && v == 3) return brick(Blocks.CAULDRON);
        } else if (theme.contains("enchant") || theme.equals("archives") || theme.equals("survey_hall")) {
            if (h <= 3 && back && v >= 4 && v <= depth - 4) return brick(Blocks.BOOKSHELF);
            if (h == 1 && u == width / 2 && v == depth / 2) return brick(Blocks.ENCHANTING_TABLE);
            if (h == 1 && (u == 10 || u == width - 10) && v == depth / 2) return brick(Blocks.LECTERN);
        } else if (theme.endsWith("_mine") || theme.equals("miners_hall")
                || theme.startsWith("ore_") || theme.equals("crystal_gallery")) {
            if (back && v >= 3 && v <= depth - 3 && h <= 3) {
                Block ore = theme.contains("copper") ? Blocks.COPPER_ORE : theme.contains("crystal") ? Blocks.AMETHYST_BLOCK
                        : room.floor <= -36 ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE;
                return brick(Math.floorMod(v + h, 4) == 0 ? ore : Blocks.STONE);
            }
            // Both minecart lanes run beside the lantern pedestals, never through them.
            if (h == 1 && (u == 13 || u == width - 13) && v >= 4 && v <= depth - 4) return brick(Blocks.RAIL);
            if (h == 1 && v == depth - 3 && u % 10 == 2) return brick(Blocks.BARREL);
        } else if (theme.equals("barracks") || theme.equals("dormitory")) {
            // Beds have both halves, face the central aisle and never obstruct a doorway.
            if (h == 1 && (u == 3 || u == 4 || u == width - 3 || u == width - 4) && v >= 4 && v <= depth - 4 && v % 4 == 0)
                return Blocks.RED_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, u < width / 2 ? Direction.WEST : Direction.EAST)
                        .setValue(BlockStateProperties.BED_PART, u == 3 || u == width - 3
                                ? net.minecraft.world.level.block.state.properties.BedPart.HEAD : net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        } else if (theme.contains("dining") || theme.contains("refectory") || theme.equals("kitchen")) {
            if (h == 1 && v == depth / 2 && u >= 12 && u <= width - 12) return brick(Blocks.SMOOTH_STONE_SLAB);
            if (h == 1 && (v == depth / 2 - 2 || v == depth / 2 + 2) && u >= 12 && u <= width - 12 && u % 3 == 0)
                return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, v < depth / 2 ? Direction.SOUTH : Direction.NORTH);
            if (h == 1 && back && v >= 4 && v <= depth - 4 && v % 3 == 0) return brick(v % 2 == 0 ? Blocks.SMOKER : Blocks.BARREL);
        } else if (theme.equals("brewing")) {
            if (h == 1 && back && v >= 4 && v <= depth - 4) return brick(Blocks.STONE_BRICKS);
            if (h == 2 && back && v >= 4 && v <= depth - 4 && v % 4 == 0) return brick(Blocks.BREWING_STAND);
            if (h == 1 && u == width / 2 && v == depth / 2) return brick(Blocks.CAULDRON);
        } else if (theme.equals("weaving")) {
            if (h == 1 && back && v >= 4 && v <= depth - 4 && v % 3 == 0) return brick(Blocks.LOOM);
            if (h <= 2 && (u == 8 || u == width - 8) && v == depth - 4) return brick(Blocks.WHITE_WOOL);
        } else if (theme.contains("yeti") || theme.contains("ice")) {
            if (h == 1 && back && v >= 4 && v <= depth - 4) return brick(Blocks.BLUE_ICE);
        } else if (h == 1 && back && v >= 4 && v <= depth - 4 && v % 4 == 0) return brick(Blocks.BARREL);
        return null;
    }

    private static BlockState roofTerrace(int x, int y, int z) {
        if (z > 36) return null;
        if (Math.abs(x) >= 3 && Math.abs(x) <= 7 && z >= -9 && z <= 2) {
            int stairY = z + 10;
            if (y == stairY) return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
            if (y < stairY) return brick(Blocks.STONE_BRICKS);
            return brick(Blocks.AIR);
        }
        if (y == 11) return brick(Blocks.STONE_BRICKS);
        if (y == 12) return brick(z >= 10 && Math.abs(x) >= 11 ? Blocks.SNOW_BLOCK : Blocks.STONE_BRICKS);
        if (y > 24) return brick(Blocks.AIR);
        if (Math.abs(x) == 74 || z == -14 || z == 36) {
            if (y <= 15 || y == 16 && (Math.floorMod(x, 3) == 0 || Math.floorMod(z, 3) == 0)) return masonry(x, y, z);
        }
        for (Room pen : PENS) if (pen.floor == 12 && x >= pen.minX && x <= pen.maxX && z >= pen.minZ && z <= pen.maxZ) {
            if (y == 13 && (x == pen.minX || x == pen.maxX || z == pen.minZ || z == pen.maxZ))
                return z == pen.minZ && x == (pen.minX + pen.maxX) / 2 ? gate(Direction.SOUTH)
                        : perimeterFence(x, z, pen.minX, pen.maxX, pen.minZ, pen.maxZ);
            if (y == 13 && x == pen.maxX - 2 && z == pen.maxZ - 2) return brick(Blocks.HAY_BLOCK);
        }
        if (y == 13 && Math.abs(x) == 8 && z % 10 == 5) return brick(Blocks.LANTERN);
        return brick(Blocks.AIR);
    }

    private static BlockState forecourt(int x, int y, int z) {
        if (y < 0) return brick(y == -1 ? Blocks.DIRT : Blocks.STONE);
        int towerX = Math.abs(x) - 68, towerZ = z + 59;
        if (y == 0) {
            if (Math.max(Math.abs(towerX), Math.abs(towerZ)) <= 8) return brick(Blocks.STONE_BRICKS);
            if (x >= -70 && x <= -12 && z >= -48 && z <= -17) return farm(x, y, z);
            return brick(Math.abs(x) <= 7 || z == -15 || z == -51 ? Blocks.STONE_BRICKS : Blocks.GRASS_BLOCK);
        }
        if (Math.max(Math.abs(towerX), Math.abs(towerZ)) <= 8) {
            boolean wall = Math.abs(towerX) >= 7 || Math.abs(towerZ) >= 7;
            if (towerX == 6 && towerZ == 0 && y <= 19) return Blocks.LADDER.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, x > 0 ? Direction.WEST : Direction.EAST);
            if (y == 10 || y == 19) return brick(Blocks.STONE_BRICKS);
            if (y <= 18 && wall) {
                boolean door = towerZ >= 7 && Math.abs(towerX) <= 1 && y <= 4;
                boolean window = (y >= 4 && y <= 6 || y >= 13 && y <= 15)
                        && (Math.abs(towerX) == 7 && Math.abs(towerZ) <= 1 || Math.abs(towerZ) == 7 && Math.abs(towerX) <= 1);
                return door ? brick(Blocks.AIR) : window ? grille(Math.abs(towerZ) == 7) : masonry(x, y, z);
            }
            if (y == 20 && wall && Math.floorMod(x + z, 3) == 0) return brick(Blocks.STONE_BRICKS);
            return y <= 24 ? brick(Blocks.AIR) : null;
        }
        if (z <= -51 || Math.abs(x) >= 74) {
            if (z <= -51 && Math.abs(x) <= 5 && y <= 7) return brick(Blocks.AIR);
            if (y <= 9 || y == 10 && Math.floorMod(x + z, 3) == 0) return masonry(x, y, z);
            return y <= 24 ? brick(Blocks.AIR) : null;
        }
        if (x >= -70 && x <= -12 && z >= -48 && z <= -17) return farm(x, y, z);
        for (Room pen : PENS) if (pen.floor == 0 && x >= pen.minX && x <= pen.maxX && z >= pen.minZ && z <= pen.maxZ) {
            if (y == 1 && (x == pen.minX || x == pen.maxX || z == pen.minZ || z == pen.maxZ))
                return x == pen.minX && z == (pen.minZ + pen.maxZ) / 2 ? gate(Direction.EAST)
                        : perimeterFence(x, z, pen.minX, pen.maxX, pen.minZ, pen.maxZ);
            if (y == 1 && x == pen.maxX - 2 && z == pen.maxZ - 2) return brick(Blocks.HAY_BLOCK);
        }
        if (x >= 12 && x <= 39 && z >= -48 && z <= -17) {
            for (int treeX : new int[]{17, 32}) for (int treeZ : new int[]{-42, -24}) {
                int distance = Math.max(Math.abs(x - treeX), Math.abs(z - treeZ));
                if (distance == 0 && y <= 10) return registry("minecraft:poplar_log", Blocks.OAK_LOG);
                if (y >= 5 && y <= 12 && distance <= (y >= 10 ? 1 : 2))
                    return registry(treeX == 17 ? "minecraft:orange_poplar_leaves" : "minecraft:yellow_poplar_leaves", Blocks.OAK_LEAVES)
                            .setValue(BlockStateProperties.PERSISTENT, true);
            }
            if (x >= 14 && x <= 37 && z == -33 && y <= 2) return registry("minecraft:poplar_log", Blocks.OAK_LOG)
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
            if (x == 39 && z == -32 && y == 1) return brick(Blocks.STONECUTTER);
        }
        if (y == 1 && Math.abs(x) == 8 && z % 10 == -5) return brick(Blocks.LANTERN);
        return y <= 24 ? brick(Blocks.AIR) : null;
    }

    private static BlockState farm(int x, int y, int z) {
        if (y > 1) return y <= 24 ? brick(Blocks.AIR) : null;
        int row = z + 48;
        boolean edge = x == -70 || x == -12 || z == -48 || z == -17;
        boolean path = row == 11 || row == 22;
        boolean irrigation = Math.floorMod(x + 69, 9) == 4;
        if (y == 0) {
            if (edge || path) return brick(Blocks.STONE_BRICKS);
            // Covered source water irrigates both neighboring rows and cannot turn into ice.
            if (irrigation) return Blocks.SPRUCE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
            return Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7);
        }
        if (edge) return x == -12 && (row == 11 || row == 22) ? gate(Direction.EAST)
                : perimeterFence(x, z, -70, -12, -48, -17);
        if (path || irrigation) return brick(Blocks.AIR);
        if (row < 11) return Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7);
        if (row < 22) return Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3);
        return Math.floorMod(x, 3) == 0 ? brick(Blocks.PUMPKIN) : Math.floorMod(x, 3) == 1
                ? Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                : brick(Blocks.AIR);
    }

    private static BlockState fishery(int x, int y, int z) {
        if (y < -5) return brick(Blocks.STONE);
        if (Math.abs(x) <= 6) return y == 0 ? brick(Blocks.STONE_BRICKS) : y >= -5 && y < 0 ? brick(Blocks.STONE) : y <= 17 ? brick(Blocks.AIR) : null;
        boolean pool = Math.abs(x) >= 11 && Math.abs(x) <= 35 && z >= -87 && z <= -62;
        if (pool) {
            if (y == -5) return brick(Blocks.GRAVEL);
            if (y >= -4 && y <= -1) return brick(Blocks.WATER);
            if (y == 0 && (Math.abs(x) == 11 || Math.abs(x) == 35 || z == -87 || z == -62)) return brick(Blocks.SPRUCE_PLANKS);
            if (y >= 0 && y <= 17) return brick(Blocks.AIR);
        }
        if (y >= -5 && y < 0) return brick(Blocks.STONE);
        if (y == 0) return brick(Blocks.SPRUCE_PLANKS);
        if (y == 1 && (Math.abs(x) == 40 || z == -92) && Math.abs(x) > 6) return fence();
        if (y == 1 && Math.abs(x) == 9 && z % 8 == 0) return brick(Blocks.BARREL);
        if (y == 2 && Math.abs(x) == 9 && z % 8 == 0) return brick(Blocks.LANTERN);
        return y <= 17 ? brick(Blocks.AIR) : null;
    }

    private static BlockState iceSanctuary(int x, int y, int z) {
        int dz = z - 76;
        int distanceSquared = x * x + dz * dz;
        boolean chamber = distanceSquared <= 15 * 15;
        boolean tunnel = z <= 65 && Math.abs(x) <= 7;
        if (!chamber && !tunnel) return null;
        int ceiling = chamber ? 14 : 9;
        if (y < -2 || y > ceiling + 3) return null;
        if (y <= 0) {
            boolean floorLight = Math.abs(x) == 7 && z == 76 || x == 0 && (z == 69 || z == 83);
            return brick(y == 0 && floorLight ? Blocks.SEA_LANTERN : Blocks.SNOW_BLOCK);
        }
        boolean wall = z <= 65 ? Math.abs(x) >= 6 : chamber && distanceSquared >= 14 * 14;
        if (y > ceiling || wall) return brick(y == ceiling + 3 ? Blocks.SNOW_BLOCK : Blocks.BLUE_ICE);
        if (y == ceiling && Math.abs(x) == 8 && Math.abs(dz) == 8) return brick(Blocks.SEA_LANTERN);
        if (isLoot(x, y, z)) return chest(Direction.NORTH);
        return brick(Blocks.CAVE_AIR);
    }

    private static boolean near(int value, int center, int radius) { return Math.abs(value - center) <= radius; }
    private static boolean towerColumn(int x, int z) { return Math.abs(Math.abs(x) - 68) <= 8 && Math.abs(z + 59) <= 8; }
    private static boolean isLoot(int x, int y, int z) {
        if (y == 1 && Math.abs(x) == 13 && z == 79) return true;
        if (Math.abs(x) != 72 || y > 1 || y < -47 || Math.floorMod(y, 12) != 1) return false;
        return (z == -9 || z == 18 || z == 45) && !(x > 0 && y == -23 && z == 18);
    }
    private static BlockState brick(Block block) { return block.defaultBlockState(); }
    private static BlockState masonry(int x, int y, int z) {
        return brick(Math.floorMod(y, 12) == 1 || Math.floorMod(y, 12) == 8 ? Blocks.POLISHED_ANDESITE
                : Math.floorMod(x + z, 12) == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
    }
    private static BlockState fence() { return Blocks.SPRUCE_FENCE.defaultBlockState()
            .setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true)
            .setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true); }
    private static boolean onPerimeter(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ
                && (x == minX || x == maxX || z == minZ || z == maxZ);
    }
    private static BlockState perimeterFence(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        return Blocks.SPRUCE_FENCE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, onPerimeter(x, z - 1, minX, maxX, minZ, maxZ))
                .setValue(BlockStateProperties.SOUTH, onPerimeter(x, z + 1, minX, maxX, minZ, maxZ))
                .setValue(BlockStateProperties.EAST, onPerimeter(x + 1, z, minX, maxX, minZ, maxZ))
                .setValue(BlockStateProperties.WEST, onPerimeter(x - 1, z, minX, maxX, minZ, maxZ));
    }
    private static BlockState grille(boolean alongX) { return Blocks.IRON_BARS.defaultBlockState()
            .setValue(BlockStateProperties.NORTH, !alongX).setValue(BlockStateProperties.SOUTH, !alongX)
            .setValue(BlockStateProperties.EAST, alongX).setValue(BlockStateProperties.WEST, alongX); }
    private static BlockState gate(Direction facing) { return Blocks.SPRUCE_FENCE_GATE.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, facing); }
    private static BlockState chest(Direction facing) { return Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing); }
    private static BlockState registry(String id, Block fallback) {
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return (block == null || block == Blocks.AIR ? fallback : block).defaultBlockState();
    }
}
