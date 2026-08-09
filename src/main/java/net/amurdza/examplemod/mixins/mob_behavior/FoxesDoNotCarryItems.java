package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Fox.class)
public abstract class FoxesDoNotCarryItems {
    @Inject(method = "canTakeItem", at = @At("HEAD"), cancellable = true)
    private void aoemod$preventItemPickup(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void aoemod$clearSpawnItem(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag tag,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        ((Fox) (Object) this).setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }
}
