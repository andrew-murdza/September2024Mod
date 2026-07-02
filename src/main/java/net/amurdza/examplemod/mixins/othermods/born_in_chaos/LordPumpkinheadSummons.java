package net.amurdza.examplemod.mixins.othermods.born_in_chaos;

import net.mcreator.borninchaosv.entity.LordPumpkinheadEntity;
import net.mcreator.borninchaosv.init.BornInChaosV1ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = LordPumpkinheadEntity.class, remap = false)
public abstract class LordPumpkinheadSummons {
    @Unique
    private static final String AOEMOD_SUMMON_COOLDOWN = "AoePumpkinLordSummonCooldown";
    @Unique
    private static final String AOEMOD_SUMMONER = "AoePumpkinLordSummoner";
    @Unique
    private static final String AOEMOD_MOUNT = "AoePumpkinLordMount";

    @Inject(method = "baseTick", at = @At("TAIL"), remap = false)
    private void aoemod$summonSirAndSenor(CallbackInfo ci) {
        LordPumpkinheadEntity boss = (LordPumpkinheadEntity) (Object) this;
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }

        this.aoemod$equipAndMountBoss(boss, level);

        LivingEntity target = boss.getTarget();
        CompoundTag data = boss.getPersistentData();
        int cooldown = data.contains(AOEMOD_SUMMON_COOLDOWN)
                ? data.getInt(AOEMOD_SUMMON_COOLDOWN)
                : 260 + boss.getRandom().nextInt(141);

        if (target == null || !target.isAlive()) {
            data.putInt(AOEMOD_SUMMON_COOLDOWN, cooldown);
            return;
        }
        if (--cooldown > 0) {
            data.putInt(AOEMOD_SUMMON_COOLDOWN, cooldown);
            return;
        }

        UUID owner = boss.getUUID();
        AABB bounds = boss.getBoundingBox().inflate(48.0D);
        int active = level.getEntitiesOfClass(
                Mob.class,
                bounds,
                mob -> mob.getPersistentData().hasUUID(AOEMOD_SUMMONER)
                        && owner.equals(mob.getPersistentData().getUUID(AOEMOD_SUMMONER))
        ).size();
        int amount = Math.min(2, 6 - active);

        for (int i = 0; i < amount; ++i) {
            int x = boss.blockPosition().getX() + boss.getRandom().nextInt(11) - 5;
            int z = boss.blockPosition().getZ() + boss.getRandom().nextInt(11) - 5;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            EntityType<?> type = i % 2 == 0
                    ? BornInChaosV1ModEntities.SIR_PUMPKINHEAD.get()
                    : BornInChaosV1ModEntities.SENOR_PUMPKIN.get();
            Entity summoned = type.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
            if (summoned instanceof Mob minion) {
                minion.getPersistentData().putUUID(AOEMOD_SUMMONER, owner);
                minion.setTarget(target);
            }
        }
        data.putInt(AOEMOD_SUMMON_COOLDOWN, 260 + boss.getRandom().nextInt(141));
    }

    @Unique
    private void aoemod$equipAndMountBoss(LordPumpkinheadEntity boss, ServerLevel level) {
        boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        boss.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        if (boss.isPassenger()) {
            return;
        }

        SkeletonHorse horse = EntityType.SKELETON_HORSE.spawn(level, boss.blockPosition(), MobSpawnType.MOB_SUMMONED);
        if (horse == null) {
            return;
        }

        horse.getPersistentData().putBoolean(AOEMOD_MOUNT, true);
        horse.setPersistenceRequired();
        horse.setTamed(true);
        boss.startRiding(horse, true);
    }
}
