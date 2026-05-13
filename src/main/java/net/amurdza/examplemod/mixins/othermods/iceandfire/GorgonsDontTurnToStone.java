package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.entity.EntityGorgon;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityGorgon.class,remap = false)
public class GorgonsDontTurnToStone {
    @Redirect(method = "doHurtTarget",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/iceandfire/entity/EntityGorgon;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private boolean hi1(EntityGorgon instance, MobEffect mobEffect){
        return true;
    }

    @Redirect(method = "setTarget",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/iceandfire/entity/EntityGorgon;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private boolean hi2(EntityGorgon instance, MobEffect mobEffect){
        return true;
    }

    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/iceandfire/entity/EntityGorgon;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private boolean hi(EntityGorgon instance, MobEffect mobEffect){
        return true;
    }
}
