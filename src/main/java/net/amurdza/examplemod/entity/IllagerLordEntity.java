package net.amurdza.examplemod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class IllagerLordEntity extends Vindicator {
    private static final EntityDataAccessor<Integer> SHIELD_HITS = SynchedEntityData.defineId(
            IllagerLordEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> SHIELD_BROKEN = SynchedEntityData.defineId(
            IllagerLordEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final String SUMMON_COOLDOWN_TAG = "IllagerLordSummonCooldown";
    private static final String SUMMONER_TAG = "IllagerLordSummoner";
    private static final int SHIELD_BREAK_HITS = 30;
    private static final int MAX_ACTIVE_SUMMONS = 6;
    private static final int MIN_SUMMON_COOLDOWN = 300;
    private static final int SUMMON_COOLDOWN_VARIANCE = 120;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    );
    private int summonCooldown;

    public IllagerLordEntity(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 50;
        this.summonCooldown = this.nextSummonCooldown();
        this.equipIronLoadout();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes()
                .add(Attributes.MAX_HEALTH, 220.0D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHIELD_HITS, 0);
        this.entityData.define(SHIELD_BROKEN, false);
    }

    private void equipIronLoadout() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        if (!this.isShieldBroken()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isShieldBroken()
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.isAttackFromFront(source)) {
            int hits = this.entityData.get(SHIELD_HITS) + 1;
            this.entityData.set(SHIELD_HITS, hits);
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            if (hits >= SHIELD_BREAK_HITS) {
                this.breakShield();
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    private boolean isAttackFromFront(DamageSource source) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null && source.getEntity() != null) {
            sourcePosition = source.getEntity().position();
        }
        if (sourcePosition == null) {
            return false;
        }
        Vec3 toSource = new Vec3(
                sourcePosition.x - this.getX(),
                0.0D,
                sourcePosition.z - this.getZ()
        );
        if (toSource.lengthSqr() < 1.0E-6D) {
            return true;
        }
        Vec3 look = this.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z).normalize();
        return horizontalLook.dot(toSource.normalize()) >= 0.0D;
    }

    private void breakShield() {
        this.entityData.set(SHIELD_BROKEN, true);
        ItemStack shield = new ItemStack(Items.SHIELD);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.playSound(SoundEvents.SHIELD_BREAK, 1.25F, 0.8F);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, shield),
                    this.getX(),
                    this.getEyeY(),
                    this.getZ(),
                    24,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.1D
            );
        }
    }

    public boolean isShieldBroken() {
        return this.entityData.get(SHIELD_BROKEN);
    }

    public int getShieldHits() {
        return this.entityData.get(SHIELD_HITS);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setName(this.getDisplayName());
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.summonCooldown > 0) {
            --this.summonCooldown;
        }
        if (this.summonCooldown <= 0) {
            this.summonRaiders(level, target);
            this.summonCooldown = this.nextSummonCooldown();
        }
    }

    private void summonRaiders(ServerLevel level, LivingEntity target) {
        UUID owner = this.getUUID();
        AABB bounds = this.getBoundingBox().inflate(48.0D);
        int active = level.getEntitiesOfClass(
                AbstractIllager.class,
                bounds,
                illager -> illager.getPersistentData().hasUUID(SUMMONER_TAG)
                        && owner.equals(illager.getPersistentData().getUUID(SUMMONER_TAG))
        ).size();
        int amount = Math.min(2 + this.random.nextInt(2), MAX_ACTIVE_SUMMONS - active);

        for (int i = 0; i < amount; ++i) {
            EntityType<? extends AbstractIllager> type = this.random.nextBoolean()
                    ? EntityType.VINDICATOR
                    : EntityType.EVOKER;
            AbstractIllager minion = type.spawn(level, this.findSummonPosition(level), MobSpawnType.MOB_SUMMONED);
            if (minion != null) {
                minion.getPersistentData().putUUID(SUMMONER_TAG, owner);
                minion.setTarget(target);
            }
        }
    }

    private BlockPos findSummonPosition(ServerLevel level) {
        int x = this.blockPosition().getX() + this.random.nextInt(13) - 6;
        int z = this.blockPosition().getZ() + this.random.nextInt(13) - 6;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private int nextSummonCooldown() {
        return MIN_SUMMON_COOLDOWN + this.random.nextInt(SUMMON_COOLDOWN_VARIANCE + 1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("IllagerLordShieldHits", this.getShieldHits());
        tag.putBoolean("IllagerLordShieldBroken", this.isShieldBroken());
        tag.putInt(SUMMON_COOLDOWN_TAG, this.summonCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(SHIELD_HITS, Math.max(0, tag.getInt("IllagerLordShieldHits")));
        this.entityData.set(SHIELD_BROKEN, tag.getBoolean("IllagerLordShieldBroken"));
        this.summonCooldown = tag.contains(SUMMON_COOLDOWN_TAG)
                ? Math.max(0, tag.getInt(SUMMON_COOLDOWN_TAG))
                : this.nextSummonCooldown();
        this.equipIronLoadout();
        if (this.isShieldBroken()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
