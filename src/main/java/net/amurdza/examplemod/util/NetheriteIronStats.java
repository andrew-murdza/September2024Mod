package net.amurdza.examplemod.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public final class NetheriteIronStats {
    private static final ThreadLocal<Boolean> BYPASS_EQUIVALENT_OVERRIDES =
            ThreadLocal.withInitial(() -> false);

    private NetheriteIronStats() {
    }

    public static Item ironEquivalent(Item item) {
        return statEquivalent(item);
    }

    private static Item copperEquivalent(Item item) {
        if (item == Items.NETHERITE_SWORD) return ModItems.COPPER_SWORD.get();
        if (item == Items.NETHERITE_SHOVEL) return ModItems.COPPER_SHOVEL.get();
        if (item == Items.NETHERITE_PICKAXE) return ModItems.COPPER_PICKAXE.get();
        if (item == Items.NETHERITE_AXE) return ModItems.COPPER_AXE.get();
        if (item == Items.NETHERITE_HOE) return ModItems.COPPER_HOE.get();
        if (item == Items.NETHERITE_HELMET) return ModItems.COPPER_HELMET.get();
        if (item == Items.NETHERITE_CHESTPLATE) return ModItems.COPPER_CHESTPLATE.get();
        if (item == Items.NETHERITE_LEGGINGS) return ModItems.COPPER_LEGGINGS.get();
        if (item == Items.NETHERITE_BOOTS) return ModItems.COPPER_BOOTS.get();
        return null;
    }

    public static Item statEquivalent(Item item) {
        if (item == Items.DIAMOND_SWORD) return Items.NETHERITE_SWORD;
        if (item == Items.DIAMOND_SHOVEL) return Items.NETHERITE_SHOVEL;
        if (item == Items.DIAMOND_PICKAXE) return Items.NETHERITE_PICKAXE;
        if (item == Items.DIAMOND_AXE) return Items.NETHERITE_AXE;
        if (item == Items.DIAMOND_HOE) return Items.NETHERITE_HOE;
        if (item == Items.DIAMOND_HELMET) return Items.NETHERITE_HELMET;
        if (item == Items.DIAMOND_CHESTPLATE) return Items.NETHERITE_CHESTPLATE;
        if (item == Items.DIAMOND_LEGGINGS) return Items.NETHERITE_LEGGINGS;
        if (item == Items.DIAMOND_BOOTS) return Items.NETHERITE_BOOTS;
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
        if (isBypassingEquivalentOverrides()) {
            return null;
        }

        Item equivalent = statEquivalent(stack.getItem());
        Item copper = copperEquivalent(stack.getItem());
        return equivalent == null ? null : withoutEquivalentOverrides(() -> copper == null
                ? equivalent.getDefaultInstance().getMaxDamage()
                : Math.round((equivalent.getDefaultInstance().getMaxDamage() + copper.getDefaultInstance().getMaxDamage()) / 2.0F));
    }

    public static Integer enchantmentValue(ItemStack stack) {
        return enchantmentValue(stack.getItem());
    }

    public static Integer enchantmentValue(Item item) {
        if (isBypassingEquivalentOverrides()) {
            return null;
        }

        Item equivalent = statEquivalent(item);
        Item copper = copperEquivalent(item);
        return equivalent == null ? null : withoutEquivalentOverrides(() -> copper == null
                ? equivalent.getDefaultInstance().getEnchantmentValue()
                : Math.round((equivalent.getDefaultInstance().getEnchantmentValue() + copper.getDefaultInstance().getEnchantmentValue()) / 2.0F));
    }

    public static Float destroySpeed(ItemStack stack, BlockState state) {
        if (isBypassingEquivalentOverrides()) {
            return null;
        }

        Item equivalent = statEquivalent(stack.getItem());
        Item copper = copperEquivalent(stack.getItem());
        return equivalent == null ? null : withoutEquivalentOverrides(() -> copper == null
                ? equivalent.getDefaultInstance().getDestroySpeed(state)
                : (equivalent.getDefaultInstance().getDestroySpeed(state) + copper.getDefaultInstance().getDestroySpeed(state)) / 2.0F);
    }

    public static Multimap<Attribute, AttributeModifier> defaultAttributes(Item item, EquipmentSlot slot) {
        if (isBypassingEquivalentOverrides()) {
            return null;
        }

        Item equivalent = statEquivalent(item);
        Item copper = copperEquivalent(item);
        if (equivalent == null) return null;
        return withoutEquivalentOverrides(() -> {
            Multimap<Attribute, AttributeModifier> iron = equivalent.getDefaultAttributeModifiers(slot);
            if (copper == null) return iron;
            Multimap<Attribute, AttributeModifier> lower = copper.getDefaultAttributeModifiers(slot);
            ImmutableMultimap.Builder<Attribute, AttributeModifier> result = ImmutableMultimap.builder();
            for (var entry : iron.entries()) {
                AttributeModifier modifier = entry.getValue();
                double copperAmount = lower.get(entry.getKey()).stream()
                        .filter(other -> other.getOperation() == modifier.getOperation())
                        .mapToDouble(AttributeModifier::getAmount).sum();
                result.put(entry.getKey(), new AttributeModifier(modifier.getId(), modifier.getName(),
                        (modifier.getAmount() + copperAmount) / 2.0D, modifier.getOperation()));
            }
            return result.build();
        });
    }

    public static boolean isBypassingEquivalentOverrides() {
        return Boolean.TRUE.equals(BYPASS_EQUIVALENT_OVERRIDES.get());
    }

    private static <T> T withoutEquivalentOverrides(Supplier<T> supplier) {
        boolean previous = isBypassingEquivalentOverrides();
        BYPASS_EQUIVALENT_OVERRIDES.set(true);

        try {
            return supplier.get();
        } finally {
            BYPASS_EQUIVALENT_OVERRIDES.set(previous);
        }
    }
}
