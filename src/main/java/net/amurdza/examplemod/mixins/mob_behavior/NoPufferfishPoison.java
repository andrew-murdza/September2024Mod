package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Pufferfish.class)
public class NoPufferfishPoison {
    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Pufferfish;touch(Lnet/minecraft/world/entity/Mob;)V"))
    private void hi(Pufferfish instance, Mob pMob){

    }
    @Redirect(method = "playerTouch",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean hi(Player instance, DamageSource pSource, float pAmount){
        return false;
    }
}
