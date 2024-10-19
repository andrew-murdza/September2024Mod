package net.amurdza.examplemod.mixins.monoxide;

import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EnchantmentMenu.class)
public class EnchantmentMenu20Lapis {
  // Not needed since we adjust the actual cost below
/*
  @Final
  @Shadow
  private Container enchantSlots;
  @Final
  @Shadow
  public int[] costs;

  @Inject(
    method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z",
    at = @At("HEAD"),
    cancellable = true
  )
  private void clickMenuButton(final Player pPlayer, final int pId, final CallbackInfoReturnable<Boolean> cir) {
    // Require 20 lapis
    if(pId >= 0 && pId < this.costs.length) {
      final ItemStack itemstack1 = this.enchantSlots.getItem(1);
      final int i = 20;
      if((itemstack1.isEmpty() || itemstack1.getCount() < i) && !pPlayer.getAbilities().instabuild) {
        cir.setReturnValue(false);
      }
    }
  }
*/

  // It would be ideal to modify the code that removes the lapis instead of doing this, but that code is inside of a lambda and seems impossible to target with a mixin
  @ModifyVariable(
    method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z",
    name = "i",
    at = @At(
      value = "STORE"
    )
  )
  private int remove20Lapis(final int i) {
    return 20;
  }
}
