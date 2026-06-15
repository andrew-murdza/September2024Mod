package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class)
public abstract class MobsInLava1 {
    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void aoemod$seaSpiderFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (LavaMobs.isLavaMob(this)) {
            cir.setReturnValue(true);
        }
    }
}
