package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionProcessor;
import net.amurdza.examplemod.worldgen.structure.MansionLibraryLayout;
import net.amurdza.examplemod.worldgen.structure.MansionResidentialPlan;
import net.amurdza.examplemod.worldgen.structure.MansionRoomAccess;
import net.amurdza.examplemod.worldgen.structure.MansionRoomCatalogue;
import net.amurdza.examplemod.worldgen.structure.MansionUpperLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionAuthoredLibraryTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void exactlyOneInteriorLibraryOnEveryFloorKeepsItsOwnDoor(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var template = manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionLibraryLayout.LIBRARY));
        var frame = manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionLibraryLayout.FRAME));
        var oldLibrary = manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.newTemplate("1x2_c4")));
        var origin = new BlockPos(0, 140, 0);
        for (int seed = 0; seed < 4; seed++) for (var rotation : Rotation.values()) {
            var pieces = new ArrayList<WoodlandMansionPiece>();
            MansionUpperLayout.generate(manager, origin, rotation, pieces, RandomSource.create(seed));
            var libraries = pieces.stream().filter(p -> p.template() == template).toList();
            h.assertTrue(libraries.size() == 5, "Exactly one authored library is required on each of five floors");
            h.assertTrue(pieces.stream().noneMatch(p -> p.template() == oldLibrary), "Random room selection must not add another library");
            h.assertTrue(pieces.stream().filter(p -> p.template() == frame).count() == 5, "Each library needs its complete indoor frame");
            h.assertTrue(MansionResidentialPlan.bedCapacity(manager, pieces) >= 180, "Reserving libraries must retain 180 sleeping places");
            var floors = new HashSet<Integer>();
            var composed = new HashMap<BlockPos, BlockState>();
            var areas = pieces.stream().filter(p -> p.template() == frame).map(WoodlandMansionPiece::getBoundingBox).toList();
            for (var piece : pieces) if (areas.stream().anyMatch(b -> b.intersects(piece.getBoundingBox())))
                for (var cell : cells(piece.template()).entrySet())
                    composed.put(world(piece, cell.getKey()), cell.getValue().mirror(piece.placeSettings().getMirror()).rotate(piece.getRotation()));
            var passages = MansionRoomAccess.plan(manager, pieces);
            for (var library : libraries) {
                floors.add(library.templatePosition().getY());
                var expectedOrigin = new BlockPos(-32, library.templatePosition().getY() - origin.getY(), -8).rotate(rotation).offset(origin);
                h.assertTrue(library.templatePosition().equals(expectedOrigin), "Library must stay in the fully interior reserved cells");
                var bounds = library.getBoundingBox();
                h.assertTrue(MansionResidentialPlan.roomBounds(manager, pieces).contains(bounds), "Library must participate in room access and resident planning");
                for (var cell : cells(template).entrySet()) {
                    var actual = composed.get(world(library, cell.getKey()));
                    h.assertTrue(cell.getValue().rotate(rotation).equals(actual), "Later pieces must preserve the authored library at " + cell.getKey());
                }
                for (int y = 1; y <= 3; y++) {
                    for (int z = -1; z <= 15; z++) for (int x : new int[]{-1, 7})
                        h.assertTrue(!composed.get(world(library, new BlockPos(x, y, z))).isAir(), "Library side walls must remain closed");
                    for (int x = 0; x <= 6; x++) {
                        h.assertTrue(!composed.get(world(library, new BlockPos(x, y, -1))).isAir(), "Library northern partition must be closed");
                        h.assertTrue(composed.get(world(library, new BlockPos(x, y, 15))).isAir() == (x >= 2 && x <= 4),
                                "The only library doorway must remain centered on the authored short end");
                    }
                }
                var door = world(library, new BlockPos(3, 1, 15));
                h.assertTrue(passages.stream().anyMatch(p -> !p.clear() && p.box().isInside(door)), "Library's authored door must connect directly to a corridor");
                h.assertTrue(passages.stream().noneMatch(p -> p.clear() && p.box().intersects(bounds)), "Access repair must not invent another library door");
            }
            h.assertTrue(floors.equals(Set.of(140, 148, 156, 164, 172)), "One library must occupy each distinct floor");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void authoredFurnitureLightingAndDoorSurvivePlacementAndReload(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var manager = level.getStructureManager();
        var serial = StructurePieceSerializationContext.fromLevel(level);
        for (var rotation : Rotation.values()) {
            var origin = new BlockPos(3800 + rotation.ordinal() * 32, 210, 3800);
            var room = new WoodlandMansionPiece(manager, MansionLibraryLayout.LIBRARY, origin, rotation);
            var frame = new WoodlandMansionPiece(manager, MansionLibraryLayout.FRAME,
                    origin.offset(new BlockPos(-1, 0, -1).rotate(rotation)), rotation);
            h.assertTrue(cells(room.template()).size() == 840, "All 7 x 8 x 15 authored cells must be present");
            h.assertTrue(room.template().filterBlocks(BlockPos.ZERO, room.placeSettings(), Blocks.BOOKSHELF).size() == 92,
                    "The player's enlarged shelves must be preserved");
            h.assertTrue(room.template().filterBlocks(BlockPos.ZERO, room.placeSettings(), Blocks.OAK_FENCE).size() == 18
                            && room.template().filterBlocks(BlockPos.ZERO, room.placeSettings(), Blocks.TORCH).size() == 10,
                    "Keep all authored lighting and add exactly two long-wall fence and torch pairs");
            var authoredCells = cells(room.template());
            for (int x : new int[]{0, 6}) {
                var fence = authoredCells.get(new BlockPos(x, 3, 7));
                h.assertTrue(fence.is(Blocks.OAK_FENCE)
                                && fence.getValue(x == 0 ? net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST
                                        : net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST)
                                && authoredCells.get(new BlockPos(x, 4, 7)).is(Blocks.TORCH),
                        "The two requested brackets occupy only the formerly empty long-wall midpoint cells");
            }
            for (var fresh : List.of(frame, room)) {
                h.assertTrue(!fresh.placeSettings().getProcessors().contains(IllagerMansionProcessor.INSTANCE), "Generic furniture repair must leave the authored room untouched");
                var restored = new WoodlandMansionPiece(manager, fresh.createTag(serial));
                h.assertTrue(!restored.placeSettings().getProcessors().contains(IllagerMansionProcessor.INSTANCE), "Authored preservation must survive saving and reloading");
                h.assertTrue(restored.template().placeInWorld(level, restored.templatePosition(), restored.templatePosition(),
                        restored.placeSettings(), RandomSource.create(0), 18), "Authored template must actually place");
            }
            for (var piece : List.of(frame, room)) for (var cell : cells(piece.template()).entrySet()) {
                var pos = world(piece, cell.getKey());
                h.assertTrue(level.getBlockState(pos).equals(cell.getValue().rotate(rotation)),
                        "Authored block state changed during real placement at " + pos + ": " + level.getBlockState(pos));
            }
        }
        h.succeed();
    }

    private static BlockPos world(WoodlandMansionPiece piece, BlockPos local) {
        return StructureTemplate.calculateRelativePosition(piece.placeSettings(), local).offset(piece.templatePosition());
    }

    private static Map<BlockPos, BlockState> cells(StructureTemplate template) {
        var data = template.save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var cells = new HashMap<BlockPos, BlockState>();
        for (var value : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) value;
            var pos = block.getList("pos", Tag.TAG_INT);
            cells.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(block.getInt("state"))));
        }
        return cells;
    }
}
