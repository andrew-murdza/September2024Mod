package net.amurdza.examplemod.mixins.monoxide;

import net.amurdza.examplemod.item.MaceItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class TreasureEnchantmentsAreOrdinary {
    @Inject(method = "isTreasureOnly", at = @At("HEAD"), cancellable = true)
    private void aoemod$treasureIsEnchantable(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void aoemod$maceAcceptsEveryEnchantment(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof MaceItem) {
            cir.setReturnValue(true);
        }
    }
}
