package net.amurdza.examplemod.util;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public final class NetheriteIronStats {
    private NetheriteIronStats() {
    }

    public static Item ironEquivalent(Item item) {
        if (item == Items.NETHERITE_SWORD) return Items.IRON_SWORD;
        if (item == Items.NETHERITE_SHOVEL) return Items.IRON_SHOVEL;
        if (item == Items.NETHERITE_PICKAXE) return Items.IRON_PICKAXE;
        if (item == Items.NETHERITE_AXE) return Items.IRON_AXE;
        if (item == Items.NETHERITE_HOE) return Items.IRON_HOE;
        if (item == Items.NETHERITE_HELMET) return Items.IRON_HELMET;
        if (item == Items.NETHERITE_CHESTPLATE) return Items.IRON_CHESTPLATE;
        if (item == Items.NETHERITE_LEGGINGS) return Items.IRON_LEGGINGS;
        if (item == Items.NETHERITE_BOOTS) return Items.IRON_BOOTS;
        return null;
    }

    public static Integer maxDamage(ItemStack stack) {
        Item equivalent = ironEquivalent(stack.getItem());
        return equivalent == null ? null : equivalent.getDefaultInstance().getMaxDamage();
    }

    public static Float destroySpeed(ItemStack stack, BlockState state) {
        Item equivalent = ironEquivalent(stack.getItem());
        return equivalent == null ? null : equivalent.getDefaultInstance().getDestroySpeed(state);
    }

    public static Multimap<Attribute, AttributeModifier> defaultAttributes(Item item, EquipmentSlot slot) {
        Item equivalent = ironEquivalent(item);
        return equivalent == null ? null : equivalent.getDefaultAttributeModifiers(slot);
    }
}
