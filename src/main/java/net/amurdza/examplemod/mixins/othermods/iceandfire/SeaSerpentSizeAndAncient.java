package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntitySeaSerpent.class)
public class SeaSerpentSizeAndAncient {
    @Redirect(method = "finalizeSpawn",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/iceandfire/entity/EntitySeaSerpent;setAncient(Z)V", remap = false))
    private void hi(EntitySeaSerpent instance, boolean ancient){

    }
    @ModifyArg(
            method = "finalizeSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/iceandfire/entity/EntitySeaSerpent;setSeaSerpentScale(F)V",
                    remap = false),
            index = 0
    )
    private float forceScale(float original) {
        return 3.0F;
    }
    @Redirect(method = "onWorldSpawn",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/iceandfire/entity/EntitySeaSerpent;setAncient(Z)V"), remap = false)
    private void hi1(EntitySeaSerpent instance, boolean ancient){

    }
    @ModifyArg(
            method = "onWorldSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/iceandfire/entity/EntitySeaSerpent;setSeaSerpentScale(F)V"
            ),
            index = 0, remap = false)
    private float forceScale1(float original) {
        return 3.0F;
    }

}
