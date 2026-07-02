package net.amurdza.examplemod.entity.future;

import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BreezeEntity extends Monster implements RangedAttackMob {
    private int jumpCooldown;

    public BreezeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.jumpCooldown = 40 + this.random.nextInt(40);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.63D)
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 40, 16.0F));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.getTarget() != null && this.onGround()) {
            if (--this.jumpCooldown <= 0) {
                LivingEntity target = this.getTarget();
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
                this.setDeltaMovement(dx / length * 0.55D, 0.55D, dz / length * 0.55D);
                this.hasImpulse = true;
                this.playSound(SoundEvents.BLAZE_SHOOT, 0.8F, 1.5F);
                this.jumpCooldown = 50 + this.random.nextInt(50);
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - this.getY(0.5D);
        double dz = target.getZ() - this.getZ();
        BreezeWindChargeEntity charge = new BreezeWindChargeEntity(
                ModEntities.BREEZE_WIND_CHARGE.get(),
                this,
                dx,
                dy,
                dz,
                this.level()
        );
        charge.setPos(this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ());
        this.level().addFreshEntity(charge);
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.7F);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }
}
