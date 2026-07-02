package net.amurdza.examplemod.entity.future;

import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class ZombieHorsemanEntity extends ZombieHorse {
    private static final String RIDER_TAG = "aoe.zombieHorsemanRider";

    public ZombieHorsemanEntity(EntityType<? extends ZombieHorse> type, Level level) {
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
            Zombie rider = EntityType.ZOMBIE.spawn(level.getLevel(), this.blockPosition(), MobSpawnType.MOB_SUMMONED);
            if (rider != null) {
                rider.addTag(RIDER_TAG);
                rider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SPEAR.get()));
                rider.startRiding(this, true);
            }
        }
        return result;
    }
}
