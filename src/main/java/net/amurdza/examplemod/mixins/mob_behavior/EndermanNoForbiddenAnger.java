package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.event_handlers.MobAI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EnderMan.class)
public abstract class EndermanNoForbiddenAnger {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity aoemod$filterBeforeAngerAnimation(LivingEntity target) {
        // EnderMan changes its synced animation/sound flags before calling Mob.setTarget.
        // Passing null runs its normal calm-state cleanup before the base target filter.
        return target != null && MobAI.isNotAllowedMobTarget((EnderMan) (Object) this, target)
                ? null : target;
    }
}
