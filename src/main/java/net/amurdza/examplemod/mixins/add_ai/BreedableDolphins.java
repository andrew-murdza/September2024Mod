package net.amurdza.examplemod.mixins.add_ai;

import net.amurdza.examplemod.future.FutureMobCompatibility;

import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.entity.ai.DolphinBreedGoal;
import net.amurdza.examplemod.entity.ai.DolphinBreedingAccess;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Dolphin.class)
public abstract class BreedableDolphins extends WaterAnimal implements DolphinBreedingAccess {
    @Unique
    private static final EntityDataAccessor<Integer> AOE_AGE = SynchedEntityData.defineId(
            Dolphin.class,
            EntityDataSerializers.INT
    );
    @Unique
    private int aoe$age;
    @Unique
    private int aoe$inLove;
    @Unique
    @Nullable
    private UUID aoe$loveCause;
    @Unique
    private boolean aoe$ageDataDefined;

    protected BreedableDolphins(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void aoe$defineAgeData(CallbackInfo callback) {
        this.entityData.define(AOE_AGE, 0);
        this.aoe$ageDataDefined = true;
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void aoe$addBreedingGoal(CallbackInfo callback) {
        this.goalSelector.addGoal(1, new DolphinBreedGoal((Dolphin) (Object) this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(
                this,
                1.0D,
                Ingredient.of(DolphinBreedingAccess.BREEDING_FOOD),
                false
        ));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aoe$saveBreedingData(CompoundTag tag, CallbackInfo callback) {
        tag.putInt("AoeAge", this.aoe$age);
        tag.putInt("AoeInLove", this.aoe$inLove);
        if (this.aoe$loveCause != null) {
            tag.putUUID("AoeLoveCause", this.aoe$loveCause);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aoe$loadBreedingData(CompoundTag tag, CallbackInfo callback) {
        this.aoe$setAge(tag.getInt("AoeAge"));
        this.aoe$inLove = Math.max(0, tag.getInt("AoeInLove"));
        this.aoe$loveCause = tag.hasUUID("AoeLoveCause") ? tag.getUUID("AoeLoveCause") : null;
        if (this.aoe$age != 0) {
            this.aoe$resetLove();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void aoe$tickBreedingData(CallbackInfo callback) {
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        int ageAmount = FutureMobCompatibility.isAgeLocked(this) ? 0
                : Helper.computeIncrements(this.random, MobConfig.mobGrowthChance(this));
        if (this.aoe$age < 0) {
            this.aoe$setAgeWithoutImmediateSync(Math.min(0, this.aoe$age + ageAmount));
        } else if (this.aoe$age > 0) {
            this.aoe$setAgeWithoutImmediateSync(Math.max(0, this.aoe$age - ageAmount));
        }

        if (this.aoe$age != 0) {
            this.aoe$resetLove();
        } else if (this.aoe$inLove > 0) {
            --this.aoe$inLove;
            if (this.aoe$inLove % 10 == 0) {
                this.level().broadcastEntityEvent(this, (byte) 18);
            }
            if (this.aoe$inLove == 0) {
                this.aoe$loveCause = null;
            }
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void aoe$feedForBreeding(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(DolphinBreedingAccess.BREEDING_FOOD)) {
            return;
        }

        if (this.aoe$isBaby()) {
            if (FutureMobCompatibility.isAgeLocked(this) || MobConfig.mobGrowthChance(this) == 0.0F) {
                callback.setReturnValue(InteractionResult.PASS);
                return;
            }
            if (!this.level().isClientSide) {
                this.aoe$consumeFood(player, stack);
                int growthSeconds = (int) ((float) (-this.aoe$age / 20) * 0.1F);
                int growth = growthSeconds * Mth.ceil(20.0F * MobConfig.mobGrowthChance(this));
                this.aoe$setAge(Math.min(0, this.aoe$age + growth));
                this.playSound(SoundEvents.DOLPHIN_EAT, 1.0F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte) 19);
            }
            callback.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide));
        } else if (this.aoe$getAge() == 0 && !this.aoe$isInLove()) {
            if (MobConfig.mobTwinsChance(this) == 0.0F) {
                callback.setReturnValue(InteractionResult.FAIL);
                return;
            }
            if (!this.level().isClientSide) {
                this.aoe$consumeFood(player, stack);
                this.aoe$setInLove(player);
                this.playSound(SoundEvents.DOLPHIN_EAT, 1.0F, 1.0F);
            }
            callback.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide));
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void aoe$handleBreedingParticles(byte eventId, CallbackInfo callback) {
        if (eventId != 18 && eventId != 19) {
            return;
        }

        for (int i = 0; i < 7; ++i) {
            double xSpeed = this.random.nextGaussian() * 0.02D;
            double ySpeed = this.random.nextGaussian() * 0.02D;
            double zSpeed = this.random.nextGaussian() * 0.02D;
            this.level().addParticle(
                    eventId == 18 ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER,
                    this.getRandomX(1.0D),
                    this.getRandomY() + 0.5D,
                    this.getRandomZ(1.0D),
                    xSpeed,
                    ySpeed,
                    zSpeed
            );
        }
        callback.cancel();
    }

    @Override
    public int aoe$getAge() {
        return this.level().isClientSide && this.aoe$ageDataDefined
                ? this.entityData.get(AOE_AGE)
                : this.aoe$age;
    }

    @Override
    public void aoe$setAge(int age) {
        boolean wasBaby = this.aoe$isBaby();
        this.aoe$age = age;
        this.entityData.set(AOE_AGE, age);
        if (wasBaby != (age < 0)) {
            this.refreshDimensions();
        }
    }

    @Unique
    private void aoe$setAgeWithoutImmediateSync(int age) {
        boolean wasBaby = this.aoe$age < 0;
        this.aoe$age = age;
        if (age == 0 || this.tickCount % 20 == 0) {
            this.entityData.set(AOE_AGE, age);
        }
        if (wasBaby != (age < 0)) {
            this.refreshDimensions();
        }
    }

    @Override
    public boolean aoe$isBaby() {
        return this.aoe$getAge() < 0;
    }

    @Override
    public boolean isBaby() {
        return this.aoe$isBaby();
    }

    @Override
    public void setBaby(boolean baby) {
        this.aoe$setAge(baby ? DolphinBreedingAccess.BABY_START_AGE : 0);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDimensions(pose);
        return this.aoe$isBaby() ? dimensions.scale(0.75F) : dimensions;
    }

    @Override
    public boolean aoe$isInLove() {
        return this.aoe$inLove > 0;
    }

    @Override
    public void aoe$setInLove(@Nullable Player player) {
        this.aoe$inLove = 600;
        this.aoe$loveCause = player == null ? null : player.getUUID();
        this.level().broadcastEntityEvent(this, (byte) 18);
    }

    @Override
    public void aoe$resetLove() {
        this.aoe$inLove = 0;
        this.aoe$loveCause = null;
    }

    @Override
    public boolean aoe$canMate(Dolphin other) {
        if (other == (Object) this || !(other instanceof DolphinBreedingAccess otherBreeding)) {
            return false;
        }
        return this.aoe$getAge() == 0
                && otherBreeding.aoe$getAge() == 0
                && this.aoe$isInLove()
                && otherBreeding.aoe$isInLove();
    }

    @Override
    @Nullable
    public ServerPlayer aoe$getLoveCause() {
        if (this.aoe$loveCause == null) {
            return null;
        }
        Player player = this.level().getPlayerByUUID(this.aoe$loveCause);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (AOE_AGE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Unique
    private void aoe$consumeFood(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
