package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.IllagerMansionProcessor;
import net.amurdza.examplemod.worldgen.structure.MansionLibraryLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WoodlandMansionPieces.WoodlandMansionPiece.class)
public class PlainsMansionMaterials {
    // Both fresh and NBT-loaded mansion pieces use this factory. The processor
    // itself checks the dimension, leaving native mansions elsewhere unchanged.
    @Inject(method = "makeSettings", at = @At("RETURN"))
    private static void aoemod$plainsMaterials(Mirror mirror, Rotation rotation,
            CallbackInfoReturnable<StructurePlaceSettings> cir) {
        cir.getReturnValue().addProcessor(IllagerMansionProcessor.INSTANCE);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Ljava/lang/String;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/block/Mirror;)V", at = @At("RETURN"))
    private void aoemod$keepAuthoredLibrary(StructureTemplateManager manager, String name, BlockPos pos,
            Rotation rotation, Mirror mirror, CallbackInfo ci) {
        if (MansionLibraryLayout.preservesAuthoredBlocks(name))
            ((WoodlandMansionPieces.WoodlandMansionPiece) (Object) this).placeSettings()
                    .getProcessors().remove(IllagerMansionProcessor.INSTANCE);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void aoemod$loadAuthoredLibrary(StructureTemplateManager manager, CompoundTag tag, CallbackInfo ci) {
        if (MansionLibraryLayout.preservesAuthoredBlocks(tag.getString("Template")))
            ((WoodlandMansionPieces.WoodlandMansionPiece) (Object) this).placeSettings()
                    .getProcessors().remove(IllagerMansionProcessor.INSTANCE);
    }
}
