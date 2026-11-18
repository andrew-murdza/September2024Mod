package net.amurdza.examplemod.mixins.othermods.orcz;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disable the dependency's morale updates, hit-and-run triggers and death reactions. */
@Mixin(targets = {
        "com.duke.orcz.procedures.OrcsTickingProcedure",
        "com.duke.orcz.procedures.OrcshurtsProcedure",
        "com.duke.orcz.procedures.EnragemoraleProcedure",
        "com.duke.orcz.procedures.MoralegoblinsProcedure"
}, remap = false)
public abstract class OrcPsychologyDisabled {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aoemod$removeMoraleSystem(CallbackInfo ci) {
        ci.cancel();
    }
}
