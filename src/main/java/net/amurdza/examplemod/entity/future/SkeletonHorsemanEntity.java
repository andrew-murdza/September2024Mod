package net.amurdza.examplemod.entity.future;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class SkeletonHorsemanEntity extends SkeletonHorse {
    private static final String RIDER_TAG = "aoe.skeletonHorsemanRider";

    public SkeletonHorsemanEntity(EntityType<? extends SkeletonHorse> type, Level level) {
        super(type, level);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag tag
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        if (!this.level().isClientSide && !this.isVehicle()) {
            Skeleton rider = EntityType.SKELETON.create(level.getLevel());
            if (rider != null) {
                rider.addTag(RIDER_TAG);
                rider.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                rider.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null, null);
                rider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                // The mount's spawn call adds its passengers after finalizeSpawn returns.
                rider.startRiding(this, true);
            }
        }
        return result;
    }
}
