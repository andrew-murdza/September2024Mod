package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
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
public final class MansionNovember15UpperTests {
    private MansionNovember15UpperTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void sharedCatalogueFitsFiveCompleteNativeFloors(GameTestHelper helper) throws Exception {
        var manager = helper.getLevel().getStructureManager();
        Map<StructureTemplate, String> names = new HashMap<>();
        manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith("woodland_mansion/"))
                .forEach(id -> manager.get(id).ifPresent(t -> names.put(t, id.getPath().substring("woodland_mansion/".length()))));
        var seenByFloor = java.util.stream.IntStream.range(0, 5).mapToObj(floor -> new HashSet<String>()).toList();
        var witnesses = new java.util.TreeMap<String, Integer>();
        var sampleCounts = new java.util.TreeMap<String, Integer>();
        for (int seed = 0; seed < 16; seed++) for (Rotation rotation : Rotation.values()) {
            var origin = new BlockPos(-127, 100, 191);
            var pieces = new ArrayList<WoodlandMansionPiece>();
            MansionUpperLayout.generate(manager, origin, rotation, pieces, RandomSource.create(seed));
            var floors = new HashSet<Integer>();
            var topCoverage = new HashSet<Long>();
            var roofCoverage = new HashSet<Long>();
            int stairs = 0;
            for (var piece : pieces) {
                String name = MansionRoomCatalogue.nativeName(names.get(piece.template()));
                int y = piece.templatePosition().getY() - origin.getY();
                if (name.equals("corridor_floor")) floors.add(y);
                if (piece.getBoundingBox().minY() <= origin.getY() + 32 && piece.getBoundingBox().maxY() >= origin.getY() + 32)
                    addColumns(topCoverage, piece.getBoundingBox());
                if (name.equals("roof")) addColumns(roofCoverage, piece.getBoundingBox());
                if (names.get(piece.template()).equals(MansionUpperLayout.SINGLE_STAIRS)) {
                    stairs++;
                    helper.assertTrue(y >= 0 && y <= 24 && y % 8 == 0, "The four one-cell flights must link consecutive storeys");
                    continue;
                }
                if (!name.startsWith("1x") && !name.startsWith("2x")) continue;
                helper.assertTrue(MansionRoomCatalogue.NORMAL.contains(name) || name.equals(MansionRoomCatalogue.FALLBACK)
                                || name.equals(MansionRoomCatalogue.STAIRS), "Unselected mansion room remains: " + name);
                helper.assertTrue(y >= 0 && y <= 32 && y % 8 == 0, "All catalogue rooms need eight-block storeys: " + name + "/" + y);
                if (name.equals(MansionRoomCatalogue.STAIRS)) {
                    stairs++;
                    helper.assertTrue(y == 8, "The straight native stair must connect the second and third floor");
                } else {
                    helper.assertTrue(piece.template().getSize().getY() <= 8, "Catalogue room intersects the next floor: " + name);
                    seenByFloor.get(y / 8).add(name);
                    witnesses.putIfAbsent((y / 8 + 1) + ":" + name, seed);
                    sampleCounts.merge((y / 8 + 1) + ":" + name, 1, Integer::sum);
                }
                if (name.equals("1x1_a1") || name.equals("1x1_a5"))
                    helper.assertTrue(y == 0, "Heavy flower rooms must stay on the ground floor");
            }
            helper.assertTrue(floors.equals(Set.of(0, 8, 16, 24, 32)) && stairs == 4,
                    "Every layout needs five complete floors and exactly four one-cell stairs: " + floors + "/" + stairs);
            helper.assertTrue(roofCoverage.size() == 94 * 94,
                    "The roof must cover eleven eight-block cells plus the three-block extension on both sides: " + roofCoverage.size());
            helper.assertTrue(pieces.stream().filter(p -> MansionResidentialPlan.isMapRoom(manager, p)).count() == 1,
                    "Bed allocation must retain exactly one map room for the lord");
            helper.assertTrue(pieces.stream().filter(p -> p.templatePosition().getY() == origin.getY())
                    .anyMatch(p -> MansionRoomCatalogue.nativeName(names.get(p.template())).equals("wall_window")),
                    "Ground-floor outer walls must use the same windows as the upper floors");
            int covered = 0;
            for (var piece : pieces) {
                String name = MansionRoomCatalogue.nativeName(names.get(piece.template()));
                if (piece.templatePosition().getY() != origin.getY()
                        || !(name.startsWith("1x") || name.startsWith("2x") || name.equals("corridor_floor"))) continue;
                var box = piece.getBoundingBox();
                for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    long key = BlockPos.asLong(x, 0, z);
                    helper.assertTrue(topCoverage.contains(key) && roofCoverage.contains(key),
                            "Fifth floor/roof must cover the original room footprint: seed=" + seed + " / " + rotation + " / " + x + "," + z);
                    covered++;
                }
            }
            helper.assertTrue(covered > 1000, "Audit a real mansion footprint, not a token upper room");
        }
        // The fixed square has fewer ordinary one-cell rooms than the old
        // irregular layout. Probe additional native seeds, without repeated
        // rotations or placement, to prove the two ground-only flowers occur.
        for (int seed = 16; seed < 256 && (!seenByFloor.get(0).contains("1x1_a1") || !seenByFloor.get(0).contains("1x1_a5")); seed++) {
            var origin = new BlockPos(-127, 100, 191);
            var pieces = new ArrayList<WoodlandMansionPiece>();
            MansionUpperLayout.generate(manager, origin, Rotation.NONE, pieces, RandomSource.create(seed));
            for (var piece : pieces) {
                String name = MansionRoomCatalogue.nativeName(names.get(piece.template()));
                if (!MansionRoomCatalogue.NORMAL.contains(name)) continue;
                int floor = (piece.templatePosition().getY() - origin.getY()) / 8;
                seenByFloor.get(floor).add(name);
                witnesses.putIfAbsent((floor + 1) + ":" + name, seed);
                sampleCounts.merge((floor + 1) + ":" + name, 1, Integer::sum);
            }
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("mansion-catalogue-witnesses.json"),
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(
                        java.util.Map.of("firstNativeSeed", witnesses, "sampleCounts", sampleCounts)));
        // Bed allocation deliberately replaces some room choices, and the
        // single map room is reserved upstairs rather than repeated per floor.
        for (int floor = 0; floor < 5; floor++) {
            helper.assertTrue(!seenByFloor.get(floor).isEmpty(), "Every storey needs furnished catalogue rooms");
            helper.assertTrue(seenByFloor.get(floor).contains("2x2_b4") == (floor == 4),
                    "The sole map room must occupy the top storey");
        }
        helper.assertTrue(seenByFloor.get(0).containsAll(Set.of("1x1_a1", "1x1_a5")),
                "Both compatible ground-only flower rooms must remain available: " + witnesses);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void nativeCrossesAndTorchPostsConvertWithoutChangingTemplates(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        for (String name : List.of("wall_flat", "indoors_wall_1", "indoors_wall_2", "carpet_south_1", "carpet_south_2")) {
            var template = level.getStructureManager().getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + name));
            var before = template.save(new CompoundTag());
            var nativeBlocks = contents(template, level);
            for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
                var settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).addProcessor(IllagerMansionProcessor.INSTANCE);
                var blocks = new HashMap<BlockPos, BlockState>();
                StructureTemplate.processBlockInfos(level, BlockPos.ZERO, BlockPos.ZERO, settings, nativeBlocks, template)
                        .forEach(info -> blocks.put(info.pos(), info.state()));
                if (name.contains("wall")) {
                    var center = new BlockPos(0, 4, 3);
                    for (int distance = -2; distance <= 2; distance++) for (BlockPos local : List.of(center.above(distance), center.south(distance))) {
                        var actual = blocks.get(StructureTemplate.calculateRelativePosition(settings, local));
                        helper.assertTrue(actual.is(Blocks.OAK_LOG) && actual.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                                "Both small and ordinary wall crosses need five-block log arms and a log center: " + name + "/" + local);
                    }
                } else {
                    for (int x : new int[]{1, 7}) for (int y = 1; y <= 7; y++) {
                        var actual = blocks.get(StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, y, 2)));
                        helper.assertTrue(actual.is(Blocks.OAK_PLANKS),
                                "Native torch supports must be oak-plank columns: " + name + "/" + x + "," + y);
                    }
                    for (int x : new int[]{2, 6}) {
                        helper.assertTrue(blocks.get(StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, 4, 2))).is(Blocks.OAK_FENCE)
                                        && blocks.get(StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, 5, 2))).is(Blocks.TORCH),
                                "Keep the ordinary native oak fence and torch on each support");
                    }
                }
            }
            helper.assertTrue(before.equals(template.save(new CompoundTag())), "Do not mutate a cached native template: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void carpetCornersRoofSlabsAndFoyerStepsSurviveClippingAndReload(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getStructureManager();
        var context = StructurePieceSerializationContext.fromLevel(level);
        int variant = 0;
        for (Rotation rotation : Rotation.values()) {
            var origin = helper.absolutePos(new BlockPos(100 + 48 * variant++, 0, 100)).atY(180);
            var pieces = new ArrayList<WoodlandMansionPiece>();
            for (BlockPos offset : List.of(BlockPos.ZERO, new BlockPos(8, 0, 0), new BlockPos(0, 0, 8)))
                pieces.add(new WoodlandMansionPiece(manager, "corridor_floor", origin.offset(offset.rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_east", origin.offset(new BlockPos(5, 1, 1).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_south_2", origin.offset(new BlockPos(-1, 0, 5).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_west_2", origin.offset(new BlockPos(7, 0, -1).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_north", origin.offset(new BlockPos(1, 1, 8).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "roof", origin.above(20), rotation));
            var carpets = MansionRoomAccess.carpetPlan(manager, pieces);
            var caps = MansionRoomAccess.roofSlabs(manager, pieces);
            var corner = origin.offset(new BlockPos(5, 1, 5).rotate(rotation));
            helper.assertTrue(!carpets.red().contains(corner) && caps.size() == 64,
                    "Do not erase the native white corner merely because all four cardinal neighbors are carpet");
            var stair = new MansionEnvelopePiece.UpperStair(origin.offset(new BlockPos(3, 8, 3).rotate(rotation)), rotation.rotate(Direction.SOUTH));
            var bounds = BoundingBox.encapsulatingBoxes(pieces.stream().map(p -> p.getBoundingBox()).toList()).orElseThrow();
            var repair = new MansionEnvelopePiece(bounds, false, List.of(), carpets.red(), carpets.white(), caps, List.of(stair));
            repair = new MansionEnvelopePiece(context, repair.createTag(context));
            for (int cx = bounds.maxX() >> 4; cx >= bounds.minX() >> 4; cx--) for (int cz = bounds.maxZ() >> 4; cz >= bounds.minZ() >> 4; cz--) {
                var chunk = new ChunkPos(cx, cz);
                var clip = new BoundingBox(chunk.getMinBlockX(), 175, chunk.getMinBlockZ(), chunk.getMaxBlockX(), 205, chunk.getMaxBlockZ());
                for (var piece : pieces) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), clip, chunk, origin);
                repair.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), clip, chunk, origin);
            }
            for (var pos : carpets.red()) helper.assertTrue(level.getBlockState(pos).is(Blocks.RED_CARPET), "Junction interior must be consistently red at " + pos);
            for (var pos : carpets.white()) helper.assertTrue(level.getBlockState(pos).is(Blocks.WHITE_CARPET), "Diagonal border must be connected in white at " + pos);
            helper.assertTrue(level.getBlockState(corner).is(Blocks.WHITE_CARPET)
                            && level.getBlockState(origin.offset(new BlockPos(3, 1, 3).rotate(rotation))).is(Blocks.RED_CARPET),
                    "The native concave corner must join the white border while the path center stays red");
            for (var pos : caps) helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_SLAB)
                            && level.getBlockState(pos).getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM
                            && !level.getBlockState(pos).isValidSpawn(level, pos, EntityType.ZOMBIE),
                    "Exposed roof tops must be bottom oak slabs that prevent mob spawning at " + pos);
            for (int step = 1; step <= 3; step++) {
                var pos = stair.landing().relative(stair.outward(), step).above(step);
                helper.assertTrue(level.getBlockState(pos).is(Blocks.COBBLESTONE_STAIRS)
                                && level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING) == stair.outward()
                                && level.isEmptyBlock(pos.above()) && level.isEmptyBlock(pos.above(2)),
                        "Each persisted foyer extension must climb one block with clear headroom: " + pos);
            }
        }
        helper.succeed();
    }

    private static void addColumns(Set<Long> target, BoundingBox box) {
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) target.add(BlockPos.asLong(x, 0, z));
    }

    private static List<StructureBlockInfo> contents(StructureTemplate template, net.minecraft.server.level.ServerLevel level) {
        var data = template.save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var blocks = new ArrayList<StructureBlockInfo>();
        for (Tag entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) entry;
            var pos = block.getList("pos", Tag.TAG_INT);
            blocks.add(new StructureBlockInfo(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state"))),
                    block.contains("nbt", Tag.TAG_COMPOUND) ? block.getCompound("nbt") : null));
        }
        return blocks;
    }
}
