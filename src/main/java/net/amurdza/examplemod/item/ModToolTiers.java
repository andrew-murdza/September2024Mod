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
            1,
            190,
            5.0F,
            1.0F,
            13,
            BlockTags.NEEDS_STONE_TOOL,
            () -> Ingredient.of(Items.BONE)
    );

    /** Post-1.20 copper tier: stone harvest level with improved speed, durability and enchantability. */
    public static final ForgeTier COPPER = new ForgeTier(
            1,
            190,
            5.0F,
            1.0F,
            13,
            BlockTags.NEEDS_STONE_TOOL,
            () -> Ingredient.of(Items.COPPER_INGOT)
    );

    public static final ForgeTier REBALANCED_NETHERITE = new ForgeTier(
            1, Math.round((190 + 250) / 2.0F), (5.0F + 6.0F) / 2.0F,
            (1.0F + 2.0F) / 2.0F, Math.round((13 + 14) / 2.0F),
            BlockTags.NEEDS_STONE_TOOL, () -> Ingredient.of(Items.NETHERITE_INGOT));

    public static void register() {
        TierSortingRegistry.registerTier(
                BONE,
                new ResourceLocation(AOEMod.MOD_ID, "bone"),
                List.of(Tiers.STONE),
                List.of(Tiers.IRON)
        );
        TierSortingRegistry.registerTier(
                COPPER,
                new ResourceLocation(AOEMod.MOD_ID, "copper"),
                List.of(Tiers.STONE),
                List.of(Tiers.IRON)
        );
        TierSortingRegistry.registerTier(REBALANCED_NETHERITE,
                new ResourceLocation(AOEMod.MOD_ID, "rebalanced_netherite"),
                List.of(COPPER), List.of(Tiers.IRON));
    }
}
