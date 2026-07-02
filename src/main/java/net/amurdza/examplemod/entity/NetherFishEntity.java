package net.amurdza.examplemod.entity;

import com.scouter.netherdepthsupgrade.entity.AbstractLavaSchoolingFish;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

public class NetherFishEntity extends AbstractLavaSchoolingFish implements VariantHolder<EndFishEntity.Variant> {
    public static final int MIN_TEXTURE_VARIANT = 3;
    public static final int VARIANTS = 2;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(
            NetherFishEntity.class,
            EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Byte> SCALE = SynchedEntityData.defineId(
            NetherFishEntity.class,
            EntityDataSerializers.BYTE
    );

    public NetherFishEntity(EntityType<? extends NetherFishEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            @NotNull ServerLevelAccessor world,
            @NotNull DifficultyInstance difficulty,
            @NotNull MobSpawnType spawnReason,
            SpawnGroupData entityData,
            CompoundTag entityTag
    ) {
        SpawnGroupData data = super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityTag);

        if (entityTag == null || !entityTag.contains("Variant")) {
            this.setVariant(EndFishEntity.Variant.byId(MIN_TEXTURE_VARIANT + this.random.nextInt(VARIANTS)));
        }

        if (entityTag != null) {
            if (entityTag.contains("Variant")) {
                this.setVariant(EndFishEntity.Variant.byId(entityTag.getInt("Variant")));
            }

            if (entityTag.contains("Scale")) {
                this.entityData.set(SCALE, sanitizeScale(entityTag.getInt("Scale")));
            }
        }

        this.refreshDimensions();
        return data;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, EndFishEntity.Variant.byId(MIN_TEXTURE_VARIANT).id());
        this.entityData.define(SCALE, (byte) this.getRandom().nextInt(16));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id());
        tag.putByte("Scale", this.getByteScale());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Variant")) {
            this.setVariant(EndFishEntity.Variant.byId(tag.getInt("Variant")));
        }

        if (tag.contains("Scale")) {
            this.entityData.set(SCALE, sanitizeScale(tag.getInt("Scale")));
        }

        this.refreshDimensions();
    }

    @Override
    public @NotNull ItemStack getBucketItemStack() {
        ItemStack bucket = ModItems.BUCKET_NETHER_FISH.get().getDefaultInstance();
        CompoundTag tag = bucket.getOrCreateTag();

        tag.putInt("Variant", this.getVariant().id());
        tag.putByte("Scale", this.getByteScale());

        return bucket;
    }

    @Override
    protected @NotNull SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SALMON_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SALMON_HURT;
    }

    public static AttributeSupplier.@NotNull Builder createMobAttributes() {
        return LivingEntity
                .createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.75);
    }

    @Override
    public @NotNull EndFishEntity.Variant getVariant() {
        return EndFishEntity.Variant.byId(this.entityData.get(VARIANT));
    }

    @Override
    public void setVariant(EndFishEntity.Variant variant) {
        this.entityData.set(VARIANT, variant.id());
    }

    public int getVariantId() {
        return this.getVariant().id();
    }

    public byte getByteScale() {
        return this.entityData.get(SCALE);
    }

    public float getScale() {
        return getByteScale() / 32F + 0.75F;
    }

    private static byte sanitizeScale(int scale) {
        if (scale < 0 || scale > 15) {
            return 0;
        }

        return (byte) scale;
    }

    public int getNetherVariantIndex() {
        return Math.max(0, Math.min(VARIANTS - 1, this.getVariantId() - MIN_TEXTURE_VARIANT));
    }
}
