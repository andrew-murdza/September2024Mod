package net.amurdza.examplemod.entity.future;

import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

public class NautilusEntity extends TamableAnimal {
    private static final int TAMING_CHANCE = 3;

    public NautilusEntity(EntityType<? extends NautilusEntity> type, Level level) {
        super(type, level);
        // Match the vanilla Nautilus' gentle idle acceleration. The previous 0.1F
        // multiplier made an unridden Nautilus repeatedly overshoot its swim target.
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.011F, 0.02F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isBaby() && !this.isTame() && stack.is(Items.PUFFERFISH)) {
            if (!this.level().isClientSide) {
                this.usePlayerItem(player, hand, stack);
                if (this.random.nextInt(TAMING_CHANCE) == 0
                        && !ForgeEventFactory.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.navigation.stop();
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        InteractionResult foodResult = super.mobInteract(player, hand);
        if (foodResult.consumesAction()) {
            return foodResult;
        }

        if (!this.isBaby() && !this.isVehicle() && stack.isEmpty()) {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return foodResult;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return (this.isTame() || this.isBaby()) && stack.is(ModTags.Items.fish);
    }

    @Override
    public @Nullable NautilusEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        NautilusEntity offspring = ModEntities.NAUTILUS.get().create(level);
        if (offspring != null && this.isTame()) {
            offspring.setOwnerUUID(this.getOwnerUUID());
            offspring.setTame(true);
        }
        return offspring;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isBaby() && !this.isVehicle() && passenger instanceof Player;
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
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public MobType getMobType() {
        return MobType.WATER;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public int getExperienceReward() {
        return 1 + this.level().random.nextInt(3);
    }

    protected void handleAirSupply(int currentAirSupply) {
        if (this.isAlive() && !this.isInWaterOrBubble()) {
            this.setAirSupply(currentAirSupply - 1);
            if (this.getAirSupply() == -20) {
                this.setAirSupply(0);
                this.hurt(this.damageSources().dryOut(), 2.0F);
            }
        } else {
            this.setAirSupply(300);
        }
    }

    @Override
    public void baseTick() {
        int currentAirSupply = this.getAirSupply();
        super.baseTick();
        this.handleAirSupply(currentAirSupply);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    public static boolean checkSurfaceWaterAnimalSpawnRules(
            EntityType<? extends NautilusEntity> type,
            LevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        int seaLevel = level.getSeaLevel();
        return pos.getY() >= seaLevel - 13
                && pos.getY() <= seaLevel
                && level.getFluidState(pos.below()).is(FluidTags.WATER)
                && level.getBlockState(pos.above()).is(Blocks.WATER);
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
