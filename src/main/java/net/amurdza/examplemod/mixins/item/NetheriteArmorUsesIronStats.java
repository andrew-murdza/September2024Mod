package net.amurdza.examplemod.mixins.item;

import com.google.common.collect.Multimap;
import net.amurdza.examplemod.util.NetheriteIronStats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public class NetheriteArmorUsesIronStats {
    @Inject(method = "getDefaultAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private void aoemod$useIronArmorAttributes(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        Multimap<Attribute, AttributeModifier> attributes = NetheriteIronStats.defaultAttributes((Item) (Object) this, slot);
        if (attributes != null) {
            cir.setReturnValue(attributes);
        }
    }
}
