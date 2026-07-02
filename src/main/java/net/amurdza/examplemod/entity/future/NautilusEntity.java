package net.amurdza.examplemod.entity.future;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class NautilusEntity extends WaterAnimal {
    public NautilusEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.02F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isVehicle() && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isVehicle() && passenger instanceof Player;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    public void travel(Vec3 input) {
        LivingEntity controller = this.getControllingPassenger();
        if (this.isAlive() && controller instanceof Player player) {
            this.setYRot(player.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(player.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;

            float forward = player.zza;
            float strafe = player.xxa * 0.5F;
            float pitchRadians = player.getXRot() * ((float) Math.PI / 180.0F);
            Vec3 riddenInput = new Vec3(
                    strafe,
                    -Math.sin(pitchRadians) * Math.abs(forward),
                    Math.cos(pitchRadians) * forward
            );
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.1F);
            this.moveRelative(this.getSpeed(), riddenInput);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            this.calculateEntityAnimation(false);
            return;
        }

        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            return;
        }
        super.travel(input);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.getFirstPassenger() instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, true));
        }
        if (this.isInWater() && this.random.nextFloat() < 0.3F) {
            this.level().addParticle(
                    ParticleTypes.BUBBLE,
                    this.getX(),
                    this.getY() + 0.25D,
                    this.getZ(),
                    0.0D,
                    0.02D,
                    0.0D
            );
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DOLPHIN_AMBIENT_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }
}
