package net.amurdza.examplemod.entity.future;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CamelHuskEntity extends Camel {
    public CamelHuskEntity(EntityType<? extends Camel> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Camel.createAttributes();
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    @Override
    public @Nullable Camel getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }
}
