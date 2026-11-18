package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

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
        // Large bedrooms offer eight beds without changing a room's doorway or footprint.
        for (String shape : List.of("2x2_", "1x2_c", "1x2_d", "1x1_")) {
            for (int i = 0; i < pieces.size() && beds < MINIMUM_BEDS; i++) {
                var piece = pieces.get(i);
                String name = nativeName(names, piece);
                if (!name.startsWith(shape) || isMapRoom(manager, piece)) continue;
                String replacement = switch (shape) {
                    case "2x2_" -> MansionRoomCatalogue.BEDROOM_LARGE;
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
            return name.startsWith("1x") || name.startsWith("2x");
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
