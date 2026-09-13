package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.entity.ai.PolarBearTamingAccess;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(PolarBear.class)
public abstract class PassiveRideablePolarBears extends Animal implements OwnableEntity, PolarBearTamingAccess {
    @Unique
    private static final float AOE_WOLF_ATTACK_DAMAGE = 4.0F;

    @Unique
    private static final EntityDataAccessor<Optional<UUID>> AOE_OWNER_UUID = SynchedEntityData.defineId(
            PolarBear.class,
            EntityDataSerializers.OPTIONAL_UUID
    );

    protected PassiveRideablePolarBears(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void aoe$defineTamingData(CallbackInfo callback) {
        this.entityData.define(AOE_OWNER_UUID, Optional.empty());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aoe$saveTamingData(CompoundTag tag, CallbackInfo callback) {
        UUID ownerUUID = this.aoe$getOwnerUUID();
        if (ownerUUID != null) {
            tag.putUUID("AoePolarBearOwner", ownerUUID);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aoe$loadTamingData(CompoundTag tag, CallbackInfo callback) {
        if (tag.hasUUID("AoePolarBearOwner")) {
            this.aoe$setOwnerUUID(tag.getUUID("AoePolarBearOwner"));
        } else if (tag.hasUUID("Owner")) {
            this.aoe$setOwnerUUID(tag.getUUID("Owner"));
        }
    }

    @Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
    private void aoe$useMobMeatForBreeding(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (this.aoe$isTame() && stack.is(ModTags.Items.mobMeatDrops)) {
            callback.setReturnValue(true);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.aoe$isTame() && stack.is(ModTags.Items.rawFish)) {
            if (!this.level().isClientSide) {
                this.usePlayerItem(player, hand, stack);
                PolarBear polarBear = (PolarBear) (Object) this;
                if (!ForgeEventFactory.onAnimalTame(polarBear, player)) {
                    this.aoe$setOwnerUUID(player.getUUID());
                    this.getNavigation().stop();
                    this.setTarget(null);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                    if (player instanceof ServerPlayer serverPlayer) {
                        CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, polarBear);
                    }
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.aoe$isOwnedBy(player)
                && !this.isBaby()
                && !this.isVehicle()
                && stack.isEmpty()
                && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Inject(method = "getBreedOffspring", at = @At("RETURN"))
    private void aoe$inheritTamingOwner(
            ServerLevel level,
            AgeableMob otherParent,
            CallbackInfoReturnable<AgeableMob> callback
    ) {
        if (!(callback.getReturnValue() instanceof PolarBearTamingAccess offspring)) {
            return;
        }

        UUID ownerUUID = this.aoe$getOwnerUUID();
        if (ownerUUID == null && otherParent instanceof PolarBearTamingAccess otherBear) {
            ownerUUID = otherBear.aoe$getOwnerUUID();
        }
        offspring.aoe$setOwnerUUID(ownerUUID);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void aoe$useWolfDamageForOwnerTarget(Entity target, CallbackInfoReturnable<Boolean> callback) {
        if (!(target instanceof LivingEntity livingTarget) || !this.aoe$canAttackOwnerTarget(livingTarget)) {
            callback.setReturnValue(false);
            return;
        }

        boolean damaged = target.hurt(this.damageSources().mobAttack(this), AOE_WOLF_ATTACK_DAMAGE);
        if (damaged) {
            this.doEnchantDamageEffects(this, target);
        }
        callback.setReturnValue(damaged);
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId != 6 && eventId != 7) {
            super.handleEntityEvent(eventId);
            return;
        }

        for (int i = 0; i < 7; ++i) {
            this.level().addParticle(
                    eventId == 7 ? ParticleTypes.HEART : ParticleTypes.SMOKE,
                    this.getRandomX(1.0D),
                    this.getRandomY() + 0.5D,
                    this.getRandomZ(1.0D),
                    this.random.nextGaussian() * 0.02D,
                    this.random.nextGaussian() * 0.02D,
                    this.random.nextGaussian() * 0.02D
            );
        }
    }

    @Override
    public boolean aoe$isTame() {
        return this.aoe$getOwnerUUID() != null;
    }

    @Override
    @Nullable
    public UUID aoe$getOwnerUUID() {
        return this.entityData.get(AOE_OWNER_UUID).orElse(null);
    }

    @Override
    public void aoe$setOwnerUUID(@Nullable UUID ownerUUID) {
        this.entityData.set(AOE_OWNER_UUID, Optional.ofNullable(ownerUUID));
    }

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return this.aoe$getOwnerUUID();
    }

    @Override
    public boolean aoe$isOwnedBy(Player player) {
        UUID ownerUUID = this.aoe$getOwnerUUID();
        return ownerUUID != null && ownerUUID.equals(player.getUUID());
    }

    @Override
    public boolean aoe$canAttackOwnerTarget(@Nullable LivingEntity target) {
        if (!this.aoe$isTame() || target == null || !target.isAlive() || target == (Object) this) {
            return false;
        }

        LivingEntity owner = this.getOwner();
        if (owner == null || target == owner || owner.isAlliedTo(target)) {
            return false;
        }

        if (target instanceof Creeper || target instanceof Ghast) {
            return false;
        }

        if (target instanceof Player targetPlayer
                && owner instanceof Player ownerPlayer
                && !ownerPlayer.canHarmPlayer(targetPlayer)) {
            return false;
        }

        if (target instanceof AbstractHorse horse && horse.isTamed()) {
            return false;
        }

        if (target instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            return false;
        }

        return !(target instanceof OwnableEntity ownableTarget) || ownableTarget.getOwner() != owner;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return this.aoe$canAttackOwnerTarget(target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        UUID ownerUUID = this.aoe$getOwnerUUID();
        if (ownerUUID != null && ownerUUID.equals(entity.getUUID())) {
            return true;
        }

        LivingEntity owner = this.getOwner();
        return owner != null ? owner.isAlliedTo(entity) : super.isAlliedTo(entity);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isBaby()
                && !this.isVehicle()
                && passenger instanceof Player player
                && this.aoe$isOwnedBy(player);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player && this.aoe$isOwnedBy(player)
                ? player
                : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(strafe, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }
}
