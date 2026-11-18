package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers hopper and Forge capability reads/removals before loot is unpacked. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class LockedEncounterChestInventory {
    @Inject(method = {"getItem", "removeItemNoUpdate"}, at = @At("HEAD"), cancellable = true)
    private void aoemod$lockedRead(int slot, CallbackInfoReturnable<ItemStack> result) {
        if (AuthoredRewardLocks.chestLocked((BlockEntity) (Object) this)) result.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "removeItem", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockedRemoval(int slot, int count, CallbackInfoReturnable<ItemStack> result) {
        if (AuthoredRewardLocks.chestLocked((BlockEntity) (Object) this)) result.setReturnValue(ItemStack.EMPTY);
    }
}
