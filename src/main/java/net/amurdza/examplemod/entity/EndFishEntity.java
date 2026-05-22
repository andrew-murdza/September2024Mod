package net.amurdza.examplemod.entity;

import net.amurdza.examplemod.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public class EndFishEntity extends AbstractSchoolingFish implements VariantHolder<EndFishEntity.Variant> {
    public static final int VARIANTS_NORMAL = 5;
    public static final int VARIANTS_SULPHUR = 3;
    public static final int VARIANTS = VARIANTS_NORMAL + VARIANTS_SULPHUR;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(
            EndFishEntity.class,
            EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Byte> SCALE = SynchedEntityData.defineId(
            EndFishEntity.class,
            EntityDataSerializers.BYTE
    );

    public EndFishEntity(EntityType<EndFishEntity> entityType, Level world) {
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

        this.setVariant(Variant.byId(this.random.nextInt(VARIANTS_SULPHUR) + VARIANTS_NORMAL));

        if (entityTag != null) {
            if (entityTag.contains("Variant")) {
                this.setVariant(Variant.byId(entityTag.getInt("Variant")));
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
        this.entityData.define(VARIANT, Variant.byId(this.getRandom().nextInt(VARIANTS_NORMAL)).id());
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
            this.setVariant(Variant.byId(tag.getInt("Variant")));
        }

        if (tag.contains("Scale")) {
            this.entityData.set(SCALE, sanitizeScale(tag.getInt("Scale")));
        }

        this.refreshDimensions();
    }

    @Override
    public @NotNull ItemStack getBucketItemStack() {
        ItemStack bucket = ModItems.BUCKET_END_FISH.get().getDefaultInstance();
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
    public @NotNull Variant getVariant() {
        return Variant.byId(this.entityData.get(VARIANT));
    }

    @Override
    public void setVariant(Variant variant) {
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

    public enum Variant {
        NORMAL_0(0),
        NORMAL_1(1),
        NORMAL_2(2),
        NORMAL_3(3),
        NORMAL_4(4),
        SULPHUR_0(5),
        SULPHUR_1(6),
        SULPHUR_2(7);

        private static final IntFunction<Variant> BY_ID = ByIdMap.sparse(
                Variant::id,
                values(),
                NORMAL_0
        );

        private final int id;

        Variant(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        public static Variant byId(int id) {
            return BY_ID.apply(id);
        }
    }
}