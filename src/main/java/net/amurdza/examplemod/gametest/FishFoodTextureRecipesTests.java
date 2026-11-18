package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FishFoodTextureRecipesTests {
    private record FishFood(String mob, String raw, String cooked, ResourceKey<Level> dimension) { }

    private static final List<FishFood> FOODS = List.of(
            new FishFood("minecraft:squid", "culturaldelights:squid", "culturaldelights:cooked_squid", GameplayDimensions.TROPICAL),
            food("hybrid_aquatic:coconut_crab", "ecologics:crab_meat", "crab_meat", GameplayDimensions.TROPICAL),
            food("hybrid_aquatic:lightfoot_crab", "ecologics:crab_meat", "crab_meat", GameplayDimensions.TROPICAL),
            food("hybrid_aquatic:flower_crab", "ecologics:crab_meat", "crab_meat", GameplayDimensions.PLAINS),
            food("hybrid_aquatic:fiddler_crab", "ecologics:crab_meat", "crab_meat", GameplayDimensions.DESERT),
            food("hybrid_aquatic:spider_crab", "ecologics:crab_meat", "crab_meat", GameplayDimensions.SNOWY),
            food("finsandtails:swamp_mucker", "finsandtails:swamp_mucker", "swamp_mucker", GameplayDimensions.PLAINS),
            food("hybrid_aquatic:umbrella_octopus", "aoemod:raw_umbrella_octopus", "umbrella_octopus", GameplayDimensions.TROPICAL),
            food("hybrid_aquatic:octopus", "aoemod:raw_octopus", "octopus", GameplayDimensions.PLAINS),
            food("hybrid_aquatic:starfish", "hybrid_aquatic:starfish", "starfish", GameplayDimensions.TROPICAL),
            food("angling:anomalocaris", "aoemod:raw_anomalocaris", "anomalocaris", GameplayDimensions.TROPICAL),
            new FishFood("minecraft:glow_squid", "culturaldelights:glow_squid", "culturaldelights:cooked_squid", GameplayDimensions.SNOWY),
            food("hybrid_aquatic:tuna", "hybrid_aquatic:tuna", "tuna", GameplayDimensions.SNOWY),
            food("hybrid_aquatic:arrow_squid", "aoemod:raw_arrow_squid", "arrow_squid", GameplayDimensions.SNOWY),
            food("alexsmobs:giant_squid", "alexsmobs:lost_tentacle", "lost_tentacle", GameplayDimensions.SNOWY),
            food("alexsmobs:flying_fish", "alexsmobs:flying_fish", "flying_fish", GameplayDimensions.SNOWY),
            food("hybrid_aquatic:vampire_squid", "aoemod:raw_vampire_squid", "vampire_squid", GameplayDimensions.SNOWY));

    private FishFoodTextureRecipesTests() { }

    private static FishFood food(String mob, String raw, String name, ResourceKey<Level> dimension) {
        return new FishFood(mob, raw, "aoemod:cooked_" + name, dimension);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyRequestedRawFoodCooksToItsOwnEdibleResult(GameTestHelper helper) {
        for (FishFood food : FOODS) {
            Item raw = item(helper, food.raw());
            Item cooked = item(helper, food.cooked());
            helper.assertTrue(raw.isEdible() && cooked.isEdible(), "Both food states must be edible: " + food.raw());
            helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(raw)) && RetainedContent.isKeptItem(new ItemStack(cooked)),
                    "Both food states must be visible in the catalog: " + food.raw());
            var input = new SimpleContainer(new ItemStack(raw));
            for (var type : List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING)) {
                // Check the recipe the appliance actually selects, so an old conflicting recipe cannot hide behind anyMatch.
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(type, input, helper.getLevel());
                helper.assertTrue(recipe.isPresent(), "Missing " + type + " recipe for " + food.raw());
                ItemStack result = recipe.orElseThrow().getResultItem(helper.getLevel().registryAccess());
                helper.assertTrue(result.is(cooked) && result.getCount() == 1,
                        "The selected " + type + " recipe must produce one " + food.cooked() + " from " + food.raw());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void requestedAnimalsDropMatchingRawAndFireCookedFoods(GameTestHelper helper) {
        for (FishFood food : FOODS) {
            var level = helper.getLevel().getServer().getLevel(food.dimension());
            helper.assertTrue(level != null, "Missing gameplay dimension for " + food.mob());
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(food.mob()));
            helper.assertTrue(type != null, "Missing animal " + food.mob());
            Mob mob = (Mob) type.create(level);
            helper.assertTrue(mob != null, "Cannot create animal " + food.mob());
            mob.setPos(0, 80, 0);
            try {
                Item raw = item(helper, food.raw());
                Item cooked = item(helper, food.cooked());
                helper.assertTrue(MobConfig.mobDropEntries(mob).stream().anyMatch(row -> row.item() == raw
                                && row.cookedItem() == cooked && MobConfig.mobAmount(mob, row) >= 1),
                        "Animal needs a nonzero raw/cooked food reward in its home dimension: " + food.mob());
                for (boolean burning : new boolean[]{false, true}) {
                    mob.setRemainingFireTicks(burning ? 80 : 0);
                    // Tropical animals cannot ignite outside lava; cooking recipes still supply their cooked food.
                    boolean expectedBurning = burning && !food.dimension().equals(GameplayDimensions.TROPICAL);
                    helper.assertTrue(mob.isOnFire() == expectedBurning,
                            "Fire state must honor the animal's home-dimension rule: " + food.mob());
                    var drops = new ArrayList<ItemEntity>();
                    drops.add(new ItemEntity(level, 0, 80, 0, new ItemStack(Items.COD)));
                    var event = new LivingDropsEvent(mob, level.damageSources().generic(), drops, 0, true);
                    CustomMobDrops.addConfiguredDrops(event);
                    Item expected = mob.isOnFire() ? cooked : raw;
                    Item opposite = mob.isOnFire() ? raw : cooked;
                    helper.assertTrue(event.getDrops().stream().anyMatch(drop -> drop.getItem().is(expected)),
                            "Death handler must yield " + ForgeRegistries.ITEMS.getKey(expected) + " for " + food.mob());
                    helper.assertTrue(event.getDrops().stream().noneMatch(drop -> drop.getItem().is(opposite)
                                    || drop.getItem().is(Items.COD)),
                            "Death handler must replace the wrong food state and native placeholder fish: " + food.mob());
                    event.getDrops().forEach(ItemEntity::discard);
                }
            } finally {
                mob.discard();
            }
        }
        helper.succeed();
    }

    private static Item item(GameTestHelper helper, String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        helper.assertTrue(item != null && item != Items.AIR, "Missing registered food " + id);
        return item;
    }
}
