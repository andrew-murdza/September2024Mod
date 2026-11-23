package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Reserve the sole map room and enough furnished sleeping space before placing any blocks. */
public final class MansionResidentialPlan {
    public static final int MINIMUM_BEDS = 180;

    private MansionResidentialPlan() { }

    public static void finish(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces, int groundY) {
        var names = names(manager);
        var mapRoom = pieces.stream().filter(piece -> nativeName(names, piece).startsWith("2x2_"))
                .max(Comparator.comparingInt(piece -> piece.templatePosition().getY())).orElseThrow();
        replace(manager, pieces, mapRoom, MansionRoomCatalogue.MAP_ROOM);
        int beds = pieces.stream().mapToInt(piece -> MansionRoomCatalogue.bedCapacity(names.get(piece.template()))).sum();
        // Prefer native bedrooms with the same footprint and doorway side.
        // A two-wide bed sleeps two; a three-wide bed sleeps three.
        for (String shape : List.of("1x2_c", "1x2_d", "1x1_")) {
            for (int i = 0; i < pieces.size() && beds < MINIMUM_BEDS; i++) {
                var piece = pieces.get(i);
                String name = nativeName(names, piece);
                if (!name.startsWith(shape) || isMapRoom(manager, piece)) continue;
                String replacement = switch (shape) {
                    case "1x2_c" -> MansionRoomCatalogue.compact("1x2_c3");
                    case "1x2_d" -> "aoe_compact_1x2_d1_v3";
                    default -> MansionRoomCatalogue.compact("1x1_b3");
                };
                int gained = MansionRoomCatalogue.bedCapacity(replacement) - MansionRoomCatalogue.bedCapacity(names.get(piece.template()));
                if (gained > 0) {
                    replace(manager, pieces, piece, replacement);
                    beds += gained;
                }
            }
        }
        if (beds < MINIMUM_BEDS) {
            var halls = pieces.stream().filter(piece -> nativeName(names, piece).equals("corridor_floor"))
                    .map(WoodlandMansionPiece::getBoundingBox).toList();
            for (var room : List.copyOf(pieces)) {
                if (beds >= MINIMUM_BEDS) break;
                if (!nativeName(names, room).startsWith("2x2_") || isMapRoom(manager, room)) continue;
                var split = bedroomPair(manager, room.getBoundingBox(), halls);
                if (split.isEmpty()) continue;
                // Remove the original large room's entrance before giving each
                // native bedroom exactly one direct hallway entrance.
                for (int i = 0; i < pieces.size(); i++) {
                    var wall = pieces.get(i);
                    if (nativeName(names, wall).equals("indoors_door_1")
                            && borders(room.getBoundingBox(), wall.getBoundingBox()))
                        pieces.set(i, new WoodlandMansionPiece(manager, "indoors_wall_1", wall.templatePosition(),
                                wall.getRotation(), wall.placeSettings().getMirror()));
                }
                pieces.remove(room);
                pieces.addAll(split);
                beds += split.stream().mapToInt(piece -> MansionRoomCatalogue.bedCapacity(names.get(piece.template()))).sum();
            }
        }
        if (beds < MINIMUM_BEDS) throw new IllegalStateException("Mansion plan has fewer than 180 beds");
        for (int i = 0; i < pieces.size(); i++) {
            var piece = pieces.get(i);
            String name = names.get(piece.template());
            if (name == null) name = names(manager).get(piece.template());
            String replacement = MansionRoomCatalogue.newTemplate(name);
            if (!replacement.equals(name)) pieces.set(i, new WoodlandMansionPiece(manager, replacement,
                    piece.templatePosition(), piece.getRotation(), piece.placeSettings().getMirror()));
        }
    }

    /** Two native rooms and native wall/door sections, with no invented bedroom template. */
    private static List<WoodlandMansionPiece> bedroomPair(StructureTemplateManager manager, BoundingBox room,
            List<BoundingBox> halls) {
        List<WoodlandMansionPiece> best = List.of();
        int capacity = 0;
        for (boolean splitX : new boolean[]{true, false}) {
            var pair = new ArrayList<WoodlandMansionPiece>();
            int beds = 0;
            for (int half = 0; half < 2; half++) {
                var box = new BoundingBox(room.minX() + (splitX ? half * 8 : 0), room.minY(),
                        room.minZ() + (splitX ? 0 : half * 8),
                        room.minX() + (splitX ? half * 8 + 6 : 14), room.minY() + 7,
                        room.minZ() + (splitX ? 14 : half * 8 + 6));
                WoodlandMansionPiece bedroom = null;
                String chosenName = null;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    if (halls.stream().noneMatch(hall -> adjacentHall(box, hall, side))) continue;
                    boolean sideEntrance = (side.getAxis() == Direction.Axis.X) == splitX;
                    String name = sideEntrance ? "1x2_c3" : "1x2_d1";
                    if (bedroom != null && MansionRoomCatalogue.bedCapacity(name) <= MansionRoomCatalogue.bedCapacity(chosenName)) continue;
                    Direction nativeSide = sideEntrance ? Direction.EAST : Direction.SOUTH;
                    Rotation rotation = java.util.Arrays.stream(Rotation.values()).filter(value -> value.rotate(nativeSide) == side)
                            .findFirst().orElseThrow();
                    for (Mirror mirror : sideEntrance ? new Mirror[]{Mirror.NONE, Mirror.LEFT_RIGHT} : new Mirror[]{Mirror.NONE}) {
                        var position = StructureTemplate.getZeroPositionWithTransform(new BlockPos(box.minX(), box.minY(), box.minZ()),
                                mirror, rotation, 7, 15);
                        var candidate = new WoodlandMansionPiece(manager,
                                name.equals("1x2_d1") ? "aoe_compact_1x2_d1_v3" : MansionRoomCatalogue.compact(name), position, rotation, mirror);
                        // A hall can border only the far half of a long room.
                        // Mirror the native side entrance to meet that hall;
                        // bounding-box overlap alone does not prove access.
                        if (!doorMeetsHall(candidate, sideEntrance, side, halls)) continue;
                        chosenName = name;
                        bedroom = candidate;
                        break;
                    }
                }
                if (bedroom == null) { pair.clear(); break; }
                pair.add(bedroom);
                beds += MansionRoomCatalogue.bedCapacity(chosenName);
                var doorOffset = chosenName.equals("1x2_c3") ? new BlockPos(7, 0, 0) : new BlockPos(6, 0, 15);
                var doorRotation = bedroom.getRotation().getRotated(chosenName.equals("1x2_c3") ? Rotation.NONE : Rotation.CLOCKWISE_90);
                pair.add(new WoodlandMansionPiece(manager, "indoors_door_1",
                        StructureTemplate.calculateRelativePosition(bedroom.placeSettings(), doorOffset).offset(bedroom.templatePosition()),
                        doorRotation, bedroom.placeSettings().getMirror()));
            }
            if (pair.isEmpty() || beds <= capacity) continue;
            for (int half = 0; half < 2; half++) {
                var position = splitX ? new BlockPos(room.minX() + 7, room.minY(), room.minZ() + half * 8)
                        : new BlockPos(room.minX() + 6 + half * 8, room.minY(), room.minZ() + 7);
                pair.add(new WoodlandMansionPiece(manager, "indoors_wall_1", position,
                        splitX ? Rotation.NONE : Rotation.CLOCKWISE_90));
            }
            best = pair;
            capacity = beds;
        }
        return best;
    }

    private static boolean doorMeetsHall(WoodlandMansionPiece room, boolean sideEntrance, Direction outward,
            List<BoundingBox> halls) {
        return halls.stream().anyMatch(hall -> {
            for (int across = 2; across <= 4; across++) {
                var local = sideEntrance ? new BlockPos(7, 1, across) : new BlockPos(across, 1, 15);
                var outside = StructureTemplate.calculateRelativePosition(room.placeSettings(), local)
                        .offset(room.templatePosition()).relative(outward);
                if (!hall.isInside(outside)) return false;
            }
            return true;
        });
    }

    private static boolean adjacentHall(BoundingBox room, BoundingBox hall, Direction side) {
        if (room.minY() != hall.minY()) return false;
        return switch (side) {
            case EAST -> hall.minX() - room.maxX() == 2 && overlap(room.minZ(), room.maxZ(), hall.minZ(), hall.maxZ()) >= 5;
            case WEST -> room.minX() - hall.maxX() == 2 && overlap(room.minZ(), room.maxZ(), hall.minZ(), hall.maxZ()) >= 5;
            case SOUTH -> hall.minZ() - room.maxZ() == 2 && overlap(room.minX(), room.maxX(), hall.minX(), hall.maxX()) >= 5;
            case NORTH -> room.minZ() - hall.maxZ() == 2 && overlap(room.minX(), room.maxX(), hall.minX(), hall.maxX()) >= 5;
            default -> false;
        };
    }

    private static boolean borders(BoundingBox room, BoundingBox wall) {
        if (room.minY() != wall.minY()) return false;
        return wall.getXSpan() == 1 && (wall.minX() == room.minX() - 1 || wall.minX() == room.maxX() + 1)
                && overlap(room.minZ(), room.maxZ(), wall.minZ(), wall.maxZ()) >= 2
                || wall.getZSpan() == 1 && (wall.minZ() == room.minZ() - 1 || wall.minZ() == room.maxZ() + 1)
                && overlap(room.minX(), room.maxX(), wall.minX(), wall.maxX()) >= 2;
    }

    private static int overlap(int firstMin, int firstMax, int secondMin, int secondMax) {
        return Math.min(firstMax, secondMax) - Math.max(firstMin, secondMin) + 1;
    }

    public static boolean isMapRoom(StructureTemplateManager manager, WoodlandMansionPiece piece) {
        return piece.template() == manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.MAP_ROOM));
    }

    public static int bedCapacity(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        var names = names(manager);
        return pieces.stream().mapToInt(piece -> MansionRoomCatalogue.bedCapacity(names.get(piece.template()))).sum();
    }

    public static List<BoundingBox> roomBounds(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        var names = names(manager);
        return pieces.stream().filter(piece -> {
            String name = nativeName(names, piece);
            return name.startsWith("1x") || name.startsWith("2x") || name.equals("library");
        }).map(WoodlandMansionPiece::getBoundingBox).toList();
    }

    private static String nativeName(Map<StructureTemplate, String> names, WoodlandMansionPiece piece) {
        return MansionRoomCatalogue.nativeName(names.getOrDefault(piece.template(), ""));
    }

    private static void replace(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces, WoodlandMansionPiece old, String name) {
        pieces.set(pieces.indexOf(old), new WoodlandMansionPiece(manager, name, old.templatePosition(), old.getRotation(), old.placeSettings().getMirror()));
    }

    private static Map<StructureTemplate, String> names(StructureTemplateManager manager) {
        var names = new IdentityHashMap<StructureTemplate, String>();
        manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith("woodland_mansion/"))
                .forEach(id -> manager.get(id).ifPresent(template -> names.put(template, id.getPath().substring("woodland_mansion/".length()))));
        return names;
    }
}
