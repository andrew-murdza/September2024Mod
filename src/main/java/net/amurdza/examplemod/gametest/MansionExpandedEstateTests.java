package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionExpandedEstateTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void fiveFloorsOneMapRoomAndBedsSurviveSeedsRotationsAndReload(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (int seed = 0; seed < 3; seed++) for (var rotation : Rotation.values()) {
            var builder = new StructurePiecesBuilder();
            IllagerMansionStructure.addMansionPieces(manager, new BlockPos(0, 140, 0), rotation, builder, RandomSource.create(seed));
            var pieces = builder.build();
            var rooms = pieces.pieces().stream().filter(WoodlandMansionPiece.class::isInstance).map(WoodlandMansionPiece.class::cast).toList();
            var names = names(manager);
            h.assertTrue(rooms.stream().filter(p -> MansionResidentialPlan.isMapRoom(manager, p)).count() == 1, "Exactly one map room is required");
            h.assertTrue(MansionResidentialPlan.bedCapacity(manager, rooms) >= 180, "The furnished rooms need at least 180 effective beds");
            var floors = rooms.stream().filter(p -> MansionRoomCatalogue.nativeName(names.get(p.template())).equals("corridor_floor"))
                    .map(p -> p.templatePosition().getY()).collect(java.util.stream.Collectors.toSet());
            h.assertTrue(floors.equals(Set.of(140, 148, 156, 164, 172)), "Five eight-block floors must be present: " + floors);
            var stairs = rooms.stream().filter(p -> names.get(p.template()).equals(MansionRoomCatalogue.newTemplate(MansionUpperLayout.SINGLE_STAIRS))).toList();
            h.assertTrue(stairs.size() == 4, "Four separate flights must connect five floors");
            var ceilings = rooms.stream().filter(p -> names.get(p.template()).equals(MansionRoomCatalogue.newTemplate("aoe_compact_ceiling_v3"))).toList();
            h.assertTrue(ceilings.size() == 121 && ceilings.stream().allMatch(p -> p.templatePosition().getY() == 180), "Roof must cover all 11 by 11 cells");
            var residents = pieces.pieces().stream().filter(MansionResidentsPiece.class::isInstance).map(MansionResidentsPiece.class::cast).findFirst().orElseThrow();
            var restored = new MansionResidentsPiece(serial, residents.createTag(serial));
            var count = restored.residents().stream().collect(java.util.stream.Collectors.groupingBy(MansionResidentsPiece.Resident::type, java.util.stream.Collectors.counting()));
            h.assertTrue(count.equals(Map.of("minecraft:evoker",15L,"minecraft:witch",15L,"minecraft:pillager",65L,"minecraft:vindicator",65L)), "Saved roster counts changed: " + count);
            for (var room : rooms) {
                var saved = new WoodlandMansionPiece(manager, room.createTag(serial));
                var data = ((MansionEncounterAccess) saved).aoemod$encounter();
                h.assertTrue(data.fixedRoster() && data.expected() == 161, "Native resident suppression must survive reload");
                h.assertTrue(room.template().filterBlocks(BlockPos.ZERO, room.placeSettings(), Blocks.BIRCH_PLANKS).isEmpty(), "New room must not retain birch planks: " + names.get(room.template()));
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void completeEstateConnectsFiveFloorsAndSpawnsExactRoster(GameTestHelper h) throws Exception {
        var level = h.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.PLAINS);
        var manager = level.getStructureManager(); var generator = level.getChunkSource().getGenerator();
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(new ResourceLocation("aoemod:illager_mansion"));
        var origin = new BlockPos(3080, 150, 3080);
        var builder = new StructurePiecesBuilder();
        IllagerMansionStructure.addMansionPieces(manager, origin, Rotation.NONE, builder, RandomSource.create(0));
        var pieces = builder.build(); var bounds = pieces.calculateBoundingBox();
        var placementBounds = structure.adjustBoundingBox(bounds);
        for (int cx = placementBounds.minX() >> 4; cx <= placementBounds.maxX() >> 4; cx++) for (int cz = placementBounds.minZ() >> 4; cz <= placementBounds.maxZ() >> 4; cz++) {
            var chunk = new ChunkPos(cx,cz);
            var clip = new BoundingBox(chunk.getMinBlockX(),level.getMinBuildHeight(),chunk.getMinBlockZ(),chunk.getMaxBlockX(),level.getMaxBuildHeight()-1,chunk.getMaxBlockZ());
            for (var piece : pieces.pieces()) if (piece.getBoundingBox().intersects(clip))
                piece.postProcess(level,level.structureManager(),generator,RandomSource.create(0),clip,chunk,origin);
            structure.afterPlace(level,level.structureManager(),generator,RandomSource.create(0),clip,chunk,pieces);
        }
        var rooms = pieces.pieces().stream().filter(WoodlandMansionPiece.class::isInstance).map(WoodlandMansionPiece.class::cast).toList();
        var cellar = pieces.pieces().stream().filter(MansionBasementPiece.class::isInstance).map(MansionBasementPiece.class::cast).findFirst().orElseThrow();
        PlacedMansions.get(level).record(cellar,rooms.stream().map(WoodlandMansionPiece::getBoundingBox).toList());
        MansionDefenderSpawns.retryLoaded(level);
        var defenders = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,net.minecraft.world.phys.AABB.of(bounds).inflate(8),
                mob -> AuthoredEncounterProgress.isDefender(mob) && cellar.encounterId().equals(AuthoredEncounterProgress.encounterId(mob)));
        var counts = defenders.stream().collect(java.util.stream.Collectors.groupingBy(mob -> net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString(),java.util.stream.Collectors.counting()));
        java.nio.file.Files.writeString(java.nio.file.Path.of("expanded-mansion-spawns.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("counts",counts,"pending",MansionDefenderSpawns.get(level).plans().stream().filter(plan -> plan.encounter().equals(cellar.encounterId())).toList())));
        h.assertTrue(counts.equals(Map.of("minecraft:evoker",15L,"minecraft:witch",15L,"minecraft:pillager",65L,"minecraft:vindicator",65L,"aoemod:illager_lord",1L)), "All 161 defenders must actually spawn: " + counts);
        var mapRoom = rooms.stream().filter(p -> MansionResidentialPlan.isMapRoom(manager,p)).findFirst().orElseThrow();
        h.assertTrue(defenders.stream().filter(m -> m.getType() == net.amurdza.examplemod.registry.ModEntities.ILLAGER_LORD.get()).allMatch(m -> mapRoom.getBoundingBox().isInside(m.blockPosition())), "The lord must spawn in the sole map room");
        var names = names(manager);
        int intactBeds = 0;
        for (var room : rooms) {
            int capacity = MansionRoomCatalogue.bedCapacity(names.get(room.template()));
            if (capacity == 0) continue;
            for (var wool : List.of(Blocks.WHITE_WOOL, Blocks.RED_WOOL))
                for (var block : room.template().filterBlocks(room.templatePosition(), room.placeSettings(), wool))
                    h.assertTrue(level.getBlockState(block.pos()).is(wool),
                            "Doorway repairs must preserve the complete native bed: " + names.get(room.template()) + "/" + block.pos());
            intactBeds += capacity;
        }
        h.assertTrue(intactBeds >= 180, "At least 180 bed-width places must survive real placement");
        var entries = rooms.stream().filter(p -> names.get(p.template()).equals(MansionRoomCatalogue.newTemplate(MansionUpperLayout.ENTRANCE))).toList();
        for (var entrance : entries) for (int x = 1; x <= 5; x++) for (int z = 0; z <= 3; z++) {
            var expected = z == 0 && (x == 1 || x == 5) ? Blocks.OAK_PLANKS : z <= 1 ? Blocks.AIR : z == 2 || x == 1 || x == 5 ? Blocks.WHITE_CARPET : Blocks.RED_CARPET;
            var pos = local(entrance, x, 1, z);
            h.assertTrue(level.getBlockState(pos).is(expected),
                    "The new bordered entrance rug must survive overlapping pieces and carpet repair: " + pos
                            + ", local=" + x + "/1/" + z + ", rotation=" + entrance.getRotation()
                            + ", expected=" + expected + ", actual=" + level.getBlockState(pos));
        }
        var facadeExpected = new HashMap<BlockPos, net.minecraft.world.level.block.Block>();
        for (var room : rooms) if (List.of("aoe_v9_wall_window_top_trim", "aoe_v9_wall_flat_top_trim",
                "aoe_v9_wall_corner_top_trim").contains(names.get(room.template()))) {
            // postProcess leaves the last chunk clip on each piece. Inspect the whole trim.
            var allBlocks = room.placeSettings().copy().setBoundingBox(null);
            for (var material : List.of(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL))
                for (var block : room.template().filterBlocks(room.templatePosition(), allBlocks, material)) {
                    var expected = material == Blocks.DARK_OAK_LOG ? Blocks.OAK_LOG : material == Blocks.DARK_OAK_PLANKS ? Blocks.OAK_PLANKS : material;
                    facadeExpected.put(block.pos(), expected);
                }
        }
        // Corner masonry deliberately owns the cells where the perimeter turns.
        // Compare the complete, ordered trim assembly rather than mutually overlapping pieces.
        facadeExpected.forEach((pos, expected) -> h.assertTrue(level.getBlockState(pos).is(expected),
                "Roof placement must retain solid headers, plank infill and extended masonry corners: "
                        + pos + "/expected=" + expected + "/actual=" + level.getBlockState(pos)));
        for (var room : rooms) if ("aoe_v9_wall_corner_top_trim".equals(names.get(room.template()))) {
            var aboveCorner = local(room, 8, 3, 1);
            h.assertFalse(level.getBlockState(aboveCorner).is(Blocks.COBBLESTONE)
                            || level.getBlockState(aboveCorner).is(Blocks.COBBLESTONE_WALL),
                    "The extended post must not create a detached masonry cap above the roof: " + aboveCorner);
        }
        for (int x = 8; x <= 30; x++) for (int y = 1; y <= 5; y++)
            h.assertTrue(level.getBlockState(cellar.world(x, y, 18)).is(Blocks.STONE_BRICKS),
                    "The upper crop room must have an authored brick front wall");
        var entry = local(entries.get(0),3,1,2);
        h.assertTrue(canWalk(level,entry), "An entrance must lead onto a supported floor: " + entry);
        var walking = walk(level,bounds,entry);
        var inaccessible = new ArrayList<String>(); var floors = new TreeMap<Integer,Integer>();
        var halls = rooms.stream().filter(p -> MansionRoomCatalogue.nativeName(names.get(p.template())).equals("corridor_floor")).toList();
        for (var room : rooms) {
            String name = MansionRoomCatalogue.nativeName(names.get(room.template()));
            if (!(name.startsWith("1x") || name.startsWith("2x") || name.equals("library") || name.equals("corridor_floor"))) continue;
            var b = room.getBoundingBox(); int y = room.templatePosition().getY();
            if (walking.stream().noneMatch(p -> p.getX() > b.minX() && p.getX() < b.maxX() && p.getZ() > b.minZ() && p.getZ() < b.maxZ() && p.getY() >= y+1 && p.getY() <= y+2)) inaccessible.add(name+"@"+b);
            if (!name.equals("corridor_floor")) h.assertTrue(halls.stream().anyMatch(hall -> directHallway(level,room,hall)), "Room has no direct hall connection: "+name+"@"+b);
            floors.merge(y,1,Integer::sum);
        }
        var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        java.nio.file.Files.writeString(java.nio.file.Path.of("expanded-mansion-report.json"),gson.toJson(Map.of("roster",counts,"beds",MansionResidentialPlan.bedCapacity(manager,rooms),"intactBeds",intactBeds,"floors",floors,"inaccessible",inaccessible,"walkingCells",walking.size())));
        h.assertTrue(inaccessible.isEmpty(), "Unreachable rooms: " + inaccessible);
        for (var door : cellar.doorLeaves()) h.assertTrue(level.getBlockState(door).is(Blocks.IRON_DOOR), "Every new cellar door must be iron");
        for (int layer=0; layer<2; layer++) for (int row=0; row<5; row++) for (boolean outer:new boolean[]{false,true}) for(boolean east:new boolean[]{false,true}) {
            var old = MansionBasementLayoutV5.roomCenter(east,row,outer);
            var target = cellar.world(old.getX()+(old.getX()>=6?1:0),1-layer*7,old.getZ()+(layer==0?5:12));
            h.assertTrue(walking.stream().anyMatch(p -> Math.abs(p.getX()-target.getX())<=5 && Math.abs(p.getZ()-target.getZ())<=5 && Math.abs(p.getY()-target.getY())<=1), "Unreachable cellar room layer"+layer+" "+target);
        }
        MansionRampTerrain.decoratePlaced(level,new net.minecraft.world.level.levelgen.structure.StructureStart(structure,new ChunkPos(origin),0,pieces));
        var views = new ArrayList<Map<String,Object>>();
        views.add(view(level,"mansion-five-floor-overview",bounds.maxX()+45.5,bounds.maxY()+26,bounds.minZ()-42.5,45,24));
        var forestView = new java.util.HashMap<>(view(level,"mansion-square-forest",origin.getX()+.5,origin.getY()+140,origin.getZ()+.5,180,90));
        forestView.put("fov", 110);
        views.add(forestView);
        for (int i=0;i<entries.size();i++) {
            var p=entries.get(i);var camera=local(p,3,2,-10);var target=local(p,3,3,3).subtract(camera);
            views.add(view(level,"mansion-vanilla-platform-"+i,camera.getX()+.5,camera.getY()+.1,camera.getZ()+.5,(float)Math.toDegrees(Math.atan2(-target.getX(),target.getZ())),0));
        }
        var northEntry = entries.stream().filter(p -> p.getRotation() == Rotation.NONE).findFirst().orElseThrow();
        var northCamera = local(northEntry, 3, 2, -8);
        views.add(view(level,"mansion-north-entrance",northCamera.getX()+.5,northCamera.getY()+.1,northCamera.getZ()+.5,0,0));
        var roofUnder = local(northEntry, 3, 34, -4);
        views.add(view(level,"mansion-north-roof-underside",roofUnder.getX()+.5,roofUnder.getY()+.1,roofUnder.getZ()+.5,0,-50));
        var roofTop = local(northEntry, 3, 45, -6);
        views.add(view(level,"mansion-north-roof-top",roofTop.getX()+.5,roofTop.getY()+.1,roofTop.getZ()+.5,0,20));
        var rugCamera = local(northEntry, 3, 3, 5);
        views.add(view(level,"mansion-bordered-entrance-rug",rugCamera.getX()+.5,rugCamera.getY()+.1,rugCamera.getZ()+.5,180,50));
        var facadeCamera = local(northEntry, 12, 36, -5);
        views.add(view(level,"mansion-top-window-headers",facadeCamera.getX()+.5,facadeCamera.getY()+.1,facadeCamera.getZ()+.5,0,-15));
        var upperFront = cellar.world(12,2,23);
        views.add(view(level,"mansion-upper-crop-room-brick-wall",upperFront.getX()+.5,upperFront.getY()+.1,upperFront.getZ()+.5,180,0));
        var lowerFront = cellar.world(12,-5,30);
        views.add(view(level,"mansion-lower-cellar-front-wall",lowerFront.getX()+.5,lowerFront.getY()+.1,lowerFront.getZ()+.5,180,0));
        var camera=local(mapRoom,7,2,1);views.add(view(level,"mansion-single-map-room",camera.getX()+.5,camera.getY()+.1,camera.getZ()+.5,mapRoom.getRotation().rotate(net.minecraft.core.Direction.SOUTH).toYRot(),-10));
        for(int layer=0;layer<2;layer++) {
            var p=layer==0?cellar.world(0,2,10):cellar.world(5,-6,20);
            float yaw=cellar.stairRotation().rotate(net.minecraft.core.Direction.SOUTH).toYRot()+(layer==0?0:45);
            var cellarView=new java.util.HashMap<>(view(level,"mansion-cellar-level-"+layer,p.getX()+.5,p.getY()+.1,p.getZ()+.5,yaw,layer==0?10:-10));
            if(layer==1)cellarView.put("fov",90);
            views.add(cellarView);
        }
        var doorCamera=cellar.world(-4,2,15);
        views.add(view(level,"mansion-iron-door-and-levers",doorCamera.getX()+.5,doorCamera.getY()+.1,doorCamera.getZ()+.5,cellar.stairRotation().rotate(net.minecraft.core.Direction.SOUTH).toYRot(),0));
        views.add(view(level,"mansion-roof-corner",bounds.maxX()+10.5,origin.getY()+39,bounds.minZ()-10.5,45,-5));
        views.add(view(level,"mansion-forest-ramp-edge",bounds.maxX()+140.5,origin.getY()+4,origin.getZ()+20.5,90,12));
        rooms.stream().filter(p -> names.get(p.template()).equals(MansionUpperLayout.SINGLE_STAIRS)).findFirst().ifPresent(p -> {
            var c=local(p,3,1,-2);var t=local(p,3,5,4).subtract(c);
            views.add(view(level,"mansion-wood-stair-torches",c.getX()+.5,c.getY()+.1,c.getZ()+.5,(float)Math.toDegrees(Math.atan2(-t.getX(),t.getZ())),-24));
        });
        for (String detail : List.of("1x2_c3","1x2_c4")) rooms.stream().filter(p -> MansionRoomCatalogue.nativeName(names.get(p.template())).equals(detail)).findFirst().ifPresent(p -> {
            var c=detail.equals("1x2_c3")?local(p,5,2,3):local(p,3,3,7);
            var t=(detail.equals("1x2_c3")?local(p,2,2,10):local(p,0,6,5)).subtract(c);
            views.add(view(level,"mansion-room-"+detail,c.getX()+.5,c.getY()+.1,c.getZ()+.5,(float)Math.toDegrees(Math.atan2(-t.getX(),t.getZ())),detail.equals("1x2_c3")?10:-20));
        });
        var library = rooms.stream().filter(p -> names.get(p.template()).equals("aoe_v9_library")).findFirst().orElseThrow();
        var libraryInside = local(library, 3, 1, 13);
        views.add(view(level, "mansion-authored-library", libraryInside.getX() + .5, libraryInside.getY() + .05,
                libraryInside.getZ() + .5, library.getRotation().rotate(net.minecraft.core.Direction.NORTH).toYRot(), 10));
        var libraryDoor = local(library, 3, 1, 17);
        views.add(view(level, "mansion-library-door", libraryDoor.getX() + .5, libraryDoor.getY() + .05,
                libraryDoor.getZ() + .5, library.getRotation().rotate(net.minecraft.core.Direction.NORTH).toYRot(), 0));
        var mineView = cellar.world(-35, 1, MansionBasementLayoutV11.FIRST_ROOM_SHIFT + 16);
        views.add(view(level, "mansion-mine-outer-wall", mineView.getX() + .5, mineView.getY() + .05,
                mineView.getZ() + .5, cellar.stairRotation().rotate(net.minecraft.core.Direction.WEST).toYRot(), 0));
        java.nio.file.Files.writeString(java.nio.file.Path.of("expanded-mansion-views.json"),gson.toJson(views));
        level.getServer().saveEverything(true,true,true); h.succeed();
    }

    private static Map<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate,String> names(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager) {
        var result = new IdentityHashMap<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate,String>();
        manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith("woodland_mansion/"))
            .forEach(id -> manager.get(id).ifPresent(t -> result.put(t,id.getPath().substring("woodland_mansion/".length()))));
        return result;
    }
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
