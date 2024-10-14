package net.amurdza.examplemod.mixins.monoxide;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Changes stack size for any ID containing "bottle" or "stew" to 64 */
@Mixin(Item.class)
public class BottlesAndStewsStackTo64 {
  @Inject(
    method = "getMaxStackSize",
    at = @At("HEAD"),
    cancellable = true
  )
  private void getMaxStackSize(final CallbackInfoReturnable<Integer> info) {
    final String id = ((Item)(Object)this).builtInRegistryHolder().key().location().getPath();

    if(id.contains("bottle") || id.contains("stew")) {
      info.setReturnValue(64);
    }
  }
}
