package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.worldgen.structure.MansionBasementLayoutV10;
import net.amurdza.examplemod.worldgen.structure.MansionBasementLayoutV11;
import net.amurdza.examplemod.worldgen.structure.MansionBasementLayoutV5;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember18BasementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void twoCellarLevelsPreserveRoomsAndHaveFiveFlatBlocksBeforeTheContinuingFlight(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 11);
            var cellar = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            var blocks = MansionBasementV3Tests.place(h, cellar);
            h.assertTrue(cellar.layoutVersion() == 11 && cellar.lowestFloorY() == cellar.keyAnchor().getY() - 7
                            && cellar.getBoundingBox().minY() == cellar.lowestFloorY() - 2,
                    "The second cellar and its foundation must fit inside the saved bounds");
            for (int layer = 0; layer < 2; layer++) {
                int dy = layer == 0 ? 0 : -7, dz = layer == 0 ? 5 : 12;
                for (int x = -48; x <= 31; x++) {
                    if (x > -6 && x < 7) continue;
                    for (int z = 14; z <= 73; z++) for (int y = 0; y <= 6; y++) {
                        var expected = MansionBasementLayoutV10.cell(x, y, z);
                        // A null cell is outside this layer's authored volume.
                        // In particular, the upper mine floor can legitimately
                        // occupy the course above the lower mine's own roof.
                        if (expected == null) continue;
                        var actual = blocks.get(cellar.world(x, y + dy, z + dz));
                        h.assertTrue(Objects.equals(actual, expected.rotate(rotation)),
                                "Both floors must preserve every authored room, crop channel and mine at layer " + layer + ", " + x + "," + y + "," + z
                                        + ": expected " + expected.rotate(rotation) + ", found " + actual);
                    }
                }
                h.assertTrue(MansionBasementLayoutV11.roomAt(25, 1 + dy, 67 + dz) == MansionBasementLayoutV5.Room.SMITH,
                        "Both smithies must retain the smith loot table");
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = 8; z <= 12; z++) assertBlock(h, blocks, cellar.world(x, 0, z), Blocks.STONE_BRICKS, "Leave five whole flat blocks before descending again");
                for (int z = 13; z <= 19; z++) {
                    int y = 13 - z;
                    var stair = blocks.get(cellar.world(x, y, z));
                    h.assertTrue(stair.is(Blocks.STONE_BRICK_STAIRS) && stair.getValue(StairBlock.FACING) == rotation.rotate(Direction.NORTH),
                            "Continue the staircase centrally to the second floor");
                    for (int head = 1; head <= 4; head++) {
                        var state = blocks.get(cellar.world(x, y + head, z));
                        h.assertTrue(state.isAir() || state.is(Blocks.WALL_TORCH), "The entire continuing flight needs four clear blocks above every tread");
                    }
                }
                for (int z = 20; z <= 24; z++) assertBlock(h, blocks, cellar.world(x, -7, z), Blocks.STONE_BRICKS, "The lower patio remains five blocks deep");
            }
            int firstLamps = 0, lowerLamps = 0;
            for (int x = -6; x <= 6; x++) {
                for (int z = 8; z <= 17; z++) if (blocks.get(cellar.world(x, 4, z)).is(Blocks.LANTERN)) firstLamps++;
                for (int z = 20; z <= 24; z++) if (blocks.get(cellar.world(x, -3, z)).is(Blocks.LANTERN)) lowerLamps++;
            }
            h.assertTrue(firstLamps == 6 && lowerLamps == 3, "The ten-deep first patio needs six lanterns and the five-deep lower patio needs three");
            for (var door : cellar.doorLeaves()) {
                assertBlock(h, blocks, door, Blocks.IRON_DOOR, "All basement entrances must be iron");
                assertBlock(h, blocks, door.above(), Blocks.IRON_DOOR, "Keep each complete iron door");
                h.assertTrue(cellar.isDoor(door) && cellar.isDoor(door.above()), "Both door halves must be discoverable after rotation");
            }
            h.assertTrue(cellar.doorLeaves().size() == 3, "Provide two independent side doors upstairs and the lower central door");
            for (int x = -3; x <= 3; x++) for (int z = 0; z <= 7; z++) {
                var state = blocks.get(cellar.world(x, 7, z));
                h.assertTrue(state != null && !state.is(Blocks.OAK_PLANKS) && !state.is(Blocks.BIRCH_PLANKS),
                        "The top stair course, including its front buried corners, must use stone");
            }
            h.assertTrue(blocks.values().stream().noneMatch(state -> state.is(Blocks.BIRCH_PLANKS)), "New cellars must contain no birch planks");
            for (var pos : blocks.keySet()) h.assertTrue(cellar.getBoundingBox().isInside(pos), "A clipped two-level cellar block escaped its saved bounds");
            h.assertTrue(cellar.createTag(serial).getInt("Chickens") == -1, "All thirty-two livestock slots must survive in the persisted spawn mask");

            var rooms = List.of(BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8)));
            var index = new PlacedMansions(); index.record(cellar, rooms);
            var indexed = PlacedMansions.load(index.save(new CompoundTag())).at(cellar.firstDoor()).get(0).basement();
            var packet = MansionProtectionSnapshot.decode(new MansionProtectionSnapshot(rooms, List.of(cellar)).encode());
            for (var restored : List.of(indexed, packet.basements().get(0))) {
                h.assertTrue(restored.layoutVersion() == 11 && restored.lowestFloorY() == cellar.lowestFloorY()
                                && restored.doorLeaves().equals(cellar.doorLeaves()), "Both protection codecs must preserve both floors and all three doors");
                for (var pos : List.of(cellar.world(31, -6, 79), cellar.world(-48, -6, 28), cellar.world(1, -5, 24)))
                    h.assertTrue(restored.isBuiltCell(pos), "The deeper ovens, mine backing and switches must retain construction protection");
            }
            var legacy = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 10);
            var savedLegacy = (MansionBasementPiece) MansionBasementPiece.load(serial, legacy.createTag(serial));
            h.assertTrue(savedLegacy.lowestFloorY() == savedLegacy.keyAnchor().getY() && savedLegacy.doorLeaves().size() == 2,
                    "Saved V10 must retain one floor and its coupled oak entrance");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void ironBasementDoorsUseTheirSymmetricFrontAndBackLeversInEveryRotation(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockSurvivalPlayer();
        for (var rotation : Rotation.values()) {
            var cellar = new MansionBasementPiece(h.absolutePos(new BlockPos(64 + rotation.ordinal() * 48, 0, 64)).atY(150), rotation, 11);
            // Place just the actual doorway neighborhoods. Known-shape placement
            // matches generation; using the levers below performs real updates.
            for (int x = -7; x <= 7; x++) for (int z = 17; z <= 26; z++) for (int y = -8; y <= 3; y++) {
                var state = MansionBasementLayoutV11.cell(x, y, z);
                if (state != null) level.setBlock(cellar.world(x, y, z), state.rotate(rotation), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            PlacedMansions.get(level).record(cellar, List.of(BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8))));
            for (var door : cellar.doorLeaves()) {
                var click = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, door,
                        new BlockHitResult(Vec3.atCenterOf(door), rotation.rotate(Direction.NORTH), door, false));
                MansionProtection.use(click);
                h.assertTrue(!click.isCanceled() && !level.getBlockState(door).getValue(DoorBlock.OPEN),
                        "An empty hand must not bypass the new iron doors' levers");
            }
            var controls = List.of(
                    new Control(new BlockPos(-5, 2, 17), new BlockPos(-4, 1, 18)),
                    new Control(new BlockPos(-5, 2, 19), new BlockPos(-4, 1, 18)),
                    new Control(new BlockPos(5, 2, 17), new BlockPos(4, 1, 18)),
                    new Control(new BlockPos(5, 2, 19), new BlockPos(4, 1, 18)),
                    new Control(new BlockPos(-1, -5, 24), new BlockPos(0, -6, 25)),
                    new Control(new BlockPos(1, -5, 24), new BlockPos(0, -6, 25)),
                    new Control(new BlockPos(-1, -5, 26), new BlockPos(0, -6, 25)),
                    new Control(new BlockPos(1, -5, 26), new BlockPos(0, -6, 25)));
            for (var control : controls) {
                var local = control.lever(); var pos = cellar.world(local.getX(), local.getY(), local.getZ());
                var doorLocal = control.door(); var door = cellar.world(doorLocal.getX(), doorLocal.getY(), doorLocal.getZ());
                h.assertTrue(level.getBlockState(pos).is(Blocks.LEVER) && level.getBlockState(pos).canSurvive(level, pos)
                                && MansionProtection.protectedAt(level, pos), "Each rotated lever needs a solid protected jamb");
                for (boolean open : new boolean[]{true, false}) {
                    var lever = level.getBlockState(pos);
                    ((LeverBlock) lever.getBlock()).use(lever, level, pos, player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos), lever.getValue(LeverBlock.FACING), pos, false));
                    for (var leaf : cellar.doorLeaves()) for (var half : List.of(leaf, leaf.above())) {
                        var state = level.getBlockState(half);
                        h.assertTrue(state.is(Blocks.IRON_DOOR) && state.getValue(DoorBlock.OPEN) == (leaf.equals(door) && open)
                                        && state.getValue(DoorBlock.POWERED) == (leaf.equals(door) && open),
                                "Each front/back lever must control only its own iron door, on both halves");
                    }
                }
            }
        }
        h.succeed();
    }

    private record Control(BlockPos lever, BlockPos door) { }

    private static void assertBlock(GameTestHelper h, Map<BlockPos, BlockState> blocks, BlockPos pos, Block expected, String message) {
        h.assertTrue(blocks.containsKey(pos) && blocks.get(pos).is(expected), message + " at " + pos + ": " + blocks.get(pos));
    }
}
