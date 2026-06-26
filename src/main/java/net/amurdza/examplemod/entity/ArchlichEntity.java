package net.amurdza.examplemod.entity;

import com.github.alexthe666.iceandfire.entity.EntityDreadKnight;
import com.github.alexthe666.iceandfire.entity.EntityDreadLich;
import com.github.alexthe666.iceandfire.entity.EntityDreadLichSkull;
import com.github.alexthe666.iceandfire.entity.EntityDreadMob;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class ArchlichEntity extends EntityDreadLich {
    private static final String SUMMON_COOLDOWN_TAG = "ArchlichSummonCooldown";
    private static final String ATTACK_COOLDOWN_TAG = "ArchlichAttackCooldown";
    private static final int MAX_ACTIVE_SUMMONS = 4;
    private static final int SUMMON_RANGE = 64;
    private static final int MIN_SUMMON_COOLDOWN = 300;
    private static final int SUMMON_COOLDOWN_VARIANCE = 100;
    private static final int RANGED_ATTACK_COOLDOWN = 40;

    private int summonCooldown;
    private int rangedAttackCooldown;

    public ArchlichEntity(EntityType<? extends EntityDreadMob> entityType, Level level) {
        super(entityType, level);
        this.summonCooldown = this.nextSummonCooldown();
        this.xpReward = 35;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.rangedAttackCooldown > 0) {
            --this.rangedAttackCooldown;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.summonCooldown > 0) {
            --this.summonCooldown;
        }

        if (this.summonCooldown <= 0) {
            this.trySummonDreadMinion(serverLevel, target);
            this.summonCooldown = this.nextSummonCooldown();
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.rangedAttackCooldown > 0 || this.level().isClientSide) {
            return;
        }

        EntityDreadLichSkull skull = new EntityDreadLichSkull(
                IafEntityRegistry.DREAD_LICH_SKULL.get(),
                this.level(),
                this,
                8.0D
        );
        double xPower = target.getX() - this.getX();
        double yPower = target.getBoundingBox().minY + target.getBbHeight() * 2.0F - skull.getY();
        double zPower = target.getZ() - this.getZ();
        double horizontalDistance = Math.sqrt(xPower * xPower + zPower * zPower);
        skull.shoot(
                xPower,
                yPower + horizontalDistance * 0.2D,
                zPower,
                0.0F,
                14 - this.level().getDifficulty().getId() * 4
        );

        this.swing(this.getUsedItemHand());
        this.playSound(SoundEvents.ZOMBIE_INFECT, 1.0F, this.getVoicePitch());
        this.level().addFreshEntity(skull);
        this.rangedAttackCooldown = RANGED_ATTACK_COOLDOWN;
    }

    private void trySummonDreadMinion(ServerLevel level, LivingEntity target) {
        if (this.countActiveSummons(level) >= MAX_ACTIVE_SUMMONS) {
            return;
        }

        BlockPos spawnPos = this.findSummonPosition(level);
        EntityDreadMob minion;
        if (this.random.nextBoolean()) {
            minion = IafEntityRegistry.DREAD_LICH.get().spawn(level, spawnPos, MobSpawnType.MOB_SUMMONED);
            if (minion instanceof EntityDreadLich lich) {
                // A summoned lich fights normally but cannot create another layer of minions.
                lich.setMinionCount(5);
            }
        } else {
            minion = IafEntityRegistry.DREAD_KNIGHT.get().spawn(level, spawnPos, MobSpawnType.MOB_SUMMONED);
        }

        if (minion == null) {
            return;
        }

        minion.setCommanderId(this.getUUID());
        minion.setTarget(target);
        this.setAnimation(ANIMATION_SUMMON);
        this.playSound(IafSoundRegistry.DREAD_LICH_SUMMON, 1.0F, this.getVoicePitch());
    }

    private int countActiveSummons(ServerLevel level) {
        UUID commanderId = this.getUUID();
        AABB searchArea = this.getBoundingBox().inflate(SUMMON_RANGE);
        return level.getEntitiesOfClass(
                EntityDreadMob.class,
                searchArea,
                minion -> commanderId.equals(minion.getCommanderId())
        ).size();
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
        tag.putInt(SUMMON_COOLDOWN_TAG, this.summonCooldown);
        tag.putInt(ATTACK_COOLDOWN_TAG, this.rangedAttackCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(SUMMON_COOLDOWN_TAG)) {
            this.summonCooldown = Math.max(0, tag.getInt(SUMMON_COOLDOWN_TAG));
        }
        if (tag.contains(ATTACK_COOLDOWN_TAG)) {
            this.rangedAttackCooldown = Math.max(0, tag.getInt(ATTACK_COOLDOWN_TAG));
        }
    }
}
