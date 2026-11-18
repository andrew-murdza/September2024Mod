package net.amurdza.examplemod.mixins.othermods.orcz;

import net.amurdza.examplemod.worldgen.OrczCampResidents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.duke.orcz.procedures.CampStructureGeneratorNeighbourBlockChangesProcedure", remap = false)
public abstract class SelectedCampResidents {
    @Inject(method = "execute", at = @At("TAIL"))
    private static void aoemod$retainSelectedCampResidents(LevelAccessor level, double x, double y, double z,
                                                          CallbackInfo ci) {
        if (level instanceof ServerLevel server) {
            OrczCampResidents.ensureSelectedResidents(server, BlockPos.containing(x, y, z));
        }
    }
}
