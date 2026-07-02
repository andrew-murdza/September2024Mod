package net.amurdza.examplemod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SpiderQueenEntity extends Spider {
    private static final String SUMMON_COOLDOWN_TAG = "SpiderQueenSummonCooldown";
    private static final String SUMMONER_TAG = "SpiderQueenSummoner";
    private static final int MAX_ACTIVE_SUMMONS = 6;
    private static final int MIN_SUMMON_COOLDOWN = 240;
    private static final int SUMMON_COOLDOWN_VARIANCE = 120;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );
    private int summonCooldown;

    public SpiderQueenEntity(EntityType<? extends Spider> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 40;
        this.summonCooldown = this.nextSummonCooldown();
        this.bossEvent.setDarkenScreen(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0), this);
            livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1), this);
            livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2), this);
        }
        return hit;
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
            this.summonSpiders(level, target);
            this.summonCooldown = this.nextSummonCooldown();
        }
    }

    private void summonSpiders(ServerLevel level, LivingEntity target) {
        UUID owner = this.getUUID();
        AABB bounds = this.getBoundingBox().inflate(48.0D);
        int active = level.getEntitiesOfClass(
                Spider.class,
                bounds,
                spider -> spider.getPersistentData().hasUUID(SUMMONER_TAG)
                        && owner.equals(spider.getPersistentData().getUUID(SUMMONER_TAG))
        ).size();
        int amount = Math.min(2 + this.random.nextInt(2), MAX_ACTIVE_SUMMONS - active);

        for (int i = 0; i < amount; ++i) {
            EntityType<? extends Spider> type = this.random.nextBoolean()
                    ? EntityType.SPIDER
                    : EntityType.CAVE_SPIDER;
            Spider minion = type.spawn(level, this.findSummonPosition(level), MobSpawnType.MOB_SUMMONED);
            if (minion != null) {
                minion.getPersistentData().putUUID(SUMMONER_TAG, owner);
                minion.setTarget(target);
            }
        }
    }

    private BlockPos findSummonPosition(ServerLevel level) {
        int x = this.blockPosition().getX() + this.random.nextInt(11) - 5;
        int z = this.blockPosition().getZ() + this.random.nextInt(11) - 5;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private int nextSummonCooldown() {
        return MIN_SUMMON_COOLDOWN + this.random.nextInt(SUMMON_COOLDOWN_VARIANCE + 1);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag tag
    ) {
        this.summonCooldown = this.nextSummonCooldown();
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(SUMMON_COOLDOWN_TAG, this.summonCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(SUMMON_COOLDOWN_TAG)) {
            this.summonCooldown = Math.max(0, tag.getInt(SUMMON_COOLDOWN_TAG));
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
