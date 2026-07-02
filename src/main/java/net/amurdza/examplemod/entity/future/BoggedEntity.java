package net.amurdza.examplemod.entity.future;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BoggedEntity extends AbstractSkeleton {
    public BoggedEntity(EntityType<? extends AbstractSkeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 16.0D);
    }

    @Override
    protected AbstractArrow getArrow(ItemStack arrowStack, float power) {
        AbstractArrow projectile = super.getArrow(arrowStack, power);
        if (projectile instanceof Arrow arrow) {
            arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        }
        return projectile;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRAY_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.STRAY_STEP;
    }
}
