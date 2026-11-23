package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ai.YakAccess;
import net.amurdza.examplemod.event_handlers.ColdCowYaks;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.registry.DimensionFoods;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DimensionFoodAndYakTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void coldCowTamingCargoAndDeathPreserveItems(GameTestHelper helper) {
        SnowyNovember14RevisionTests.dedicatedYakUsesNativeMountInventoryAndMilk(helper);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void missingDocumentedFishHaveEdibleCookingResults(GameTestHelper helper) {
        for (String name : new String[]{"octopus",
                "flying_fish", "arrow_squid", "vampire_squid", "crab_meat", "umbrella_octopus",
                "triops", "blue_spotted_stingray",
                "tuna"}) {
            var cooked = DimensionFoods.get("cooked_" + name);
            helper.assertTrue(cooked != null && cooked != Items.AIR && cooked.isEdible(),
                    "Missing edible cooked item for " + name);
            for (String method : new String[]{"smelting", "smoking", "campfire_cooking"}) {
                var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(AOEMod.MOD_ID,
                        method + "/cooked_" + name));
                helper.assertTrue(recipe.isPresent() && recipe.get().getResultItem(helper.getLevel().registryAccess()).is(cooked),
                        "Missing or incorrect " + method + " result for " + name);
            }
        }
        helper.assertTrue(DimensionFoods.get("plum").isEdible(), "The selected dark-oak fruit must be an edible plum");
        helper.succeed();
    }
}
