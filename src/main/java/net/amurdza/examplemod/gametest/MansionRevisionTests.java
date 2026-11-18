package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionRevisionTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void secretRoomsGainWalkableConnectionsAfterRotationAndReload(GameTestHelper h) {
        var level = h.getLevel();
        var context = StructurePieceSerializationContext.fromLevel(level);
        int variant = 0;
        for (Rotation rotation : Rotation.values()) {
            var origin = h.absolutePos(new BlockPos(80 + 48 * variant++, 0, 40)).atY(185);
            var hall = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", origin, rotation);
            var secret = new WoodlandMansionPiece(level.getStructureManager(), "1x1_as1",
                    origin.offset(new BlockPos(8, 0, 0).rotate(rotation)), rotation, Mirror.NONE);
            var wall = new WoodlandMansionPiece(level.getStructureManager(), "indoors_wall_1",
                    origin.offset(new BlockPos(7, 0, 0).rotate(rotation)), rotation);
            var pieces = List.of(hall, secret, wall);
            var passages = MansionRoomAccess.plan(level.getStructureManager(), pieces);
            h.assertTrue(passages.stream().anyMatch(MansionEnvelopePiece.Passage::clear), "A closed secret room needs a real doorway: " + rotation);
            var bounds = BoundingBox.encapsulatingBoxes(pieces.stream().map(p -> p.getBoundingBox()).toList()).orElseThrow();
            var clear = new MansionEnvelopePiece(bounds, true, List.of());
            var repair = new MansionEnvelopePiece(bounds, false, passages);
            repair = new MansionEnvelopePiece(context, repair.createTag(context));
            for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                var chunk = new ChunkPos(x, z);
                var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                clear.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), clip, chunk, origin);
                for (var piece : pieces) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), clip, chunk, origin);
                repair.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), clip, chunk, origin);
            }
            for (var passage : passages) if (passage.clear()) {
                var box = passage.box();
                // Carpet is intentional walking surface, not a blocked doorway.
                // Check supported feet and headroom rather than requiring bare air.
                for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY() + 1, box.minZ(), box.maxX(), box.minY() + 1, box.maxZ()))
                    h.assertTrue(canWalk(level, pos), "A repaired rotated doorway is still blocked at " + pos
                            + " / " + rotation + ": feet=" + level.getBlockState(pos)
                            + ", head=" + level.getBlockState(pos.above()) + ", floor=" + level.getBlockState(pos.below()));
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void circularEstateLeavesCornersNaturalAndSupportsTheWholeCellar(GameTestHelper h) {
        var level = h.getLevel();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod", "illager_mansion"));
        var origin = h.absolutePos(new BlockPos(96, 0, 160)).atY(155);
        var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", origin, Rotation.NONE);
        var cellar = new MansionBasementPiece(origin, Rotation.NONE, 8);
        var pieces = new PiecesContainer(List.of(room, cellar));
        var corner = new BlockPos(room.getBoundingBox().maxX() + 64, origin.getY() + 10, room.getBoundingBox().maxZ() + 64);
        level.setBlock(corner, Blocks.DIRT.defaultBlockState(), 18);
        var chunk = new ChunkPos(corner);
        structure.afterPlace(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(5),
                new BoundingBox(corner.getX(), level.getMinBuildHeight(), corner.getZ(), corner.getX(), level.getMaxBuildHeight() - 1, corner.getZ()), chunk, pieces);
        h.assertTrue(level.getBlockState(corner).is(Blocks.DIRT), "A circle must retain the old square's far corners as natural terrain");
        var floor = cellar.world(0, -2, 20);
        var clip = new BoundingBox(floor.getX(), level.getMinBuildHeight(), floor.getZ(), floor.getX(), level.getMaxBuildHeight() - 1, floor.getZ());
        chunk = new ChunkPos(floor);
        for (int y = floor.getY() - 8; y < floor.getY(); y++) level.setBlock(floor.atY(y), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(floor.below(4), Blocks.STONE.defaultBlockState(), 18);
        level.setBlock(floor.below(9), Blocks.STONE.defaultBlockState(), 18);
        cellar.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(5), clip, chunk, origin);
        structure.afterPlace(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(5), clip, chunk, pieces);
        for (int y = floor.getY() - 8; y < floor.getY(); y++)
            h.assertTrue(level.getBlockState(floor.atY(y)).is(Blocks.STONE),
                    "The hollow cellar foundation below a solid ledge remains at " + floor.atY(y));
        h.assertTrue(level.isEmptyBlock(cellar.world(0, 1, 20)), "Foundation filling must preserve the basement hallway");
        h.succeed();
    }


    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void naturalAndCommandBasementsExcludeWildMushroomsThroughBuffers(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.PLAINS);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:illager_mansion"));
        for (boolean natural : new boolean[]{true, false}) {
            var entrance = new BlockPos(1848 + (natural ? 0 : 128), 160, 1848);
            var cellar = new MansionBasementPiece(entrance);
            var root = cellar.world(0, 1, 20);
            // A dark podzol cave is otherwise eligible for both mushroom routes.
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                level.setBlock(root.offset(x, -1, z), Blocks.PODZOL.defaultBlockState(), 18);
                for (int y = 0; y <= 3; y++) level.setBlock(root.offset(x, y, z), Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(root.offset(x, 4, z), Blocks.STONE.defaultBlockState(), 18);
            }
            var outside = new BlockPos(root.getX(), root.getY(), cellar.getBoundingBox().maxZ() + 1);
            if (natural) {
                var owner = new net.minecraft.world.level.ChunkPos(entrance);
                var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, owner, 0, new PiecesContainer(List.of(cellar)));
                level.getChunk(owner.x, owner.z).setStartForStructure(structure, start);
                var target = new net.minecraft.world.level.ChunkPos(root);
                level.getChunk(target.x, target.z).addReferenceForStructure(structure, owner.toLong());
            } else PlacedMansions.get(level).record(cellar, List.of(BoundingBox.fromCorners(entrance, entrance.offset(6, 7, 6))));
            var buffered = new net.amurdza.examplemod.worldgen.GrowthPlacementBuffer(new net.amurdza.examplemod.worldgen.GrowthPlacementBuffer(level));
            for (net.minecraft.world.level.WorldGenLevel source : List.of(level, buffered)) {
                h.assertTrue(net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludes(source, root)
                                && !net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludes(source, outside),
                        "Only authored basement space is excluded: natural=" + natural + ", buffered=" + (source == buffered));
                h.assertTrue(net.amurdza.examplemod.worldgen.PlainsCaveVegetation.placePatch(source, root, Blocks.RED_MUSHROOM, RandomSource.create(0)) == 0,
                        "Cave mushroom patches must not populate basement air");
                for (String name : List.of("minecraft:red_mushroom", "minecraft:brown_mushroom", "farmersdelight:wild_cabbages"))
                    h.assertFalse(net.amurdza.examplemod.worldgen.DocumentedFloraFeature.placePlant(source, root, name, RandomSource.create(0)),
                            "Scattered natural flora must respect the same exclusion: " + name);
            }
            h.assertTrue(level.isEmptyBlock(root), "Rejected mushroom placement changed the authored basement");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void earlierBasementsKeepTheirSavedBlueprintAndKeyAnchor(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(8, 0, 8)).atY(100);
        var before = new MansionBasementPiece(origin, Rotation.COUNTERCLOCKWISE_90, 1);
        var context = StructurePieceSerializationContext.fromLevel(h.getLevel());
        var tag = before.createTag(context);
        tag.remove("LayoutVersion");
        var restored = (MansionBasementPiece) MansionBasementPiece.load(context, tag);
        h.assertTrue(restored.layoutVersion() == 1 && restored.keyAnchor().equals(before.keyAnchor())
                        && restored.isDoor(before.firstDoor()) && restored.isBuiltCell(before.world(-31, -10, 26)),
                "Previously saved vertical shafts and locks must keep their original positions");
        var manual = new PlacedMansions();
        manual.record(before, List.of(BoundingBox.fromCorners(origin, origin.offset(6, 7, 6))));
        var saved = PlacedMansions.load(manual.save(new CompoundTag()));
        h.assertTrue(saved.at(before.firstDoor()).get(0).basement().layoutVersion() == 1, "Manual saved basements must retain their blueprint version");
        h.succeed();
    }
    @GameTest(template = "plant_gameplay_empty")
    public static void upperLandingRepairsKeepAscendingStairHeadroom(GameTestHelper h) {
        var level = h.getLevel();
        var stair = h.absolutePos(new BlockPos(12, 3, 12));
        level.setBlock(stair, Blocks.BIRCH_STAIRS.defaultBlockState(), 18);
        level.setBlock(stair.above(), Blocks.AIR.defaultBlockState(), 18);
        var gap = stair.above().east();
        level.setBlock(gap, Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(gap.below(), Blocks.AIR.defaultBlockState(), 18);
        var passage = BoundingBox.fromCorners(stair.above(), gap.above(3));
        var repair = new MansionEnvelopePiece(passage, false, List.of(new MansionEnvelopePiece.Passage(passage, true)));
        var chunk = new ChunkPos(stair);
        repair.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), passage, chunk, stair);
        h.assertTrue(level.isEmptyBlock(stair.above()) && level.getBlockState(stair).is(Blocks.BIRCH_STAIRS),
                "The third-floor doorway must retain headroom above its ascending landing stair");
        h.assertTrue(level.getBlockState(gap).is(Blocks.BIRCH_PLANKS), "A genuine adjacent deck hole still needs its missing floor tile");
        var tread = stair.east(4);
        level.setBlock(tread, Blocks.COBBLESTONE_STAIRS.defaultBlockState(), 18);
        var doorway = BoundingBox.fromCorners(tread.below(), tread.above(2));
        new MansionEnvelopePiece(doorway, false, List.of(new MansionEnvelopePiece.Passage(doorway, true)))
                .postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), doorway, new ChunkPos(tread), tread);
        h.assertTrue(level.getBlockState(tread).is(Blocks.COBBLESTONE_STAIRS), "A new doorway must retain the lower turning treads it crosses");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bothNativeUpperStairTemplatesRetainTheirTurningTreads(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.PLAINS);
        for (String name : List.of("1x2_c_stairs", "1x2_d_stairs")) {
            var template = level.getStructureManager().getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + name));
            var data = template.save(new CompoundTag());
            var palette = data.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
            var states = new java.util.ArrayList<net.minecraft.world.level.block.state.BlockState>();
            for (var entry : palette) states.add(net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) entry));
            var original = new java.util.ArrayList<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo>();
            for (var entry : data.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var block = (CompoundTag) entry;
                var pos = block.getList("pos", net.minecraft.nbt.Tag.TAG_INT);
                original.add(new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                        new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), states.get(block.getInt("state")), block.contains("nbt") ? block.getCompound("nbt") : null));
            }
            var treads = name.equals("1x2_c_stairs") ? List.of(new BlockPos(6, 1, 3), new BlockPos(6, 1, 4), new BlockPos(5, 2, 2), new BlockPos(6, 2, 2))
                    : List.of(new BlockPos(2, 1, 13), new BlockPos(3, 1, 13), new BlockPos(4, 1, 13), new BlockPos(3, 9, 5));
            for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
                var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(rotation).setMirror(mirror)
                        .addProcessor(IllagerMansionProcessor.INSTANCE);
                var processed = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.processBlockInfos(level, BlockPos.ZERO, BlockPos.ZERO, settings, original, template);
                var byPosition = new java.util.HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
                processed.forEach(info -> byPosition.put(info.pos(), info.state()));
                for (var tread : treads) {
                    var target = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings, tread);
                    h.assertTrue(byPosition.get(target).is(Blocks.COBBLESTONE_STAIRS), "Native functional tread changed: " + name + "/" + mirror + "/" + rotation + "/" + tread);
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 240)
    public static void stairDoorHeaderKeepsSurfaceWaterOutAfterFluidTicks(GameTestHelper h) {
        var level = h.getLevel();
        var cellar = new MansionBasementPiece(h.absolutePos(new BlockPos(512, 0, 512)).atY(110), Rotation.NONE, 3);
        var low = cellar.world(-5, -2, 7);
        var high = cellar.world(5, 8, 17);
        var clip = BoundingBox.fromCorners(low, high);
        var forced = new java.util.ArrayList<ChunkPos>();
        for (int cx = clip.minX() >> 4; cx <= clip.maxX() >> 4; cx++) for (int cz = clip.minZ() >> 4; cz <= clip.maxZ() >> 4; cz++) {
            var chunk = new ChunkPos(cx, cz);
            if (!level.getForcedChunks().contains(chunk.toLong())) {
                level.setChunkForced(cx, cz, true);
                forced.add(chunk);
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(low, high)) {
            boolean edge = pos.getX() == low.getX() || pos.getX() == high.getX() || pos.getZ() == low.getZ()
                    || pos.getZ() == high.getZ() || pos.getY() == low.getY() || pos.getY() == high.getY();
            level.setBlock(pos, (edge ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
        }
        for (int cx = clip.minX() >> 4; cx <= clip.maxX() >> 4; cx++) for (int cz = clip.minZ() >> 4; cz <= clip.maxZ() >> 4; cz++) {
            var chunk = new ChunkPos(cx, cz);
            var chunkClip = new BoundingBox(Math.max(clip.minX(), chunk.getMinBlockX()), clip.minY(), Math.max(clip.minZ(), chunk.getMinBlockZ()),
                    Math.min(clip.maxX(), chunk.getMaxBlockX()), clip.maxY(), Math.min(clip.maxZ(), chunk.getMaxBlockZ()));
            cellar.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), chunkClip, chunk, cellar.keyAnchor());
        }
        var source = cellar.world(0, 5, 12);
        level.setBlock(source, Blocks.WATER.defaultBlockState(), 3);
        level.scheduleTick(source, net.minecraft.world.level.material.Fluids.WATER, 5);
        // Natural lake water also touches both front corners of the wider
        // landing, outside the last narrow tread's side walls.
        for (int x : new int[]{-3, 3}) for (int y = 1; y <= 3; y++) {
            var sideSource = cellar.world(x, y, 8);
            level.setBlock(sideSource, Blocks.WATER.defaultBlockState(), 3);
            level.scheduleTick(sideSource, net.minecraft.world.level.material.Fluids.WATER, 5);
        }
        h.runAfterDelay(80, () -> {
            try {
                var flowingControl = level.getBlockState(cellar.world(0, 5, 11));
                h.assertTrue(flowingControl.is(Blocks.WATER) && flowingControl.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) > 0,
                        "The forced fixture must execute real fluid ticks before checking the door seam");
                for (int x = -3; x <= 3; x++) for (int z = 10; z <= 12; z++) for (int y = 1; y <= 3; y++) {
                    var pos = cellar.world(x, y, z);
                    h.assertTrue(level.getFluidState(pos).isEmpty(), "Lake water entered a widened stair-landing shoulder at " + pos);
                }
                for (int x = -2; x <= 1; x++) for (int z = 14; z <= 16; z++) for (int y = 1; y <= 5; y++) {
                    var pos = cellar.world(x, y, z);
                    h.assertTrue(level.getFluidState(pos).isEmpty(), "Surface water leaked over the stair door into the basement at " + pos);
                }
                h.succeed();
            } finally {
                for (var chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            }
        });
    }

    // Current full-estate placement, connectivity and roster coverage lives in MansionExpandedEstateTests.

    private static BlockPos local(WoodlandMansionPiece piece, int x, int y, int z) {
        return net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                .calculateRelativePosition(piece.placeSettings(), new BlockPos(x, y, z)).offset(piece.templatePosition());
    }

    private static boolean directHallway(net.minecraft.server.level.ServerLevel level, WoodlandMansionPiece room, WoodlandMansionPiece hall) {
        if (room.templatePosition().getY() != hall.templatePosition().getY()) return false;
        var a = room.getBoundingBox();
        var b = hall.getBoundingBox();
        int lo, hi, first, last;
        boolean xAxis;
        if (a.maxX() <= b.minX() && b.minX() - a.maxX() <= 3) { lo = a.maxX() - 1; hi = b.minX() + 1; xAxis = true; }
        else if (b.maxX() <= a.minX() && a.minX() - b.maxX() <= 3) { lo = b.maxX() - 1; hi = a.minX() + 1; xAxis = true; }
        else if (a.maxZ() <= b.minZ() && b.minZ() - a.maxZ() <= 3) { lo = a.maxZ() - 1; hi = b.minZ() + 1; xAxis = false; }
        else if (b.maxZ() <= a.minZ() && a.minZ() - b.maxZ() <= 3) { lo = b.maxZ() - 1; hi = a.minZ() + 1; xAxis = false; }
        else return false;
        first = xAxis ? Math.max(a.minZ(), b.minZ()) : Math.max(a.minX(), b.minX());
        last = xAxis ? Math.min(a.maxZ(), b.maxZ()) : Math.min(a.maxX(), b.maxX());
        int y = room.templatePosition().getY() + 1;
        for (int center = first + 1; center < last; center++) {
            boolean open = true;
            for (int along = lo; along <= hi; along++)
                open &= canWalk(level, xAxis ? new BlockPos(along, y, center) : new BlockPos(center, y, along));
            if (open) return true;
        }
        return false;
    }

    private static java.util.Map<String, Object> view(net.minecraft.server.level.ServerLevel level, String name, double x, double y, double z, float yaw, float pitch) {
        return java.util.Map.of("label", name, "dimension", level.dimension().location().toString(), "x", x, "y", y, "z", z, "yaw", yaw, "pitch", pitch);
    }

    private static boolean canWalk(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        var feet = level.getBlockState(pos);
        var head = level.getBlockState(pos.above());
        var below = level.getBlockState(pos.below());
        boolean stairs = feet.getBlock() instanceof net.minecraft.world.level.block.StairBlock;
        boolean open = feet.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                || feet.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock || stairs
                || feet.getCollisionShape(level, pos).isEmpty() || feet.is(net.minecraft.tags.BlockTags.WOOL_CARPETS);
        return open && (head.getCollisionShape(level, pos.above()).isEmpty() || head.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)
                && (stairs || !below.getCollisionShape(level, pos.below()).isEmpty());
    }

    private static java.util.Set<BlockPos> walk(net.minecraft.server.level.ServerLevel level, BoundingBox bounds, BlockPos start) {
        var visited = new java.util.HashSet<BlockPos>();
        var queue = new java.util.ArrayDeque<BlockPos>();
        visited.add(start); queue.add(start);
        while (!queue.isEmpty()) {
            var from = queue.remove();
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                var to = from.relative(direction).above(dy);
                if (!bounds.isInside(to) || visited.contains(to) || !canWalk(level, to)) continue;
                var clearance = dy > 0 ? from.above(2) : dy < 0 ? to.above(2) : null;
                if (clearance != null && !level.getBlockState(clearance).getCollisionShape(level, clearance).isEmpty()) continue;
                visited.add(to); queue.add(to);
            }
        }
        return visited;
    }

}
