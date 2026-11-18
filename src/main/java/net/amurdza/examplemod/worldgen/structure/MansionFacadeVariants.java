package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Whole-estate facade variants use distinct template IDs, including after a saved chunk reload. */
public final class MansionFacadeVariants {
    private MansionFacadeVariants() { }

    public static void withoutWalls(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        Map<StructureTemplate, String> names = new HashMap<>();
        manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft")
                && id.getPath().startsWith("woodland_mansion/") && !id.getPath().contains("/aoe_plain_"))
                .forEach(id -> manager.get(id).ifPresent(template -> names.put(template,
                        id.getPath().substring("woodland_mansion/".length()))));
        for (int i = 0; i < pieces.size(); i++) {
            var piece = pieces.get(i);
            if (piece.template().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.COBBLESTONE_WALL).isEmpty()) continue;
            String name = names.get(piece.template());
            if (name == null) throw new IllegalStateException("Unknown facade template");
            String plain = "aoe_plain_" + name;
            if (manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + plain)).isEmpty())
                throw new IllegalStateException("Missing plain mansion facade: " + plain);
            pieces.set(i, new WoodlandMansionPiece(manager, plain, piece.templatePosition(), piece.getRotation(),
                    piece.placeSettings().getMirror()));
        }
    }
}
