package net.amurdza.examplemod.entity.future;

import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class CamelHuskJockeyEntity extends CamelHuskEntity {
    public CamelHuskJockeyEntity(EntityType<? extends Camel> type, Level level) {
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
            Husk husk = EntityType.HUSK.spawn(level.getLevel(), this.blockPosition(), MobSpawnType.MOB_SUMMONED);
            if (husk != null) {
                husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOLDEN_SPEAR.get()));
                husk.startRiding(this, true);
            }
            ParchedEntity parched = ModEntities.PARCHED.get().spawn(
                    level.getLevel(),
                    this.blockPosition(),
                    MobSpawnType.MOB_SUMMONED
            );
            if (parched != null) {
                parched.startRiding(this, true);
            }
        }
        return result;
    }
}
