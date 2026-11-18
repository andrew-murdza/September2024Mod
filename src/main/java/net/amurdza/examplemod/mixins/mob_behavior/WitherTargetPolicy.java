package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.util.WitherTargets;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(WitherBoss.class)
public abstract class WitherTargetPolicy extends Monster {
    @Shadow @Final @Mutable private static Predicate<LivingEntity> LIVING_ENTITY_SELECTOR;
    @Shadow @Final private static TargetingConditions TARGETING_CONDITIONS;

    protected WitherTargetPolicy(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void aoemod$selectOnlyIntendedTargets(CallbackInfo ci) {
        LIVING_ENTITY_SELECTOR = entity -> WitherTargets.allowed(entity) && entity.isAlive();
        TARGETING_CONDITIONS.selector(LIVING_ENTITY_SELECTOR);
    }

    @Override
    public boolean canAttack(LivingEntity entity) {
        return WitherTargets.allowed(entity) && super.canAttack(entity);
    }
}
