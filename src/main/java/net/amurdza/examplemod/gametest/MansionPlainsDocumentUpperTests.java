package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionUpperLayout;
import net.amurdza.examplemod.worldgen.structure.MansionRoomCatalogue;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionPlainsDocumentUpperTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void interiorSingleCellFlightsAndFourEntriesSurviveEveryRotation(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var names = new IdentityHashMap<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate, String>();
        manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith("woodland_mansion/"))
                .forEach(id -> manager.get(id).ifPresent(template -> names.put(template, id.getPath().substring("woodland_mansion/".length()))));
        for (int seed = 0; seed < 8; seed++) for (var rotation : Rotation.values()) {
            var origin = new BlockPos(37, 120, -93);
            var pieces = new ArrayList<WoodlandMansionPiece>();
            MansionUpperLayout.generate(manager, origin, rotation, pieces, RandomSource.create(seed));
            var bounds = BoundingBox.encapsulatingBoxes(pieces.stream().map(WoodlandMansionPiece::getBoundingBox).toList()).orElseThrow();
            var stairs = pieces.stream().filter(p -> names.get(p.template()).equals(MansionUpperLayout.SINGLE_STAIRS)).toList();
            h.assertTrue(stairs.size() == 4, "Four independent single-cell flights must connect the five floors");
            h.assertTrue(stairs.stream().map(p -> p.templatePosition().getY()).sorted().toList().equals(List.of(120, 128, 136, 144)),
                    "One flight must begin on each of the first four floors");
            for (var stair : stairs) {
                var box = stair.getBoundingBox();
                h.assertTrue(box.getYSpan() == 16 && Math.min(box.getXSpan(), box.getZSpan()) == 7
                                && Math.max(box.getXSpan(), box.getZSpan()) == 9,
                        "Only the two shared door planes extend past the seven-by-seven interior");
                h.assertTrue(box.minX() > bounds.minX() + 7 && box.maxX() < bounds.maxX() - 7
                                && box.minZ() > bounds.minZ() + 7 && box.maxZ() < bounds.maxZ() - 7,
                        "Stairs may never border exterior windows: " + rotation);
            }
            h.assertTrue(pieces.stream().filter(p -> names.get(p.template()).equals(MansionUpperLayout.ENTRANCE)).count() == 4,
                    "Every mansion must have four matching facade entrances");
            h.assertTrue(pieces.stream().noneMatch(p -> names.get(p.template()).equals("aoe_compact_entrance_v2")
                            || names.get(p.template()).contains("1x2_d_stairs")),
                    "The old large foyer and two-cell upper stair must be absent from new generation");
            var landing = MansionUpperLayout.basementLanding(manager, pieces);
            h.assertTrue(landing.position().getY() == origin.getY() && landing.rotation() == rotation,
                    "The basement marker must describe the actual ground-floor doorway");
            var cellar = new net.amurdza.examplemod.worldgen.structure.MansionBasementPiece(landing.position(), landing.rotation());
            // Four threshold centers describe the ground-floor facade; the
            // projecting roof is deliberately excluded from this footprint.
            var thresholds = pieces.stream().filter(p -> names.get(p.template()).equals(MansionUpperLayout.ENTRANCE))
                    .map(p -> net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                            .calculateRelativePosition(p.placeSettings(), new BlockPos(3, 0, 0)).offset(p.templatePosition())).toList();
            var facade = BoundingBox.encapsulatingPositions(thresholds).orElseThrow();
            var cellarBounds = cellar.getBoundingBox();
            for (int x = cellarBounds.minX(); x <= cellarBounds.maxX(); x++)
                for (int z = cellarBounds.minZ(); z <= cellarBounds.maxZ(); z++) {
                    var ceiling = new BlockPos(x, origin.getY() - 1, z);
                    h.assertTrue(!cellar.containsAuthoredSpace(ceiling) || facade.isInside(ceiling.above()),
                            "The raised cellar ceiling must stay under the ground-floor facade, not its roof overhang: "
                                    + rotation + "/" + ceiling + "/" + facade);
                }
            var halls = pieces.stream().filter(p -> MansionRoomCatalogue.nativeName(names.get(p.template())).equals("corridor_floor")).toList();
            for (var room : pieces) {
                String name = MansionRoomCatalogue.nativeName(names.get(room.template()));
                if (!name.startsWith("1x") && !name.startsWith("2x")) continue;
                h.assertTrue(halls.stream().anyMatch(hall -> hall.templatePosition().getY() == room.templatePosition().getY()
                                && adjacent(room.getBoundingBox(), hall.getBoundingBox())),
                        "Every room must directly border a hallway: " + seed + "/" + rotation + "/" + name + "/" + room.getBoundingBox());
            }
            var ceilings = pieces.stream().filter(p -> names.get(p.template()).equals(MansionRoomCatalogue.newTemplate("ceiling"))).toList();
            h.assertTrue(ceilings.size() == 121 && ceilings.stream().allMatch(p -> p.templatePosition().getY() == origin.getY() + 40),
                    "A solid two-layer ceiling must cover every cell of the eleven-by-eleven fifth floor");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bedroomCanopyIsRemovedAndThirdCeilingHasCorrectMaterials(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var bedroom = manager.getOrCreate(new ResourceLocation("minecraft:woodland_mansion/aoe_compact_1x2_d1_v3")).save(new CompoundTag());
        var palette = bedroom.getList("palette", Tag.TAG_COMPOUND);
        for (var entry : bedroom.getList("blocks", Tag.TAG_COMPOUND)) {
            var cell = (CompoundTag) entry;
            if (cell.getList("pos", Tag.TAG_INT).getInt(1) < 3) continue;
            String name = palette.getCompound(cell.getInt("state")).getString("Name");
            h.assertTrue(!List.of("minecraft:oak_planks", "minecraft:oak_slab", "minecraft:oak_fence").contains(name),
                    "The bed canopy must no longer intercept the chandelier");
        }
        var ceiling = manager.getOrCreate(new ResourceLocation("minecraft:woodland_mansion/aoe_compact_ceiling_v3")).save(new CompoundTag());
        palette = ceiling.getList("palette", Tag.TAG_COMPOUND);
        h.assertTrue(ceiling.getList("blocks", Tag.TAG_COMPOUND).size() == 128, "The ceiling layers must be solid, not sparse edge strips");
        for (var entry : ceiling.getList("blocks", Tag.TAG_COMPOUND)) {
            var cell = (CompoundTag) entry;
            int y = cell.getList("pos", Tag.TAG_INT).getInt(1);
            h.assertTrue(palette.getCompound(cell.getInt("state")).getString("Name").equals(y == 0 ? "minecraft:birch_planks" : "minecraft:oak_planks"),
                    "The solid birch ceiling must sit below the full oak layer");
        }
        h.succeed();
    }

    private static boolean adjacent(BoundingBox a, BoundingBox b) {
        int xGap = Math.max(a.minX() - b.maxX(), b.minX() - a.maxX());
        int zGap = Math.max(a.minZ() - b.maxZ(), b.minZ() - a.maxZ());
        int xOverlap = Math.min(a.maxX(), b.maxX()) - Math.max(a.minX(), b.minX());
        int zOverlap = Math.min(a.maxZ(), b.maxZ()) - Math.max(a.minZ(), b.minZ());
        return xGap >= 0 && xGap <= 3 && zOverlap >= 2 || zGap >= 0 && zGap <= 3 && xOverlap >= 2;
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void cornerPillarsRemainSymmetricAfterRealPlacementAndNeighborUpdates(GameTestHelper h) {
        var level = h.getLevel();
        var manager = level.getStructureManager();
        int variant = 0;
        for (var rotation : Rotation.values()) {
            var origin = h.absolutePos(new BlockPos(48 + 32 * variant++, 0, 64)).atY(180);
            for (int floor = 0; floor < 3; floor++) {
                var piece = new WoodlandMansionPiece(manager, "aoe_compact_wall_corner_v3", origin.above(floor * 8), rotation);
                var bounds = piece.getBoundingBox();
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0),
                        bounds, new net.minecraft.world.level.ChunkPos(origin), origin);
            }
            // Recompute top-to-bottom just as placing the last neighboring
            // roof/beam would, after all three clipped templates are present.
            for (int y = 23; y >= 1; y--) {
                if (y % 8 == 0) continue;
                var point = new BlockPos(8, y, 1).rotate(rotation).offset(origin);
                var state = level.getBlockState(point);
                h.assertTrue(state.is(net.minecraft.world.level.block.Blocks.COBBLESTONE_WALL), "The corner's wall pillar must remain present");
                var updated = net.minecraft.world.level.block.Block.updateFromNeighbourShapes(state, level, point);
                level.setBlock(point, updated, 18);
                var north = wallSide(rotation.rotate(net.minecraft.core.Direction.NORTH));
                var west = wallSide(rotation.rotate(net.minecraft.core.Direction.WEST));
                h.assertTrue(updated.getValue(north) == net.minecraft.world.level.block.state.properties.WallSide.TALL
                                && updated.getValue(west) == net.minecraft.world.level.block.state.properties.WallSide.TALL,
                        "Both inward pillar arms must stay equally tall after native neighbor recomputation at " + point + ": " + updated);
            }
        }
        h.succeed();
    }

    private static net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.world.level.block.state.properties.WallSide> wallSide(
            net.minecraft.core.Direction direction) {
        return switch (direction) {
            case NORTH -> net.minecraft.world.level.block.WallBlock.NORTH_WALL;
            case SOUTH -> net.minecraft.world.level.block.WallBlock.SOUTH_WALL;
            case EAST -> net.minecraft.world.level.block.WallBlock.EAST_WALL;
            default -> net.minecraft.world.level.block.WallBlock.WEST_WALL;
        };
    }
}
