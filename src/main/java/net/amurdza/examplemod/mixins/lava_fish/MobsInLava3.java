package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiPredicate;

@Mixin(Entity.class)
public class MobsInLava3 {
    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    private void aoemod$lavaCountsAsWater(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (LavaMobs.isLavaMob(self)) {
            cir.setReturnValue(self.isInLava());
        }
    }
    @Redirect(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isInFluidType(Ljava/util/function/BiPredicate;)Z",remap = false
            )
    )
    private boolean aoemod$lavaMobsCanSwimInLavaForUpdateSwimming(Entity entity, BiPredicate biPredicate) {
        if (LavaMobs.isLavaMob(entity) && entity.getEyeInFluidType() == ForgeMod.LAVA_TYPE.get()) {
            return true;
        }

        return entity.isInFluidType((fluidType, height) -> entity.canSwimInFluidType(fluidType));
    }

    @Redirect(
            method = "isVisuallyCrawling",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isInFluidType(Ljava/util/function/BiPredicate;)Z",remap = false
            )
    )
    private boolean aoemod$lavaMobsCanSwimInLavaForCrawling(Entity entity, BiPredicate biPredicate) {
        if (LavaMobs.isLavaMob(entity) && entity.getEyeInFluidType() == ForgeMod.LAVA_TYPE.get()) {
            return true;
        }

        return entity.isInFluidType((fluidType, height) -> entity.canSwimInFluidType(fluidType));
    }
}
