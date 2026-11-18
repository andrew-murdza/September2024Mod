package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.sugar.Local;
import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlaceCommand.class)
public abstract class DecoratePlacedSavannaCamp {
    @Inject(method = "placeStructure", at = @At("RETURN"))
    private static void aoemod$decorateFinishedCamp(CommandSourceStack source, Holder.Reference<Structure> structure,
            BlockPos pos, CallbackInfoReturnable<Integer> callback, @Local StructureStart start) {
        for (var piece : start.getPieces()) if (piece instanceof SavannaOrcCampPiece village) {
            SavannaCampVegetation.decoratePlaced(source.getLevel(), village);
            if (village.encounterId() != null) net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot.refresh(
                    source.getLevel(), village.getBoundingBox());
        }
    }
}
