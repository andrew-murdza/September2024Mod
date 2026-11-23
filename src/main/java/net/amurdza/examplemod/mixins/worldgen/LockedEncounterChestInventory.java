package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks;
import net.amurdza.examplemod.worldgen.structure.CreativeRewardChestContainer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers hopper and Forge capability reads/removals before loot is unpacked. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class LockedEncounterChestInventory {
    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockedRead(int slot, CallbackInfoReturnable<ItemStack> result) {
        var chest = (BlockEntity) (Object) this;
        if (AuthoredRewardLocks.chestLocked(chest) && !CreativeRewardChestContainer.mayRead(chest)) result.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockedRemovalWithoutUpdate(int slot, CallbackInfoReturnable<ItemStack> result) {
        var chest = (BlockEntity) (Object) this;
        if (AuthoredRewardLocks.chestLocked(chest) && !CreativeRewardChestContainer.mayRemove(chest)) result.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "removeItem", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockedRemoval(int slot, int count, CallbackInfoReturnable<ItemStack> result) {
        var chest = (BlockEntity) (Object) this;
        if (AuthoredRewardLocks.chestLocked(chest) && !CreativeRewardChestContainer.mayRemove(chest)) result.setReturnValue(ItemStack.EMPTY);
    }

    @WrapOperation(method = "unpackLootTable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;fill(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/storage/loot/LootParams;J)V"))
    private void aoemod$fillActualInventory(LootTable table, Container container, LootParams params, long seed, Operation<Void> original) {
        CreativeRewardChestContainer.filling(container, () -> original.call(table, container, params, seed));
    }
}
