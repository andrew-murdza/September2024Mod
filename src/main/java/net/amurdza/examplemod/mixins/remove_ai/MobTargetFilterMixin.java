package net.amurdza.examplemod.mixins.remove_ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobTargetFilterMixin {

    @Shadow
    @Nullable
    private LivingEntity target;

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void aoemod$blockForbiddenTargets(@Nullable LivingEntity newTarget, CallbackInfo ci) {
        Mob attacker = (Mob) (Object) this;

        if (newTarget != null && aoemod$isNotAllowedMobTarget(attacker, newTarget)) {
            this.target = null;
            ci.cancel();
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void aoemod$blockForbiddenDirectAttacks(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Mob attacker = (Mob) (Object) this;

        if (entity instanceof LivingEntity living && aoemod$isNotAllowedMobTarget(attacker, living)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean aoemod$isNotAllowedMobTarget(Mob attacker, LivingEntity target) {
        return aoemod$isNotProtectedCombatEntity(attacker) && aoemod$isNotProtectedCombatEntity(target);
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