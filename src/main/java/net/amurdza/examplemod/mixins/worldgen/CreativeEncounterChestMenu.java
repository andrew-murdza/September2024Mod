package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.sugar.Local;
import net.amurdza.examplemod.worldgen.structure.CreativeRewardChestContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestMenu.class)
public abstract class CreativeEncounterChestMenu {
    @ModifyVariable(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At("HEAD"), argsOnly = true)
    private static Container aoemod$creativeInventory(Container container, @Local(argsOnly = true) Inventory inventory) {
        return CreativeRewardChestContainer.wrap(container, inventory.player);
    }

    @Inject(method = "getContainer", at = @At("RETURN"), cancellable = true)
    private void aoemod$underlyingChest(CallbackInfoReturnable<Container> result) {
        result.setReturnValue(CreativeRewardChestContainer.unwrap(result.getReturnValue()));
    }
}
