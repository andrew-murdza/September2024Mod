package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaBoundPathNavigation;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFish.class)
public class MobsInLava4 {
    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$useGroundNavigation(Level level, CallbackInfoReturnable<PathNavigation> cir) {
        AbstractFish self = (AbstractFish)(Object)this;
        if(LavaMobs.isLavaMob(self)){
            cir.setReturnValue(new LavaBoundPathNavigation(self, level));
        }
    }
}
