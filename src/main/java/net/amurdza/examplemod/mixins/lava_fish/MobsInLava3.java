package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaBoundPathNavigation;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFish.class)
public class MobsInLava3 {

    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/AbstractFish;isInWater()Z"))
    private boolean hi(AbstractFish instance){
        return LavaMobs.isLavaMob(instance)?instance.isInLava():instance.isInWater();
    }
    @Inject(method = "createNavigation",at= @At("HEAD"),cancellable = true)
    private void hi(Level pLevel, CallbackInfoReturnable<PathNavigation> cir){
        if(LavaMobs.isLavaMob(this)){
            cir.setReturnValue(new LavaBoundPathNavigation((Mob)(Object)this,pLevel));
        }
    }
    @Redirect(method = "travel",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/AbstractFish;isInWater()Z"))
    private boolean hi1(AbstractFish instance){
        return LavaMobs.isLavaMob(instance)?instance.isInLava():instance.isInWater();
    }
}
