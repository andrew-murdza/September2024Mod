package net.amurdza.examplemod.item;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public class ModToolTiers {

    public static final ForgeTier BONE = new ForgeTier(
            1,                      // harvest level (netherite = 4)
            250,                   // max uses (durability)
            4.0f,                  // mining speed
            1.0f,                   // attack damage bonus
            5,                     // enchantability
            BlockTags.NEEDS_STONE_TOOL, // what blocks it can mine
            () -> Ingredient.of(Items.BONE)
    );

    public static void register() {
        TierSortingRegistry.registerTier(
                BONE,
                new ResourceLocation(AOEMod.MOD_ID, "bone"),
                List.of(Tiers.WOOD), // after
                List.of(Tiers.IRON)                                          // before
        );
    }
}
