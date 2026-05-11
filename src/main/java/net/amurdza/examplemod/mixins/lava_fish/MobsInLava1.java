package net.amurdza.examplemod.mixins.lava_fish;

import com.legacy.blue_skies.entities.hostile.EmberbackEntity;
import com.legacy.blue_skies.entities.passive.SlivEntity;
import com.starfish_studios.naturalist.common.entity.Lizard;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MobsInLava1 {
    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void aoemod$seaSpiderFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (LavaMobs.isLavaMob(this)||(Object) this instanceof SlivEntity||(Object) this instanceof Lizard
                ||(Object) this instanceof EmberbackEntity) {
            cir.setReturnValue(true);
        }
    }
}
