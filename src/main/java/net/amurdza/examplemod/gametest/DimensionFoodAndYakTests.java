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
        var level = helper.getLevel();
        var owner = helper.makeMockSurvivalPlayer();
        var cow = EntityType.COW.create(level);
        var ordinary = EntityType.COW.create(level);
        ColdCowYaks.makeCold(cow);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 2));
        var ordinaryUse = new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, ordinary);
        ColdCowYaks.interact(ordinaryUse);
        helper.assertTrue(!ordinaryUse.isCanceled() && ((YakAccess) ordinary).aoe$yakOwner() == null,
                "Ordinary cows must retain their normal wheat interaction");
        ColdCowYaks.interact(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, cow));
        var yak = (YakAccess) cow;
        helper.assertTrue(owner.getUUID().equals(yak.aoe$yakOwner()), "Cold cow wheat taming must set the owner");
        helper.assertTrue(owner.getMainHandItem().getCount() == 1, "Taming must consume one wheat");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
        ColdCowYaks.interact(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, cow));
        helper.assertTrue(yak.aoe$hasYakChest() && owner.getMainHandItem().isEmpty(), "A chest must equip a cargo pack");
        var cargo = new ItemStack(Items.DIAMOND, 17);
        cargo.setHoverName(Component.literal("Persistent cargo"));
        yak.aoe$yakInventory().setItem(19, cargo);
        helper.assertTrue(cow.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),
                "Equipped yak packs must expose their item-handler capability");
        var saved = new CompoundTag();
        cow.saveWithoutId(saved);
        var restored = EntityType.COW.create(level);
        restored.load(saved);
        var loaded = (YakAccess) restored;
        helper.assertTrue(ColdCowYaks.isColdCow(restored), "Saving must preserve the Backport cold variant");
        helper.assertTrue(owner.getUUID().equals(loaded.aoe$yakOwner()) && loaded.aoe$hasYakChest(),
                "Owner and equipped chest must survive reload");
        helper.assertTrue(ItemStack.matches(cargo, loaded.aoe$yakInventory().getItem(19))
                        && loaded.aoe$yakInventory().getItem(0).isEmpty(),
                "Pack cargo must retain its exact slot, count and item NBT");
        var drops = new LivingDropsEvent(restored, level.damageSources().generic(), new ArrayList<>(), 0, true);
        CustomMobDrops.addConfiguredDrops(drops);
        ColdCowYaks.drops(drops);
        helper.assertTrue(drops.getDrops().stream().anyMatch(drop -> ItemStack.matches(cargo, drop.getItem())),
                "Configured cow loot must not erase cargo on death");
        helper.assertTrue(drops.getDrops().stream().filter(drop -> drop.getItem().is(Items.CHEST)).count() == 1,
                "An equipped chest must drop exactly once");
        helper.assertTrue(ColdCowYaks.drainCargo(loaded).isEmpty(), "Cargo must not duplicate on repeated cleanup");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void missingDocumentedFishHaveEdibleCookingResults(GameTestHelper helper) {
        for (String name : new String[]{"mahi_mahi", "anomalocaris", "octopus", "rubber_belly_glider",
                "flying_fish", "arrow_squid", "vampire_squid", "anglerfish", "spider_crab_leg", "isopod",
                "triops", "blue_spotted_stingray", "hound_shark", "shame_faced_crab", "trout", "carp",
                "swamp_mucker", "needlefish", "tuna", "dragonfish", "giant_river_prawn", "sulfur_end_fish"}) {
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
