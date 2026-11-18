package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionBasementV3Tests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void widenedBasementKeepsStationsMinesAndSoilInsideTheirRooms(GameTestHelper h) {
        var level = h.getLevel();
        var serial = StructurePieceSerializationContext.fromLevel(level);
        for (Rotation rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(8, 100, 8), rotation, 3);
            var piece = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(piece.layoutVersion() == 3 && piece.doorLeaves().size() == 1, "New pieces must retain v3's single lock after reload");
            var blocks = place(h, piece);
            java.util.function.Function<BlockPos, BlockState> at = p -> blocks.getOrDefault(p, Blocks.AIR.defaultBlockState());
            int chests = 0, singles = 0, anvils = 0, counters = 0;
            for (var entry : blocks.entrySet()) {
                var state = entry.getValue();
                if (state.is(Blocks.ANVIL)) anvils++;
                if (state.is(Blocks.CRAFTING_TABLE)) counters++;
                if (!state.is(Blocks.CHEST)) continue;
                chests++;
                if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) { singles++; continue; }
                var other = at.apply(entry.getKey().relative(ChestBlock.getConnectedDirection(state)));
                h.assertTrue(other.is(Blocks.CHEST) && other.getValue(ChestBlock.TYPE) != state.getValue(ChestBlock.TYPE),
                        "A rotated double chest lost its matching half: " + entry.getKey());
            }
            h.assertTrue(chests == 35 && singles == 1 && anvils == 12 && counters == 20,
                    "Retain 17 double chests plus a central single, with trimmed workstations: " + chests + "/" + singles + "/" + anvils + "/" + counters);
            for (int step = 0; step < 10; step++) for (int x = -1; x <= 1; x++) {
                h.assertTrue(at.apply(piece.world(x, 10 - step, step)).is(Blocks.STONE_BRICK_STAIRS)
                                && at.apply(piece.world(x, 12 - step, step)).isAir(), "All three stair lanes need clear ascending headroom");
            }
            for (int x = -3; x <= 3; x++) h.assertTrue(at.apply(piece.world(x, 1, 20)).isAir(), "The basement hall must be seven wide");
            for (int x = -4; x <= 4; x++) h.assertTrue(at.apply(piece.world(x, 1, 55)).isAir() == (Math.abs(x) <= 1),
                    "The smith entrance must be exactly three wide");
            h.assertTrue(at.apply(piece.firstDoor()).is(Blocks.IRON_DOOR) && !piece.isDoor(piece.world(-1, 1, 13)),
                    "The lock must have one centered leaf");
            h.assertTrue(at.apply(piece.world(23, 1, 29)).is(Blocks.CHEST)
                            && at.apply(piece.world(23, 1, 29)).getValue(ChestBlock.TYPE) == ChestType.SINGLE,
                    "The widened granary needs its single chest in the center");
            for (int z : new int[]{25, 33}) for (int y = 1; y <= 5; y++)
                h.assertTrue(at.apply(piece.world(28, y, z)).is(Blocks.STONE_BRICKS), "The store room's rear columns must reach its ceiling");
            for (int x = -28; x <= -18; x++) for (int z = 35; z <= 43; z++) {
                boolean water = x == -23 && z >= 36 && z <= 42;
                h.assertTrue(at.apply(piece.world(x, 0, z)).is(water ? Blocks.WATER : Blocks.PODZOL)
                                && at.apply(piece.world(x, -1, z)).is(Blocks.STONE_BRICKS),
                        "Mushroom floors must be fully authored podzol over solid brick, apart from irrigation");
            }
            for (int sign : new int[]{-1, 1}) for (int firstZ : sign < 0 ? new int[]{25, 35} : new int[]{35}) {
                for (int center : new int[]{1, 4, 7}) for (int side = 29; side <= 44; side++) {
                    for (int y = 1; y <= 3; y++) {
                        var pos = piece.world(sign * side, y, firstZ + center);
                        h.assertTrue(at.apply(pos).getCollisionShape(level, pos).isEmpty(), "Each of three mines needs sixteen clear blocks: " + pos);
                    }
                    for (int y : new int[]{0, 4}) {
                        var pos = piece.world(sign * side, y, firstZ + center);
                        h.assertTrue(at.apply(pos).is(Blocks.STONE) && !piece.isBuiltCell(pos), "The mine floor and ceiling must be mineable even at the mouth");
                    }
                    h.assertTrue(at.apply(piece.world(sign * side, -1, firstZ + center)).is(Blocks.STONE_BRICKS)
                                    && at.apply(piece.world(sign * side, 5, firstZ + center)).is(Blocks.STONE_BRICKS),
                            "Mine floors and ceilings need a brick backing outside their mineable stone");
                }
                h.assertTrue(at.apply(piece.world(sign * 46, 2, firstZ + 4)).is(Blocks.STONE_BRICKS), "Mine end backing is missing");
            }
            for (int z : new int[]{45, 53}) {
                h.assertTrue(at.apply(piece.world(27, 1, z)).is(Blocks.CRAFTING_TABLE)
                                && at.apply(piece.world(28, 1, z)).is(Blocks.STONE_BRICKS)
                                && at.apply(piece.world(29, 1, z)).is(Blocks.STONE_BRICKS),
                        "Kitchen counters must stop before the back wall and never continue inside it");
            }
            for (int x : new int[]{-5, 5}) for (int z = 60; z <= 65; z++)
                h.assertTrue(at.apply(piece.world(x, 1, z)).is(Blocks.STONE_BRICKS), "No inaccessible blast furnace may hide behind an anvil");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void basementBeginsAtTheInteriorLobbyInEveryRotationAndKeepsOldBlueprints(GameTestHelper h) {
        var level = h.getLevel();
        var manager = level.getStructureManager();
        for (Rotation rotation : Rotation.values()) for (int seed = 0; seed < 4; seed++) {
            var builder = new StructurePiecesBuilder();
            IllagerMansionStructure.addMansionPieces(manager, new BlockPos(0, 100, 0), rotation, builder, RandomSource.create(seed));
            var pieces = builder.build().pieces();
            var upper = pieces.stream().filter(WoodlandMansionPiece.class::isInstance).map(WoodlandMansionPiece.class::cast).toList();
            var landing = MansionUpperLayout.basementLanding(manager, upper);
            var cellar = pieces.stream().filter(MansionBasementPiece.class::isInstance).map(MansionBasementPiece.class::cast).findFirst().orElseThrow();
            var next = landing.position().relative(landing.rotation().rotate(Direction.SOUTH));
            h.assertTrue(cellar.world(0, cellar.mainFloorOffset(), 0).equals(landing.position())
                            && cellar.world(0, cellar.mainFloorOffset(), 1).equals(next),
                    "Basement descent must start at the dedicated interior lobby and face its flight: " + rotation);
        }
        var serial = StructurePieceSerializationContext.fromLevel(level);
        for (int version : new int[]{1, 2}) {
            var old = new MansionBasementPiece(new BlockPos(8, 100, 8), Rotation.CLOCKWISE_90, version);
            var restored = (MansionBasementPiece) MansionBasementPiece.load(serial, old.createTag(serial));
            h.assertTrue(restored.layoutVersion() == version && restored.doorLeaves().size() == 2
                            && restored.firstDoor().equals(old.firstDoor()) && restored.secondDoor().equals(old.secondDoor()),
                    "Saved old stair and paired-door positions must not shift");
            var manual = new PlacedMansions();
            manual.record(old, List.of(BoundingBox.fromCorners(old.keyAnchor(), old.keyAnchor().offset(6, 8, 6))));
            h.assertTrue(PlacedMansions.load(manual.save(new CompoundTag())).at(old.firstDoor()).get(0).basement().layoutVersion() == version,
                    "The command-placed index must also retain each older blueprint");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void legacyDoorOpensByHandAndKeepsItsMineableMouth(GameTestHelper h) {
        var level = h.getLevel();
        var entrance = h.absolutePos(new BlockPos(72, 0, -72)).atY(210);
        var cellar = new MansionBasementPiece(entrance, Rotation.NONE, 3);
        PlacedMansions.get(level).record(cellar, List.of(BoundingBox.fromCorners(entrance, entrance.offset(6, 8, 6))));
        var door = cellar.firstDoor();
        level.setBlock(door.below(), Blocks.STONE_BRICKS.defaultBlockState(), 18);
        level.setBlock(door, Blocks.IRON_DOOR.defaultBlockState(), 18);
        level.setBlock(door.above(), Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);
        var player = h.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        for (boolean open : new boolean[]{true, false}) {
            MansionProtection.use(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, door,
                    new BlockHitResult(Vec3.atCenterOf(door), Direction.NORTH, door, false)));
            h.assertTrue(level.getBlockState(door).getValue(DoorBlock.OPEN) == open
                            && level.getBlockState(door.above()).getValue(DoorBlock.OPEN) == open,
                    "Empty-hand use must toggle exactly one leaf and both vertical halves");
        }
        var mouth = cellar.world(-29, 0, 26);
        var backing = mouth.below();
        level.setBlock(mouth, Blocks.STONE.defaultBlockState(), 18);
        level.setBlock(backing, Blocks.STONE_BRICKS.defaultBlockState(), 18);
        for (var pos : List.of(mouth, backing)) {
            var event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
            MansionProtection.breakBlock(event);
            h.assertTrue(event.isCanceled() == pos.equals(backing), "Mining may break the stone mouth but not the brick backing");
        }
        h.succeed();
    }

    static Map<BlockPos, BlockState> place(GameTestHelper h, MansionBasementPiece piece) {
        var level = h.getLevel();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var fixtureChunk = level.getChunkAt(h.absolutePos(BlockPos.ZERO));
        BoundingBox[] active = {null};
        WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(MansionBasementV3Tests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBlockState": return blocks.getOrDefault(args[0], Blocks.AIR.defaultBlockState());
                        case "getBlockEntity": return null;
                        case "getChunk": return fixtureChunk;
                        case "setBlock": {
                            var pos = ((BlockPos) args[0]).immutable();
                            h.assertTrue(active[0].isInside(pos), "The revised basement wrote outside its generating chunk");
                            blocks.put(pos, (BlockState) args[1]); return true;
                        }
                        case "addFreshEntityWithPassengers": return null;
                        case "addFreshEntity": return true;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        var box = piece.getBoundingBox();
        for (int x = box.maxX() >> 4; x >= box.minX() >> 4; x--) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            active[0] = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
            piece.postProcess(world, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), active[0], chunk, piece.keyAnchor());
        }
        return blocks;
    }
}
