package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.worldgen.PlainsWaterProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.registries.ForgeRegistries;

public final class MansionBasementPiece extends StructurePiece {
    private static final ResourceLocation PANTRY = new ResourceLocation("aoemod", "chests/illager_mansion_pantry");
    private static final ResourceLocation SMITH = new ResourceLocation("aoemod", "chests/illager_mansion_smith");
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private enum Room { WHEAT, CARROT, POTATO, BEET, CABBAGE, KITCHEN, GRANARY, PUMPKIN, CANE, COOP, MUSHROOM }
    // In each pair the first room opens onto the central aisle, and the second lies beyond it.
    private static final Room[][] WEST = {{Room.WHEAT, Room.PUMPKIN}, {Room.CARROT, Room.CABBAGE},
            {Room.BEET, Room.MUSHROOM}, {Room.POTATO, Room.CANE}};
    private static final Room[][] EAST = {{Room.CABBAGE, Room.COOP}, {Room.BEET, Room.GRANARY},
            {Room.WHEAT, Room.POTATO}, {Room.CARROT, Room.KITCHEN}};
    private BlockPos floor;
    private final Rotation rotation;
    private int chickens;
    private final int layoutVersion;
    private final boolean decorativeWalls;
    private java.util.UUID encounterId;
    private int defenderCount;

    public MansionBasementPiece(BlockPos entrance) { this(entrance, Rotation.NONE); }

    public MansionBasementPiece(BlockPos entrance, Rotation rotation) {
        this(entrance, rotation, 14);
    }

    public MansionBasementPiece(BlockPos entrance, Rotation rotation, int layoutVersion) {
        this(entrance, rotation, layoutVersion, true);
    }

    public MansionBasementPiece(BlockPos entrance, Rotation rotation, int layoutVersion, boolean decorativeWalls) {
        super(ModStructures.MANSION_BASEMENT_PIECE.get(), 0, BoundingBox.fromCorners(
                entrance.offset(new BlockPos(layoutVersion < 2 ? -33 : layoutVersion == 2 ? -45 : layoutVersion == 3 ? -46 : layoutVersion < 6 ? -47 : -48,
                        layoutVersion < 2 ? -28 : layoutVersion >= 11 ? -16 : layoutVersion >= 7 ? -9 : -12, -1).rotate(rotation)),
                entrance.offset(new BlockPos(layoutVersion < 2 ? 32 : layoutVersion == 2 ? 44 : layoutVersion == 3 ? 46 : layoutVersion >= 10 ? 31 : 30,
                        layoutVersion == 8 ? 8 : layoutVersion >= 7 ? 7 : 3, layoutVersion < 2 ? 67 : layoutVersion == 2 ? 65 : layoutVersion == 3 ? 69 : layoutVersion >= 11 ? 85 : 73).rotate(rotation))));
        this.rotation = rotation;
        this.layoutVersion = layoutVersion;
        this.decorativeWalls = decorativeWalls;
        floor = entrance.below(mainFloorOffset(layoutVersion));
    }

    private MansionBasementPiece(CompoundTag tag) {
        super(ModStructures.MANSION_BASEMENT_PIECE.get(), tag);
        rotation = Rotation.valueOf(tag.getString("StairRotation"));
        floor = BlockPos.of(tag.getLong("BasementFloor"));
        chickens = tag.getInt("Chickens");
        layoutVersion = tag.contains("LayoutVersion") ? tag.getInt("LayoutVersion") : 1;
        decorativeWalls = !tag.contains("DecorativeWalls") || tag.getBoolean("DecorativeWalls");
        readEncounter(tag);
    }

    public static StructurePiece load(StructurePieceSerializationContext context, CompoundTag tag) {
        return tag.contains("BasementFloor") ? new MansionBasementPiece(tag) : new LegacyMansionBasementPiece(context, tag);
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("StairRotation", rotation.name());
        tag.putLong("BasementFloor", floor.asLong());
        tag.putInt("Chickens", chickens);
        tag.putInt("LayoutVersion", layoutVersion);
        tag.putBoolean("DecorativeWalls", decorativeWalls);
        saveEncounter(tag);
    }

    public java.util.UUID encounterId() { return encounterId; }
    public int defenderCount() { return defenderCount; }
    public void encounter(java.util.UUID id, int expected) { encounterId = id; defenderCount = expected; }
    public void saveEncounter(CompoundTag tag) {
        if (encounterId != null) { tag.putUUID("EncounterId", encounterId); tag.putInt("DefenderCount", defenderCount); }
    }
    public void readEncounter(CompoundTag tag) {
        if (tag.hasUUID("EncounterId")) encounter(tag.getUUID("EncounterId"), tag.getInt("DefenderCount"));
    }

    @Override public void move(int x, int y, int z) {
        super.move(x, y, z);
        floor = floor.offset(x, y, z);
    }

    public BlockPos world(int x, int y, int z) { return floor.offset(new BlockPos(x, y, z).rotate(rotation)); }
    private BlockPos local(BlockPos pos) {
        return pos.subtract(floor).rotate(switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        });
    }
    public boolean isBuiltCell(BlockPos pos) {
        var p = local(pos);
        var state = cell(p.getX(), p.getY(), p.getZ(), Blocks.WHEAT);
        // The stone beyond the brick tunnel mouth is ordinary mineable terrain.
        int side = p.getX() < 0 ? -1 - p.getX() : p.getX();
        return state != null && !state.isAir() && (layoutVersion < 2 || layoutVersion == 2 && side <= 29
                || layoutVersion >= 10 || layoutVersion >= 3 && !state.is(Blocks.STONE));
    }
    public boolean containsAuthoredSpace(BlockPos pos) {
        if (!boundingBox.isInside(pos)) return false;
        var p = local(pos);
        return cell(p.getX(), p.getY(), p.getZ(), Blocks.WHEAT) != null;
    }
    public boolean isMineOre(BlockPos pos) {
        if (layoutVersion < 14 || !boundingBox.isInside(pos)) return false;
        var p = local(pos);
        return MansionBasementLayoutV14.isOre(cell(p.getX(), p.getY(), p.getZ(), Blocks.WHEAT));
    }
    public int layoutVersion() { return layoutVersion; }
    public static int mainFloorOffset(int version) { return version >= 7 ? 7 : 10; }
    public int mainFloorOffset() { return mainFloorOffset(layoutVersion); }
    public int mainFloorY() { return floor.getY() + mainFloorOffset(); }
    public int lowestFloorY() { return floor.getY() + (layoutVersion >= 11 ? MansionBasementLayoutV11.LOWER_FLOOR : 0); }
    public boolean decorativeWalls() { return decorativeWalls; }
    public boolean gentleRamp() { return layoutVersion >= 8; }
    public int groundsY() { return mainFloorY() - (decorativeWalls ? 1 : 0); }
    public Rotation stairRotation() { return rotation; }
    public BlockPos keyAnchor() { return floor; }
    public BlockPos firstDoor() { return layoutVersion >= 11 ? world(-4, 1, 18) : world(layoutVersion >= 3 ? 0 : -1, 1, 13); }
    public BlockPos secondDoor() { return layoutVersion >= 11 ? world(4, 1, 18) : world(layoutVersion >= 10 ? 1 : 0, 1, 13); }
    public java.util.List<BlockPos> doorLeaves() {
        if (layoutVersion >= 11) return MansionBasementLayoutV11.doors().stream().map(p -> world(p.getX(), p.getY(), p.getZ())).toList();
        return layoutVersion >= 3 && layoutVersion < 10 ? java.util.List.of(firstDoor()) : java.util.List.of(firstDoor(), secondDoor());
    }
    public boolean isDoor(BlockPos pos) {
        var p = local(pos);
        if (layoutVersion >= 11) return MansionBasementLayoutV11.isDoor(p.getX(), p.getY(), p.getZ());
        return p.getZ() == 13 && (p.getX() == 0 || layoutVersion < 3 && p.getX() == -1 || layoutVersion >= 10 && p.getX() == 1)
                && (p.getY() == 1 || p.getY() == 2);
    }
    public boolean isBasementBrick(BlockPos pos) {
        var p = local(pos);
        return BRICK.equals(cell(p.getX(), p.getY(), p.getZ(), Blocks.WHEAT));
    }
    public boolean isStairwell(BlockPos pos) {
        var p = local(pos);
        if (layoutVersion >= 11) return p.getZ() >= 0 && p.getZ() <= 25
                && Math.abs(p.getX()) <= (p.getZ() < 8 ? 4 : 7);
        if (layoutVersion >= 7) return p.getZ() >= 0 && p.getZ() <= 13
                && Math.abs(p.getX()) <= (p.getZ() < 8 ? 4 : 7);
        if (layoutVersion >= 4) return p.getZ() >= 0 && p.getZ() <= 13 && Math.abs(p.getX()) <= (p.getZ() < 10 ? 3 : 6);
        return p.getX() >= (layoutVersion >= 3 ? -2 : -3) && p.getX() <= 2 && p.getZ() >= 0 && p.getZ() <= 13;
    }

    @Override public synchronized void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        Block cabbage = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("farmersdelight", "cabbages"));
        for (int wx = Math.max(clip.minX(), boundingBox.minX()); wx <= Math.min(clip.maxX(), boundingBox.maxX()); wx++) {
            for (int wz = Math.max(clip.minZ(), boundingBox.minZ()); wz <= Math.min(clip.maxZ(), boundingBox.maxZ()); wz++) {
                BlockPos column = local(new BlockPos(wx, floor.getY(), wz));
                int x = column.getX(), z = column.getZ();
                for (int y = Math.max(-18, clip.minY() - floor.getY()); y <= Math.min(layoutVersion == 8 ? 15 : layoutVersion >= 7 ? 14 : 13, clip.maxY() - floor.getY()); y++) {
                    BlockState state = cell(x, y, z, cabbage);
                    if (state == null) continue;
                    var pos = new BlockPos(wx, floor.getY() + y, wz);
                    if (state.is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity) continue;
                    level.setBlock(pos, state.rotate(rotation), FLAGS);
                    if (!state.getFluidState().isEmpty()) PlainsWaterProof.mark(level.getChunk(chunk.x, chunk.z), pos);
                    if (state.is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                        chest.setLootTable((layoutVersion < 2 ? z > 53 : layoutVersion == 2 ? z >= 52 : layoutVersion == 3 ? z >= 55
                                : layoutVersion == 4 ? MansionBasementLayoutV4.roomAt(x, z) == MansionBasementLayoutV4.Room.SMITH
                                : layoutVersion >= 11 ? MansionBasementLayoutV11.roomAt(x, y, z) == MansionBasementLayoutV5.Room.SMITH
                                : MansionBasementLayoutV5.roomAt(layoutVersion >= 10 ? MansionBasementLayoutV10.previousX(x, z) : x, z) == MansionBasementLayoutV5.Room.SMITH)
                                ? SMITH : PANTRY, level.getSeed() ^ pos.asLong());
                    }
                }
            }
        }
        if (layoutVersion >= 4) {
            var animals = new java.util.ArrayList<MansionBasementLayoutV4.Animal>();
            if (layoutVersion == 4) animals.addAll(MansionBasementLayoutV4.animals());
            else for (int layer = 0; layer < (layoutVersion >= 11 ? 2 : 1); layer++) {
                int dy = layer == 1 ? MansionBasementLayoutV11.LOWER_FLOOR : 0;
                int dz = layoutVersion >= 11 ? layer == 0 ? MansionBasementLayoutV11.FIRST_ROOM_SHIFT : MansionBasementLayoutV11.SECOND_ROOM_SHIFT : 0;
                for (var animal : MansionBasementLayoutV5.animals())
                    animals.add(new MansionBasementLayoutV4.Animal(animal.id(), animal.pos().offset(0, dy, dz)));
            }
            for (int i = 0; i < animals.size(); i++) {
                var animal = animals.get(i);
                int animalX = animal.pos().getX();
                var pos = world(animalX + (layoutVersion >= 10 && animalX >= 6 ? 1 : 0), animal.pos().getY(), animal.pos().getZ());
                if ((chickens & (1 << i)) == 0 && clip.isInside(pos) && BossLairPiece.spawn(level, animal.id(), pos, mob -> {
                    if (encounterId != null) AuthoredEncounterProgress.markLivestock(mob, encounterId, floor, defenderCount, "mansion");
                })) chickens |= 1 << i;
            }
            return;
        }
        for (int i = 0; i < 4; i++) {
            var pos = world((layoutVersion < 2 ? 21 : layoutVersion == 2 ? 18 : 19) + i, 1, 18);
            if ((chickens & (1 << i)) == 0 && clip.isInside(pos) && BossLairPiece.spawn(level, "minecraft:chicken", pos)) {
                chickens |= 1 << i;
            }
        }
    }

    private BlockState cell(int x, int y, int z, Block cabbage) {
        if (layoutVersion < 2) return MansionBasementLayoutV1.cell(x, y, z, cabbage);
        if (layoutVersion == 2) return MansionBasementLayoutV2.cell(x, y, z, cabbage);
        if (layoutVersion == 4) return MansionBasementLayoutV4.cell(x, y, z);
        if (layoutVersion == 5) return MansionBasementLayoutV5.cell(x, y, z);
        if (layoutVersion == 6) return MansionBasementLayoutV6.cell(x, y, z);
        if (layoutVersion == 7) return MansionBasementLayoutV7.cell(x, y, z);
        if (layoutVersion == 8) return MansionBasementLayoutV8.cell(x, y, z);
        if (layoutVersion == 9) return MansionBasementLayoutV9.cell(x, y, z);
        if (layoutVersion == 10) return MansionBasementLayoutV10.cell(x, y, z);
        if (layoutVersion == 11) return MansionBasementLayoutV11.cell(x, y, z);
        if (layoutVersion == 12) return MansionBasementLayoutV12.cell(x, y, z);
        if (layoutVersion == 13) return MansionBasementLayoutV13.cell(x, y, z);
        if (layoutVersion >= 14) return MansionBasementLayoutV14.cell(x, y, z);
        int side = Math.abs(x);
        // A continuous slab sits beneath every channel and the sand-lined cane pond.
        if (y == -2 && side <= 30 && z >= 0 && z <= 69 && !(z < 10 && side <= 1)) return BRICK;
        if (z < 14) return stairwell(x, y, z, side);
        if (z > 69) return null;
        if (z >= 55) return smith(x, y, z);
        if (side >= 29 && side <= 46) {
            for (int firstZ : x < 0 ? new int[]{25, 35} : new int[]{35})
                if (z >= firstZ - 1 && z <= firstZ + 9) return mine(side, y, z - firstZ);
        }
        int row = Math.min(3, (z - 14) / 10);
        int across = z - (15 + 10 * row);
        boolean back = side >= 18;
        int depth = back ? side - 18 : side - 6;
        Room room = (x < 0 ? WEST : EAST)[row][back ? 1 : 0];
        if (side > 30 || y < -2 || y > 6) return null;
        if (y == -2 || y == -1 || y == 6) {
            if (y == -1 && room == Room.CANE && across >= 1 && across <= 7 && depth >= 1 && depth <= 9)
                return Blocks.SAND.defaultBlockState();
            return BRICK;
        }
        if (side <= 3) {
            if (y == 4 && (z - 19) % 10 == 0 && side == 2) return hangingLamp();
            if (y == 5 && (z - 19) % 10 == 0 && side == 2) return Blocks.CHAIN.defaultBlockState();
            return y == 0 ? BRICK : AIR;
        }
        // Adjacent rooms share one wall. The outer rectangular shell remains two courses thick.
        if (across < 0 || across > 8) {
            if (room == Room.KITCHEN && (across == -1 || across == 9) && depth >= 0 && depth <= 9 && y == 2)
                return facing(Blocks.FURNACE, across < 0 ? Direction.SOUTH : Direction.NORTH);
            return BRICK;
        }
        boolean doorway = across >= 3 && across <= 5 && y >= 1 && y <= 3;
        if (side == 4 || side == 5 || side == 17) {
            Room behind = (x < 0 ? WEST : EAST)[row][1];
            if (behind == Room.COOP && side == 17 && across >= 2 && across <= 5 && y == 1)
                return facing(Blocks.OAK_FENCE_GATE, Direction.WEST);
            if (behind == Room.COOP && side == 17 && across == 2 && y >= 2 && y <= 3) return AIR;
            return doorway ? AIR : BRICK;
        }
        if (back && (depth == 11 || depth == 12)) {
            return BRICK;
        }
        if (depth < 0 || depth > 10) return BRICK;
        if ((depth == 2 || depth == 8) && (across == 2 || across == 6)) {
            if (y == 5) return Blocks.CHAIN.defaultBlockState();
            if (y == 4) return hangingLamp();
        }
        return furnishings(room, x >= 0, depth, y, across, cabbage);
    }

    private static BlockState stairwell(int x, int y, int z, int side) {
        // Close the shoulders where the three-wide flight opens into the
        // seven-wide landing. Otherwise untouched lake water beside the last
        // narrow tread can enter the landing from its exposed front corners.
        if (z == 9 && side >= 3 && side <= 4 && y >= -2 && y <= 5) return BRICK;
        if (z < 0 || z > 13 || side > (z < 10 ? 2 : 4)) return null;
        // The stair roof ends two blocks below the basement roof. Close that
        // vertical seam so natural lake water above the stairs cannot enter
        // the hall over its locked doorway.
        if (z == 13 && y >= 3 && y <= 6) return BRICK;
        int step = Math.max(0, 10 - z);
        if (y < step - 2) return null;
        // Keep ground-floor headroom open on every side of the first steps so the corridor remains traversable.
        int ceiling = step + 4;
        if (y > ceiling || y >= 10 && z >= 4 || side == 2 && z < 10 && y >= 10) return null;
        if (z == 13) {
            if (x == 0 && (y == 1 || y == 2)) {
                return facing(Blocks.IRON_DOOR, Direction.SOUTH)
                        .setValue(DoorBlock.HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
            }
            return BRICK;
        }
        if (y < step || side == (z < 10 ? 2 : 4) && y < 10 || y == ceiling && ceiling < 10) return BRICK;
        if (y == step) return z < 10 ? facing(Blocks.STONE_BRICK_STAIRS, Direction.NORTH) : BRICK;
        return AIR;
    }

    private static BlockState furnishings(Room room, boolean east, int depth, int y, int across, Block cabbage) {
        Direction in = east ? Direction.WEST : Direction.EAST;
        Direction out = in.getOpposite();
        if (room == Room.GRANARY && depth == 10 && (across == 0 || across == 8)) return BRICK;
        if (room == Room.GRANARY && y == 1) {
            if (depth == 10 && across >= 1 && across <= 6) return chest(in, (across % 2 == 1) ? Direction.SOUTH : Direction.NORTH);
            if ((across == 0 || across == 8) && depth < 10) return chest(across == 0 ? Direction.SOUTH : Direction.NORTH, depth % 2 == 0 ? out : in);
            if (depth == 5 && across == 4) return facing(Blocks.CHEST, in).setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }
        if (room == Room.KITCHEN) {
            // Counters stop before the back oven wall; there are no hidden bottom
            // furnaces behind workbenches and no stations continuing into masonry.
            if (depth == 10) return across >= 1 && across <= 7 && (y == 1 || y == 2)
                    ? facing(Blocks.FURNACE, in) : BRICK;
            if (y == 1 && (across == 0 || across == 8)) return Blocks.CRAFTING_TABLE.defaultBlockState();
        }
        if (room == Room.COOP) {
            if (y == 0) return (depth == 10 ? Blocks.STONE_BRICKS
                    : across == 0 || across == 8 ? Blocks.WATER : Blocks.GRASS_BLOCK).defaultBlockState();
            if (y == 1 && depth == 10) return Blocks.HAY_BLOCK.defaultBlockState();
        }
        if (room == Room.MUSHROOM) {
            if (depth == 5 && across >= 1 && across <= 7) return y == 0 ? Blocks.WATER.defaultBlockState() : AIR;
            if (y == 0) return Blocks.PODZOL.defaultBlockState();
            if (y == 1 && depth >= 1 && depth <= 9 && across >= 1 && across <= 7 && (depth + across) % 3 == 0)
                return (depth % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
            return AIR;
        }
        if (room == Room.KITCHEN || room == Room.GRANARY || room == Room.COOP
                || depth < 1 || depth > 9 || across < 1 || across > 7) return y == 0 ? BRICK : AIR;
        if (room == Room.CANE) {
            if (y == 0) return depth % 3 == 0 ? Blocks.SUGAR_CANE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true) : Blocks.WATER.defaultBlockState();
            return depth % 3 == 0 && (y == 1 || y == 2) ? Blocks.SUGAR_CANE.defaultBlockState() : AIR;
        }
        if (depth == 5) return y == 0 ? Blocks.WATER.defaultBlockState() : AIR;
        if (y == 0) return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
        if (y != 1) return AIR;
        if (room == Room.PUMPKIN) {
            int strip = depth < 5 ? depth - 1 : depth - 6;
            boolean stem = depth < 5 ? strip % 2 == 0 : strip % 2 == 1;
            return stem ? facing(Blocks.ATTACHED_PUMPKIN_STEM, depth < 5 ? out : in) : Blocks.PUMPKIN.defaultBlockState();
        }
        Block crop = switch (room) {
            case WHEAT -> Blocks.WHEAT;
            case CARROT -> Blocks.CARROTS;
            case POTATO -> Blocks.POTATOES;
            case BEET -> Blocks.BEETROOTS;
            case CABBAGE -> cabbage;
            default -> null;
        };
        return crop instanceof CropBlock plant ? plant.getStateForAge(plant.getMaxAge()) : AIR;
    }

    private static BlockState smith(int x, int y, int z) {
        if (Math.abs(x) > 6 || y < -2 || y > 6) return null;
        if (y <= 0 || y == 6 || Math.abs(x) == 6 || z >= 68) return BRICK;
        if (z == 55) return Math.abs(x) <= 1 && y <= 3 ? AIR : BRICK;
        if (z == 67) {
            return Math.abs(x) <= 3 && (y == 1 || y == 2)
                    ? facing(Blocks.BLAST_FURNACE, Direction.NORTH) : BRICK;
        }
        if (Math.abs(x) == 5)
            return z >= 60 && z <= 65 && y == 2 ? facing(Blocks.BLAST_FURNACE, x < 0 ? Direction.EAST : Direction.WEST) : BRICK;
        if (Math.abs(x) == 4) {
            if (z == 66) return BRICK;
            if (z >= 60 && z <= 65 && y == 1) return facing(Blocks.ANVIL, Direction.NORTH);
            if (z <= 59 && y == 1) return chest(x < 0 ? Direction.EAST : Direction.WEST,
                    (z - 56) % 2 == 0 ? Direction.SOUTH : Direction.NORTH);
        }
        if (Math.abs(x) == 2 && (z == 59 || z == 63) && y == 5) return hangingLamp();
        return AIR;
    }

    private static BlockState mine(int side, int y, int across) {
        if (y < -1 || y > 5) return null;
        if (y == -1 || y == 5 || across == -1 || across == 9 || side == 46) return BRICK;
        if (y == 0 || y == 4 || across % 3 != 1 || side == 45) return Blocks.STONE.defaultBlockState();
        if (y == 2 && (side == 34 || side == 39 || side == 44))
            return facing(Blocks.WALL_TORCH, Direction.SOUTH);
        return AIR;
    }

    private static BlockState chest(Direction facing, Direction partner) {
        return facing(Blocks.CHEST, facing).setValue(ChestBlock.TYPE,
                partner == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT);
    }
    private static BlockState facing(Block block, Direction direction) {
        return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
    }
    private static BlockState hangingLamp() { return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true); }
}
