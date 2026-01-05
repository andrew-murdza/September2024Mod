package net.amurdza.examplemod.entity;

import net.amurdza.examplemod.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class CubozoaEntity extends AbstractSchoolingFish {
    public static final int VARIANTS = 2;
    private static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(
            CubozoaEntity.class,
            EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Byte> SCALE = SynchedEntityData.defineId(
            CubozoaEntity.class,
            EntityDataSerializers.BYTE
    );

    public CubozoaEntity(EntityType<CubozoaEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, SpawnGroupData entityData, CompoundTag entityTag) {
        SpawnGroupData data = super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityTag);

        if (entityTag != null) {
            if (entityTag.contains("Variant")) {
                this.entityData.set(VARIANT, entityTag.getByte("Variant"));
            }
            if (entityTag.contains("Scale")) {
                this.entityData.set(SCALE, entityTag.getByte("Scale"));
            }
        }

        this.refreshDimensions();
        return data;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(SCALE, (byte) this.getRandom().nextInt(16));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", (byte) getVariant());
        tag.putByte("Scale", getByteScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            this.entityData.set(VARIANT, tag.getByte("Variant"));
        }
        if (tag.contains("Scale")) {
            this.entityData.set(SCALE, tag.getByte("Scale"));
        }
    }

    @Override
    public ItemStack getBucketItemStack() {
        ItemStack bucket = ModItems.BUCKET_CUBOZOA.get().getDefaultInstance();
        CompoundTag tag = bucket.getOrCreateTag();
        tag.putByte("Variant", entityData.get(VARIANT));
        tag.putByte("Scale", entityData.get(SCALE));
        return bucket;
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return LivingEntity
                .createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    public int getVariant() {
        return (int) this.entityData.get(VARIANT);
    }

    public byte getByteScale() {
        return this.entityData.get(SCALE);
    }

    public float getScale() {
        return getByteScale() / 32F + 0.75F;
    }

    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5F;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.SALMON_FLOP;
    }
}
