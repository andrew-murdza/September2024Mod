package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(WitherBoss.class)
public abstract class WitherSummons {
    @Unique
    private static final String AOEMOD_SUMMON_COOLDOWN = "AoeWitherSummonCooldown";
    @Unique
    private static final String AOEMOD_SUMMONER = "AoeWitherSummoner";

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void aoemod$summonSkeletons(CallbackInfo ci) {
        WitherBoss wither = (WitherBoss) (Object) this;
        if (!(wither.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = wither.getTarget();
        CompoundTag data = wither.getPersistentData();
        int cooldown = data.contains(AOEMOD_SUMMON_COOLDOWN)
                ? data.getInt(AOEMOD_SUMMON_COOLDOWN)
                : 300 + wither.getRandom().nextInt(201);

        if (target == null || !target.isAlive() || wither.getInvulnerableTicks() > 0) {
            data.putInt(AOEMOD_SUMMON_COOLDOWN, cooldown);
            return;
        }
        if (--cooldown > 0) {
            data.putInt(AOEMOD_SUMMON_COOLDOWN, cooldown);
            return;
        }

        UUID owner = wither.getUUID();
        AABB bounds = wither.getBoundingBox().inflate(64.0D);
        int active = level.getEntitiesOfClass(
                AbstractSkeleton.class,
                bounds,
                skeleton -> skeleton.getPersistentData().hasUUID(AOEMOD_SUMMONER)
                        && owner.equals(skeleton.getPersistentData().getUUID(AOEMOD_SUMMONER))
        ).size();

        int amount = Math.min(1 + wither.getRandom().nextInt(2), 6 - active);
        for (int i = 0; i < amount; ++i) {
            int x = wither.blockPosition().getX() + wither.getRandom().nextInt(13) - 6;
            int z = wither.blockPosition().getZ() + wither.getRandom().nextInt(13) - 6;
            BlockPos pos = null;
            // Heightmaps point above the entire cave. Search the nearby room floor instead.
            for (int y = wither.blockPosition().getY() + 4; y >= wither.blockPosition().getY() - 16; y--) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), net.minecraft.core.Direction.UP)
                        && level.isEmptyBlock(candidate) && level.isEmptyBlock(candidate.above())
                        && level.isEmptyBlock(candidate.above(2))) {
                    pos = candidate;
                    break;
                }
            }
            if (pos == null) continue;
            EntityType<? extends AbstractSkeleton> type = EntityType.WITHER_SKELETON;
            AbstractSkeleton minion = type.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
            if (minion != null) {
                minion.getPersistentData().putUUID(AOEMOD_SUMMONER, owner);
                minion.setTarget(target);
            }
        }
        data.putInt(AOEMOD_SUMMON_COOLDOWN, 300 + wither.getRandom().nextInt(201));
    }
}
