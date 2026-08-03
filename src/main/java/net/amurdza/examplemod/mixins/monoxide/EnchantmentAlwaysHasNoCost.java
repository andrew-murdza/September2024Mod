package net.amurdza.examplemod.mixins.monoxide;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla enchantment completion side effects, then refunds the level cost. */
@Mixin(Player.class)
public class EnchantmentAlwaysHasNoCost {
  @Shadow public int experienceLevel;
  @Unique private int aoemod$levelBeforeEnchantment;

  @Inject(
    method = "onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V",
    at = @At("HEAD")
  )
  private void rememberLevel(final ItemStack pEnchantedItem, final int pLevelCost, final CallbackInfo info) {
    this.aoemod$levelBeforeEnchantment = this.experienceLevel;
  }

  @Inject(
    method = "onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V",
    at = @At("RETURN")
  )
  private void noCost(final ItemStack pEnchantedItem, final int pLevelCost, final CallbackInfo info) {
    this.experienceLevel = this.aoemod$levelBeforeEnchantment;
  }
}
