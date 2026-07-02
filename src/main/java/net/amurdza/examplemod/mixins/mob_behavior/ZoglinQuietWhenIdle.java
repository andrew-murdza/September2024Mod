package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Zoglin.class)
public class ZoglinQuietWhenIdle {
    @Inject(method = "isTargetable(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void aoemod$doNotBrainTargetProtectedMobs(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (aoemod$isNotAllowedMobTarget((Zoglin) (Object) this, target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setAttackTarget(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void aoemod$doNotRememberProtectedAttackTargets(LivingEntity target, CallbackInfo ci) {
        if (aoemod$isNotAllowedMobTarget((Zoglin) (Object) this, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateActivity()V", at = @At("TAIL"))
    private void aoemod$clearDisallowedBrainTarget(CallbackInfo ci) {
        Zoglin zoglin = (Zoglin) (Object) this;
        Optional<LivingEntity> attackTarget = zoglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (attackTarget.isPresent() && aoemod$isNotAllowedMobTarget(zoglin, attackTarget.get())) {
            zoglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            zoglin.setAggressive(false);
        }
    }

    @Inject(method = "getAmbientSound()Lnet/minecraft/sounds/SoundEvent;", at = @At("HEAD"), cancellable = true)
    private void aoemod$calmAmbientWhenNoAllowedTarget(CallbackInfoReturnable<SoundEvent> cir) {
        if (!aoemod$hasAllowedBrainTarget()) {
            cir.setReturnValue(SoundEvents.ZOGLIN_AMBIENT);
        }
    }

    @Inject(method = "playAngrySound()V", at = @At("HEAD"), cancellable = true)
    private void aoemod$skipAngrySoundForDisallowedTargets(CallbackInfo ci) {
        if (!aoemod$hasAllowedBrainTarget()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean aoemod$hasAllowedBrainTarget() {
        Zoglin zoglin = (Zoglin) (Object) this;
        Optional<LivingEntity> target = zoglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return target.isPresent() && !aoemod$isNotAllowedMobTarget(zoglin, target.get());
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
