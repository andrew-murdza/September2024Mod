package net.amurdza.examplemod.mixins.remove_ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvoidEntityGoal.class)
public class AvoidEntityGoalFilterMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    @Nullable
    protected LivingEntity toAvoid;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void aoemod$doNotRunFromForbiddenTargets(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && this.toAvoid != null && aoemod$isNotAllowedRunTarget(this.mob, this.toAvoid)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void aoemod$stopRunningFromForbiddenTargets(CallbackInfoReturnable<Boolean> cir) {
        if (this.toAvoid != null && aoemod$isNotAllowedRunTarget(this.mob, this.toAvoid)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean aoemod$isNotAllowedRunTarget(Mob mob, LivingEntity target) {
        return aoemod$isNotProtectedCombatEntity(mob) && aoemod$isNotProtectedCombatEntity(target);
    }

    @Unique
    private static boolean aoemod$isNotProtectedCombatEntity(LivingEntity entity) {
        if (entity instanceof Player) {
            return false;
        }

        if (entity instanceof Villager) {
            return false;
        }

        if (entity instanceof Piglin) {
            return false;
        }

        if (entity instanceof PiglinBrute) {
            return false;
        }

        if (entity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            return false;
        }

        if (entity instanceof AbstractHorse abstractHorse && abstractHorse.isTamed()) {
            return false;
        }

        return true;
    }
}
