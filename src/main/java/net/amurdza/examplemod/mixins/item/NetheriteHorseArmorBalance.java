package net.amurdza.examplemod.mixins.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseArmorItem.class)
public class NetheriteHorseArmorBalance {
    @Inject(method = "getProtection", at = @At("HEAD"), cancellable = true)
    private void aoemod$midpoint(CallbackInfoReturnable<Integer> cir) {
        HorseArmorItem item = (HorseArmorItem) (Object) this;
        if (!new ResourceLocation("minecraft", "netherite_horse_armor").equals(ForgeRegistries.ITEMS.getKey(item))) return;
        var copper = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", "copper_horse_armor"));
        if (copper instanceof HorseArmorItem lower) {
            cir.setReturnValue(Math.round((lower.getProtection() + ((HorseArmorItem) Items.IRON_HORSE_ARMOR).getProtection()) / 2.0F));
        }
    }
}
