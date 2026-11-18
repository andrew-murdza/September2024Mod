package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.IllagerMansionProcessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WoodlandMansionPieces.WoodlandMansionPiece.class)
public class PlainsMansionMaterials {
    // Both fresh and NBT-loaded mansion pieces use this factory. The processor
    // itself checks the dimension, leaving native mansions elsewhere unchanged.
    @Inject(method = "makeSettings", at = @At("RETURN"))
    private static void aoemod$plainsMaterials(Mirror mirror, Rotation rotation,
            CallbackInfoReturnable<StructurePlaceSettings> cir) {
        cir.getReturnValue().addProcessor(IllagerMansionProcessor.INSTANCE);
    }
}
