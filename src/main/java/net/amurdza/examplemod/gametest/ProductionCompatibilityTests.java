package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProductionCompatibilityTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void vintageBrewingUsesRegisteredRecipes(GameTestHelper helper) {
        Potion dehydrated = potion("dehydrated");
        ItemStack salt = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("vintagedelight:salt_dust")));
        helper.assertFalse(salt.isEmpty(), "Salt must be the registered Vintage Delight ingredient");
        checkMix(helper, Potions.AWKWARD, salt, dehydrated);
        checkMix(helper, dehydrated, new ItemStack(Items.REDSTONE), potion("long_dehydrated"));
        checkMix(helper, dehydrated, new ItemStack(Items.GLOWSTONE_DUST), potion("strong_dehydrated"));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void llamaJoinSetsStrengthWithoutReflectiveNames(GameTestHelper helper) {
        var llama = EntityType.LLAMA.create(helper.getLevel());
        helper.assertTrue(llama != null, "Llama must be constructible");
        llama.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        try {
            helper.assertTrue(helper.getLevel().addFreshEntity(llama), "Llama must join without a reflection exception");
            helper.assertTrue(llama.getStrength() == 5, "Initial join must set five-column llama strength");
        } finally {
            llama.discard();
        }
        helper.succeed();
    }

    private static Potion potion(String path) {
        return BuiltInRegistries.POTION.get(new ResourceLocation("vintagedelight", path));
    }

    private static void checkMix(GameTestHelper helper, Potion input, ItemStack ingredient, Potion output) {
        ItemStack bottle = PotionUtils.setPotion(new ItemStack(Items.POTION), input);
        helper.assertTrue(output != Potions.EMPTY && PotionBrewing.hasMix(bottle, ingredient),
                "Expected brewing recipe is registered for " + ingredient);
        ItemStack result = PotionBrewing.mix(ingredient, bottle);
        helper.assertTrue(PotionUtils.getPotion(result) == output,
                "Brewing must produce the registered potion for " + ingredient);
    }
}
