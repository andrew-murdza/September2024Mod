package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiPredicate;

@Mixin(Swim.class)
public class MobsInLava9 {
    @Redirect(
            method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;isInFluidType(Ljava/util/function/BiPredicate;)Z",remap = false
            )
    )
    private boolean aoemod$lavaMobsCanFloatInLava(Mob entity, BiPredicate biPredicate) {
        if (LavaMobs.isLavaMob(entity) && entity.getEyeInFluidType() == ForgeMod.LAVA_TYPE.get()) {
            return true;
        }

        return entity.isInFluidType((fluidType, height) -> entity.canSwimInFluidType(fluidType));
    }
}
