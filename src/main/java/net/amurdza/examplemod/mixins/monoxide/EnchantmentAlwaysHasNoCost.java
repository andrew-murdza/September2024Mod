package net.amurdza.examplemod.mixins.monoxide;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hardcodes enchantment cost to 0 */
@Mixin(Player.class)
public class EnchantmentAlwaysHasNoCost {
  @Inject(
    method = "onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V",
    at = @At("HEAD"),
    cancellable = true
  )
  private void noCost(final ItemStack pEnchantedItem, final int pLevelCost, final CallbackInfo info) {
    info.cancel();
  }
}
