package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SmoothSwimmingMoveControl.class)
public class MobsInLava5 {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;isInWater()Z"
            )
    )
    private boolean aoemod$treatLavaAsWaterForLavaMobs(Mob mob) {
        return mob.isInWater() || (LavaMobs.isLavaMob(mob) && mob.isInLava());
    }
}
