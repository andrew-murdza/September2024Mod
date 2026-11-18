package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionBasementLayoutV7;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember17BasementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void savedV9RestoresTheAuthoredStairCeilingWithoutChangingSavedV8(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (Rotation rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 9);
            var cellar = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            var old = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 8);
            var savedV8 = (MansionBasementPiece) MansionBasementPiece.load(serial, old.createTag(serial));
            h.assertTrue(cellar.layoutVersion() == 9 && cellar.getBoundingBox().maxY() == cellar.mainFloorY() + 7,
                    "New cellars must use the restored authored room height");
            h.assertTrue(savedV8.layoutVersion() == 8 && savedV8.getBoundingBox().maxY() == savedV8.mainFloorY() + 8
                            && savedV8.isBuiltCell(savedV8.world(0, 3, 10)) && savedV8.isBuiltCell(savedV8.world(0, 15, 1)),
                    "Saved V8 must keep its former landing projection and taller room roof");
            var blocks = MansionBasementV3Tests.place(h, cellar);
            var torches = Map.of(
                    new BlockPos(-2, 3, 8), Direction.SOUTH, new BlockPos(2, 3, 8), Direction.SOUTH,
                    new BlockPos(-1, 5, 6), Direction.EAST, new BlockPos(1, 5, 6), Direction.WEST,
                    new BlockPos(-1, 9, 3), Direction.NORTH, new BlockPos(0, 9, 3), Direction.NORTH,
                    new BlockPos(1, 9, 3), Direction.NORTH);
            for (int x = -7; x <= 7; x++) for (int z = 0; z <= 13; z++) for (int y = -2; y <= 15; y++) {
                var local = new BlockPos(x, y, z);
                var expected = MansionBasementLayoutV7.cell(x, y, z);
                if (torches.containsKey(local)) expected = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, torches.get(local));
                if (z == 0 && Math.abs(x) <= 1 && y == 11) expected = Blocks.AIR.defaultBlockState();
                var actual = blocks.get(cellar.world(x, y, z));
                h.assertTrue(Objects.equals(actual, expected == null ? null : expected.rotate(rotation)),
                        "Restore the original stair, headroom and landing; only the requested lights and taller upper doorway differ at " + local);
            }
            for (var torch : torches.entrySet()) assertTorch(h, blocks, cellar, torch.getKey(), torch.getValue(),
                    torch.getKey().getY() == 9 ? Blocks.OAK_PLANKS : Blocks.STONE_BRICKS);
            for (int x = -1; x <= 1; x++) for (int z = 8; z <= 10; z++) {
                for (int y = 1; y <= 5; y++) assertBlock(h, blocks, cellar.world(x, y, z), Blocks.AIR,
                        "The restored landing must have no low ceiling projection");
                assertBlock(h, blocks, cellar.world(x, 6, z), Blocks.STONE_BRICKS, "Keep the original flat landing ceiling");
            }
            long roomLights = 0;
            for (int x = -29; x <= 29; x++) for (int z = 14; z <= 72; z++)
                if (blocks.get(cellar.world(x, 3, z)).is(Blocks.WALL_TORCH)) roomLights++;
            h.assertTrue(roomLights == 132, "Restoring the staircase must retain every room entrance light");
            for (int row : new int[]{3, 4}) for (int z = 14 + row * 12; z <= 24 + row * 12; z++)
                for (int y = 3; y <= 5; y++) assertBlock(h, blocks, cellar.world(row == 3 ? -29 : 29, y, z), Blocks.AIR,
                        "Restoring the staircase must not restore the removed oven hoods");
            for (var pos : blocks.keySet()) h.assertTrue(cellar.getBoundingBox().isInside(pos), "A V9 cell escaped its saved bounds");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void revisedCellarLightsBothFacesOfEveryDoorAndClearsTheOvenHoods(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (Rotation rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 8);
            var cellar = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(cellar.layoutVersion() == 8 && cellar.gentleRamp(), "New cellars must retain their V8 identity after reload");
            var blocks = MansionBasementV3Tests.place(h, cellar);
            Set<BlockPos> roomTorches = new HashSet<>();
            int doorways = 0;
            for (int sign : new int[]{-1, 1}) {
                // Every room has a front entrance; inspect its original aperture
                // independently of the new lighting rules.
                for (int wallX : new int[]{6, 18}) for (int row = 0; row < 5; row++) {
                    int centerZ = 19 + row * 12;
                    h.assertTrue(MansionBasementLayoutV7.cell(sign * wallX, 2, centerZ).isAir(), "Expected an existing front doorway");
                    doorways++;
                    for (int face : new int[]{-1, 1}) for (int flank : new int[]{-2, 2}) {
                        var local = new BlockPos(sign * (wallX + face), 3, centerZ + flank);
                        Direction facing = sign * face < 0 ? Direction.WEST : Direction.EAST;
                        assertTorch(h, blocks, cellar, local, facing, Blocks.STONE_BRICKS);
                        roomTorches.add(cellar.world(local.getX(), local.getY(), local.getZ()));
                    }
                    for (int z = centerZ - 1; z <= centerZ + 1; z++) for (int y = 1; y <= 3; y++)
                        assertLegacyState(h, blocks, cellar, sign * wallX, y, z, "Lighting must not close an entrance or replace its animal gates");
                }
                for (int centerX : new int[]{12, 24}) for (int wallZ : new int[]{25, 37, 49, 61}) {
                    if (!MansionBasementLayoutV7.cell(sign * centerX, 2, wallZ).isAir()) continue;
                    doorways++;
                    for (int face : new int[]{-1, 1}) for (int flank : new int[]{-2, 2}) {
                        var local = new BlockPos(sign * (centerX + flank), 3, wallZ + face);
                        assertTorch(h, blocks, cellar, local, face < 0 ? Direction.NORTH : Direction.SOUTH, Blocks.STONE_BRICKS);
                        roomTorches.add(cellar.world(local.getX(), local.getY(), local.getZ()));
                    }
                    for (int x = centerX - 1; x <= centerX + 1; x++) for (int y = 1; y <= 3; y++)
                        assertLegacyState(h, blocks, cellar, sign * x, y, wallZ, "Keep the existing connecting doorway and animal gates");
                }
            }
            h.assertTrue(doorways == 33 && roomTorches.size() == 132, "Each of the 33 room entrances needs four supported torches");
            Set<BlockPos> actualTorches = new HashSet<>();
            for (int x = -29; x <= 29; x++) for (int z = 14; z <= 72; z++) {
                var pos = cellar.world(x, 3, z);
                if (blocks.get(pos).is(Blocks.WALL_TORCH)) actualTorches.add(pos);
            }
            h.assertTrue(actualTorches.equals(roomTorches), "Closed room partitions must not receive stray doorway lights");

            for (int row : new int[]{3, 4}) {
                int sign = row == 3 ? -1 : 1;
                Block oven = row == 3 ? Blocks.FURNACE : Blocks.BLAST_FURNACE;
                for (int z = 14 + row * 12; z <= 24 + row * 12; z++) {
                    for (int y = 1; y <= 5; y++) assertBlock(h, blocks, cellar.world(sign * 29, y, z), Blocks.AIR, "Clear the complete former oven column");
                    for (int y = 1; y <= 2; y++) assertBlock(h, blocks, cellar.world(sign * 30, y, z), oven, "Keep the working ovens in the rear wall");
                    for (int y = 3; y <= 6; y++) assertBlock(h, blocks, cellar.world(sign * 30, y, z), Blocks.STONE_BRICKS, "Keep the solid exterior wall above the ovens");
                    assertBlock(h, blocks, cellar.world(sign * 29, 6, z), Blocks.STONE_BRICKS, "Removing the hood must preserve the structural ceiling");
                }
            }
            for (int x : new int[]{-2, 2}) assertTorch(h, blocks, cellar, new BlockPos(x, 3, 8), Direction.SOUTH, Blocks.STONE_BRICKS);
            for (int x : new int[]{-1, 1}) assertTorch(h, blocks, cellar, new BlockPos(x, 5, 6), x < 0 ? Direction.EAST : Direction.WEST, Blocks.STONE_BRICKS);
            for (int x = -1; x <= 1; x++) {
                assertTorch(h, blocks, cellar, new BlockPos(x, 6, 6), Direction.NORTH, Blocks.STONE_BRICKS);
                assertBlock(h, blocks, cellar.world(x, 7, 0), Blocks.BIRCH_PLANKS, "Keep a flat upper doorway");
                for (int y = 8; y <= 11; y++) assertBlock(h, blocks, cellar.world(x, y, 0), Blocks.AIR, "Use the taller four-high upper doorway");
                for (int z = 1; z <= 7; z++) {
                    var stair = blocks.get(cellar.world(x, 8 - z, z));
                    h.assertTrue(stair.is(Blocks.STONE_BRICK_STAIRS) && stair.getValue(StairBlock.FACING) == rotation.rotate(Direction.NORTH), "Preserve the seven-riser spawn flight in every rotation");
                    for (int head = 1; head <= 4; head++) {
                        var state = blocks.get(cellar.world(x, 8 - z + head, z));
                        h.assertTrue(state.isAir() || state.is(Blocks.WALL_TORCH), "Stair lights must leave the four-block headroom open");
                    }
                }
                for (int z = 1; z <= 3; z++) {
                    for (int y = 9 - z; y <= 14; y++) assertBlock(h, blocks, cellar.world(x, y, z), Blocks.AIR, "Moving the soffit must clear its old upper columns");
                    assertBlock(h, blocks, cellar.world(x, 15, z), Blocks.BIRCH_PLANKS, "Raise the wooden room ceiling one block");
                }
                for (int z = 4; z <= 10; z++) {
                    int ceiling = 13 - z;
                    assertBlock(h, blocks, cellar.world(x, ceiling, z), ceiling >= 7 ? Blocks.OAK_PLANKS : Blocks.STONE_BRICKS, "Translate the stepped soffit three down and three toward the landing");
                    var headroom = blocks.get(cellar.world(x, ceiling - 1, z));
                    h.assertTrue(headroom.isAir() || headroom.is(Blocks.WALL_TORCH), "Keep the space immediately below the shifted soffit clear");
                }
            }
            for (var pos : blocks.keySet()) h.assertTrue(cellar.getBoundingBox().isInside(pos), "A clipped V8 cell escaped its saved bounds");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void newCellarProtectionCodecsPreserveV8WithoutChangingSavedV7(GameTestHelper h) {
        for (int version : new int[]{7, 8, 9}) for (Rotation rotation : Rotation.values()) for (boolean walls : new boolean[]{true, false}) {
            var cellar = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, version, walls);
            var rooms = List.of(BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8)));
            var index = new PlacedMansions();
            index.record(cellar, rooms);
            var indexCellar = PlacedMansions.load(index.save(new CompoundTag())).at(cellar.firstDoor()).get(0).basement();
            var clientCellar = MansionProtectionSnapshot.decode(new MansionProtectionSnapshot(rooms, List.of(cellar)).encode()).basements().get(0);
            for (var restored : List.of(indexCellar, clientCellar)) {
                h.assertTrue(restored.layoutVersion() == version && restored.keyAnchor().equals(cellar.keyAnchor())
                        && restored.stairRotation() == rotation && restored.decorativeWalls() == walls
                        && restored.groundsY() == cellar.groundsY() && restored.gentleRamp() == (version >= 8),
                        "Both codecs must retain saved geometry, wall variant and ramp generation");
                h.assertTrue(restored.isBuiltCell(cellar.world(5, 3, 17)) == (version >= 8), "Only V8 protects the new room torch");
                h.assertTrue(restored.isBuiltCell(cellar.world(29, 4, 67)) == (version == 7), "Saved V7 keeps its hood while V8 leaves it clear");
                h.assertTrue(restored.getBoundingBox().maxY() == cellar.mainFloorY() + (version == 8 ? 8 : 7)
                                && restored.isBuiltCell(cellar.world(0, 15, 1)) == (version == 8),
                        "The reconstructed bounds must include only the appropriate saved room ceiling");
            }
        }
        h.succeed();
    }

    private static void assertTorch(GameTestHelper h, Map<BlockPos, BlockState> blocks, MansionBasementPiece cellar,
                                    BlockPos local, Direction facing, Block backing) {
        var pos = cellar.world(local.getX(), local.getY(), local.getZ());
        assertBlock(h, blocks, pos, Blocks.WALL_TORCH, "Place the doorway or stair torch");
        Direction worldFacing = cellar.stairRotation().rotate(facing);
        h.assertTrue(blocks.get(pos).getValue(WallTorchBlock.FACING) == worldFacing, "Rotate the torch away from its supporting wall");
        assertBlock(h, blocks, pos.relative(worldFacing.getOpposite()), backing, "Every torch must have a solid backing block");
    }

    private static void assertLegacyState(GameTestHelper h, Map<BlockPos, BlockState> blocks, MansionBasementPiece cellar,
                                          int x, int y, int z, String message) {
        h.assertTrue(blocks.get(cellar.world(x, y, z)).equals(MansionBasementLayoutV7.cell(x, y, z).rotate(cellar.stairRotation())), message);
    }

    private static void assertBlock(GameTestHelper h, Map<BlockPos, BlockState> blocks, BlockPos pos, Block expected, String message) {
        h.assertTrue(blocks.containsKey(pos) && blocks.get(pos).is(expected), message + " at " + pos + ": " + blocks.get(pos));
    }
}
