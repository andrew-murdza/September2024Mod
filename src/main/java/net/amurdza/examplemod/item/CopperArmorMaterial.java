package net.amurdza.examplemod.item;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum CopperArmorMaterial implements ArmorMaterial {
    INSTANCE;

    private static final int[] DURABILITY = {13, 15, 16, 11};
    private static final int[] DEFENSE = {1, 3, 4, 2};

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY[type.getSlot().getIndex()] * 11;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return 8;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.COPPER_INGOT);
    }

    @Override
    public String getName() {
        return AOEMod.MOD_ID + ":copper";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
