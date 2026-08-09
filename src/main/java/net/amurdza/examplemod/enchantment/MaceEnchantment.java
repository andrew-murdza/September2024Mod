package net.amurdza.examplemod.enchantment;

import net.amurdza.examplemod.item.MaceItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class MaceEnchantment extends Enchantment {
    private static final EnchantmentCategory MACE =
            EnchantmentCategory.create("AOEMOD_MACE", item -> item instanceof MaceItem);

    private final int maxLevel;
    private final int baseCost;
    private final int perLevelCost;

    public MaceEnchantment(Rarity rarity, int maxLevel, int baseCost, int perLevelCost) {
        super(rarity, MACE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.perLevelCost = perLevelCost;
    }

    @Override
    public int getMaxLevel() {
        return this.maxLevel;
    }

    @Override
    public int getMinCost(int level) {
        return this.baseCost + (level - 1) * this.perLevelCost;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 50;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof MaceItem || super.canEnchant(stack);
    }
}
