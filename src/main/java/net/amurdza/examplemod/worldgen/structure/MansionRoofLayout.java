package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Lower the whole roof three blocks and extend its eaves three blocks outward. */
public final class MansionRoofLayout {
    public static final String FLAT = "aoe_v6_roof";
    public static final String FRONT = "aoe_v6_roof_front";
    public static final String CORNER = "aoe_v6_roof_corner";
    public static final String INNER_CORNER = "aoe_v6_roof_inner_corner";

    private MansionRoofLayout() { }

    public static WoodlandMansionPiece adapt(StructureTemplateManager manager, String nativeName, WoodlandMansionPiece original) {
        String name;
        BlockPos offset;
        switch (nativeName) {
            case "roof" -> { name = FLAT; offset = new BlockPos(-3, -5, -3); }
            case "roof_front" -> { name = FRONT; offset = new BlockPos(3, -5, -3); }
            case "roof_corner" -> { name = CORNER; offset = new BlockPos(3, -5, 3); }
            case "roof_inner_corner" -> { name = INNER_CORNER; offset = new BlockPos(3, -5, -3); }
            default -> throw new IllegalArgumentException("Unknown mansion roof template: " + nativeName);
        }
        // The native placer emits unmirrored roof pieces. Rotate both outward
        // axes together so all four corners remain joined in every rotation.
        return new PlannedRoofPiece(manager, name, original.templatePosition().offset(offset.rotate(original.getRotation())),
                original.getRotation(), original.placeSettings().getMirror(), nativeName.equals("roof") ? 0 : nativeName.equals("roof_front") ? 1 : 2);
    }

    /** Marks newly planned pieces for placement after the continuous Y24/Y25 ceiling. */
    public static boolean isRoof(WoodlandMansionPiece piece) { return piece instanceof PlannedRoofPiece; }

    /** Expanded flat tiles must never overwrite an already shaped edge or corner. */
    public static int placementOrder(WoodlandMansionPiece piece) {
        return piece instanceof PlannedRoofPiece roof ? roof.order : -1;
    }

    // Serialization remains the native mansion-piece format and saves the
    // versioned template and transformed position, not a new piece type.
    private static final class PlannedRoofPiece extends WoodlandMansionPiece {
        private final int order;

        private PlannedRoofPiece(StructureTemplateManager manager, String name, BlockPos position, Rotation rotation, Mirror mirror, int order) {
            super(manager, name, position, rotation, mirror);
            this.order = order;
        }
    }
}
