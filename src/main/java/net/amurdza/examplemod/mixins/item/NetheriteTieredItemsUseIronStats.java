package net.amurdza.examplemod.mixins.item;

import net.amurdza.examplemod.util.NetheriteIronStats;
import net.amurdza.examplemod.item.ModToolTiers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TieredItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TieredItem.class)
public class NetheriteTieredItemsUseIronStats {
    @Inject(method = "getTier", at = @At("RETURN"), cancellable = true)
    private void aoemod$netheriteBetweenCopperAndIron(CallbackInfoReturnable<Tier> cir) {
        if (!NetheriteIronStats.isBypassingEquivalentOverrides() && cir.getReturnValue() == Tiers.NETHERITE) {
            cir.setReturnValue(ModToolTiers.REBALANCED_NETHERITE);
        }
    }

    @Inject(method = "getEnchantmentValue", at = @At("HEAD"), cancellable = true)
    private void aoemod$useEquivalentEnchantability(CallbackInfoReturnable<Integer> cir) {
        Integer enchantmentValue = NetheriteIronStats.enchantmentValue((Item) (Object) this);
        if (enchantmentValue != null) {
            cir.setReturnValue(enchantmentValue);
        }
    }
}
