//package net.amurdza.examplemod.mixins.monoxide;
//
//import net.minecraft.world.entity.player.Inventory;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.inventory.AnvilMenu;
//import net.minecraft.world.inventory.ContainerLevelAccess;
//import net.minecraft.world.inventory.DataSlot;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
///** Replaces the anvil cost DataSlot so that it ignores any value given to it */
//@Mixin(AnvilMenu.class)
//public class AnvilAlwaysHasNoCost {
//  @Inject(
//    method = "<init>*",
//    at = @At(
//      value = "RETURN",
//      target = "Lnet/minecraft/world/inventory/AnvilMenu;<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V"
//    )
//  )
//  private void replaceDataSlot(final int pContainerId, final Inventory pPlayerInventory, final ContainerLevelAccess pAccess, final CallbackInfo info) {
//    final AnvilMenu menu = (AnvilMenu)(Object)this;
//
//    menu.cost = new DataSlot() {
//      @Override
//      public int get() {
//        return 0;
//      }
//
//      @Override
//      public void set(final int i) {
//
//      }
//    };
//  }
//
//  @Inject(
//    method = "mayPickup(Lnet/minecraft/world/entity/player/Player;Z)Z",
//    at = @At("HEAD"),
//    cancellable = true
//  )
//  protected void alwaysPickup(final Player pPlayer, final boolean pHasStack, final CallbackInfoReturnable<Boolean> info) {
//    info.setReturnValue(true);
//  }
//}
