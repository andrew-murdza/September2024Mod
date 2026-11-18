package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.DimensionFoods;
import net.amurdza.examplemod.registry.ModCreativeTabs;
import net.amurdza.examplemod.util.CreativeCatalog;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.violetmoon.quark.content.tools.item.SeedPouchItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeCatalogRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void harvestedSourcesAndReachableRecipesStayInTheCreativeCatalog(GameTestHelper helper) {
        var level = helper.getLevel();
        net.amurdza.examplemod.util.ObtainableContent.rebuild(level.getRecipeManager(), level.registryAccess());
        for (var block : ForgeRegistries.BLOCKS.getValues()) {
            if (net.amurdza.examplemod.util.DocumentedPlacementRules.sourceMask(block) == null
                    || block.asItem() == Items.AIR) continue;
            var id = ForgeRegistries.ITEMS.getKey(block.asItem());
            if (RetainedContent.isExplicitlyRemovedItem(id)
                    || net.amurdza.examplemod.future.FutureContentPolicy.isExcludedItem(id)) continue;
            helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(block)), "Harvestable source missing from creative: " + id);
        }
        net.amurdza.examplemod.config.BlockConfig.BLOCK_INFO_BY_BLOCK.forEach((source, info) -> {
            if (!net.amurdza.examplemod.util.ObtainableContent.contains(source.asItem())
                    && net.amurdza.examplemod.util.DocumentedPlacementRules.sourceMask(source) == null) return;
            info.drops().forEach(drop -> {
                    if (drop.item() == null || drop.item() == Items.AIR) return;
                    helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(drop.item())),
                            "Configured harvest missing from creative: " + drop.item());
                });
        });
        helper.assertFalse(RetainedContent.isKeptItem(stack("aoemod:pale_moss_block")),
                "A registered block without a natural source or reachable recipe must stay hidden");
        helper.assertFalse(RetainedContent.isKeptItem(stack("aoemod:pale_moss_carpet")),
                "A recipe using an unobtainable source must not bootstrap that source");
        helper.assertFalse(RetainedContent.isKeptItem(stack("vintagedelight:oat_seeds"))
                        || RetainedContent.isKeptItem(stack("vintagedelight:raw_oats")),
                "Removed oat seed sources must not bootstrap availability through crop drops");
        helper.assertTrue(RetainedContent.isKeptItem(stack("aoemod:pale_oak_planks"))
                        && RetainedContent.isKeptItem(stack("aoemod:pale_oak_stairs")),
                "Building products crafted from naturally generated pale oak must remain");
        helper.assertTrue(net.amurdza.examplemod.registry.ModBlocks.DESERT_GRASS.get()
                        == ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:short_dry_grass"))
                        && net.amurdza.examplemod.registry.ModBlocks.DESERT_TALL_GRASS.get()
                        == ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:tall_dry_grass")),
                "Desert generation and harvests must use the native Backport grass");
        for (String removed : List.of("raw_spider_crab_leg", "cooked_spider_crab_leg", "desert_grass", "desert_tallgrass")) {
            helper.assertFalse(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(AOEMod.MOD_ID, removed)),
                    "Obsolete duplicate item still registered: " + removed);
        }
        var nativeTab = new ResourceLocation("vanillabackport", "vanilla_backport");
        helper.assertTrue("vanillabackport".equals(ModCreativeTabs.namespaceFor(nativeTab)),
                "Native Backport tab must survive to provide its update selector");
        helper.assertFalse(ModCreativeTabs.catalogs().containsKey("vanillabackport"),
                "A replacement Backport tab would lose the update selector");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void creativeCatalogsAreMergedOwnedAndPackedIntoFewerThanSixPages(GameTestHelper helper) throws Exception {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        List<CreativeModeTab> categories = CreativeModeTabs.allTabs().stream()
                .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY).toList();
        List<CreativeModeTab> visible = CreativeCatalog.visibleTabs(categories);
        Map<String, Set<String>> report = new TreeMap<>();
        Map<String, Integer> catalogCounts = new HashMap<>();
        for (CreativeModeTab tab : visible) {
            ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            String owner = ModCreativeTabs.namespaceFor(tabId);
            helper.assertTrue(owner != null || tabId.getNamespace().equals("minecraft"),
                    "Only the native Backport sidebar tab may remain alongside source catalogs: " + tabId);
            if (owner != null) catalogCounts.merge(owner, 1, Integer::sum);
            Set<String> ids = new TreeSet<>();
            for (ItemStack stack : tab.getDisplayItems()) {
                String expected = owner == null ? "minecraft" : owner;
                helper.assertTrue(expected.equals(CreativeCatalog.owner(stack)) && RetainedContent.isKeptItem(stack),
                        "Item leaked into another source's category: " + tabId + " / " + stack);
                ids.add(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
            }
            helper.assertTrue(!ids.isEmpty(), "Empty category consumed a tab slot");
            report.put(tabId.toString(), ids);
        }
        helper.assertTrue(catalogCounts.values().stream().allMatch(count -> count == 1), "A source mod has duplicate catalogs");
        helper.assertTrue(visible.size() > 10 && (visible.size() + 9) / 10 < 6,
                "Visible categories must pack into fewer than six ten-category pages: " + visible.size());
        for (String namespace : List.of("hearthandharvest", "legumedelight", "hybrid_aquatic", "farmersdelight", "netherdepthsupgrade")) {
            helper.assertTrue(catalogCounts.getOrDefault(namespace, 0) == 1, "Missing single merged source tab: " + namespace);
        }
        for (String id : List.of("legumedelight:peanuts", "farmersdelight:rich_soil_farmland", "nourished_nether:soul_quartz_slab",
                "ecologics:crab_meat", "hybrid_aquatic:starfish", "aoemod:cooked_starfish",
                "mynethersdelight:bullet_pepper",
                "outer_end:stalker_meat", "minecraft:short_dry_grass", "minecraft:tall_dry_grass",
                "aoemod:bucket_cubozoa", "aoemod:spawn_egg_cubozoa", "betterend:charnia_light_blue")) {
            helper.assertTrue(report.values().stream().anyMatch(ids -> ids.contains(id)), "Requested item missing from all catalogs: " + id);
        }
        ResourceLocation optionalBucket = new ResourceLocation("finsandtails:rubber_belly_glider_bucket");
        if (ForgeRegistries.ITEMS.containsKey(optionalBucket)) {
            helper.assertTrue(report.values().stream().noneMatch(ids -> ids.contains(optionalBucket.toString())), "Removed Rubber Belly Glider bucket must stay hidden");
        }
        Files.writeString(Path.of("creative-catalog-revision.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                Map.of("tabs", report, "pages", (visible.size() + 9) / 10,
                        "rubberBellyGliderBucketRegistered", ForgeRegistries.ITEMS.containsKey(optionalBucket))));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void metadataFiltersRemoveModVariantsWithoutRemovingVanillaForms(GameTestHelper helper) {
        helper.assertTrue(RetainedContent.isKeptItem(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1))),
                "Vanilla enchanted books must remain available");
        for (var entry : ForgeRegistries.ENCHANTMENTS.getEntries()) {
            if (entry.getKey().location().getNamespace().equals("minecraft")) continue;
            helper.assertFalse(RetainedContent.isKeptItem(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(entry.getValue(), 1))),
                    "Unrequested mod enchantment book leaked: " + entry.getKey().location());
        }
        helper.assertTrue(RetainedContent.isKeptItem(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)), "Vanilla potions must remain");
        for (var entry : ForgeRegistries.POTIONS.getEntries()) {
            if (entry.getKey().location().getNamespace().equals("minecraft")) continue;
            for (var item : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW)) {
                helper.assertFalse(RetainedContent.isKeptItem(PotionUtils.setPotion(new ItemStack(item), entry.getValue())),
                        "Mod potion variant leaked through a vanilla item ID: " + entry.getKey().location());
            }
        }
        ItemStack painting = new ItemStack(Items.PAINTING);
        painting.getOrCreateTagElement("EntityTag").putString("variant", "minecraft:kebab");
        helper.assertTrue(RetainedContent.isKeptItem(painting), "Vanilla painting must remain");
        painting.getOrCreateTagElement("EntityTag").putString("variant", "quark:sunset");
        helper.assertFalse(RetainedContent.isKeptItem(painting), "Mod painting NBT must be filtered");
        var parameters = new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        ModCreativeTabs.QUARK.get().buildContents(parameters);
        var tomes = ModCreativeTabs.QUARK.get().getDisplayItems().stream()
                .filter(stack -> ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals("quark:ancient_tome")).toList();
        helper.assertTrue(!tomes.isEmpty() && tomes.stream().allMatch(stack -> {
            var enchantment = org.violetmoon.quark.content.tools.module.AncientTomesModule.getTomeEnchantment(stack);
            return enchantment != null && ForgeRegistries.ENCHANTMENTS.getKey(enchantment).getNamespace().equals("minecraft");
        }), "Only usable ancient tomes of vanilla enchantments may remain");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void removedPlantsCannotHideInsideCreativeSeedPouches(GameTestHelper helper) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("quark:seed_pouch"));
        ItemStack pouch = new ItemStack(item);
        SeedPouchItem.mutateContents(pouch, contents -> {
            contents.setContents(new ItemStack(Items.WHEAT_SEEDS));
            contents.setCount(64);
            return null;
        });
        helper.assertFalse(RetainedContent.isKeptItem(pouch), "Preloaded pouch variants must not crowd the catalog");
        helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(item)), "The empty, usable seed pouch must remain");
        var removed = ForgeRegistries.ITEMS.getValues().stream().filter(candidate -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(candidate);
            return id.getPath().contains("sapling") && !RetainedContent.isKeptItem(new ItemStack(candidate));
        }).findFirst().orElseThrow();
        SeedPouchItem.mutateContents(pouch, contents -> {
            contents.setContents(new ItemStack(removed));
            contents.setCount(64);
            return null;
        });
        helper.assertFalse(RetainedContent.isKeptItem(pouch), "A removed sapling must not reappear through a filled pouch");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void requestedRemovalsAreHiddenAndCannotBeRecipeResults(GameTestHelper helper) {
        for (String id : List.of("hapi:creature_net", "wildernature:flamingo_spawn_egg", "mynethersdelight:blackstone_bricks_cabinet",
                "mynethersdelight:nether_bricks_cabinet",
                "vintagedelight:honey_roasted_peanut", "farmersdelight:apple_pie_slice", "farmersdelight:chocolate_pie_slice",
                "pineapple_delight:pineapple_pie_side", "nourished_nether:ghoulflower", "hybrid_aquatic:lightfoot_crab_claw")) {
            helper.assertFalse(RetainedContent.isKeptItem(stack(id)), "Removed creative item remains: " + id);
        }
        helper.assertTrue(RetainedContent.isKeptItem(stack("farmersdelight:pumpkin_slice")), "Pumpkin slice must remain");
        for (String recipe : List.of("pineapple_delight/cut/pineapple_pie_side", "farmersdelight/cutting/apple_pie",
                "farmersdelight/cutting/chocolate_pie", "vintagedelight/honey_roasted_peanut")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new ResourceLocation("aoemod", "retained/" + recipe)).isEmpty(),
                    "Removed slice/honeyed-peanut recipe still loads: " + recipe);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void starfishEatsInsteadOfThrowingAndHasAllThreeCookingRecipes(GameTestHelper helper) {
        var raw = stack("hybrid_aquatic:starfish");
        helper.assertTrue(raw.isEdible() && DimensionFoods.get("cooked_starfish").isEdible(), "Both starfish foods must be edible");
        var player = helper.makeMockSurvivalPlayer();
        player.getFoodData().setFoodLevel(10);
        player.setItemInHand(InteractionHand.MAIN_HAND, raw.copyWithCount(2));
        var response = raw.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(response.getResult().consumesAction() && player.isUsingItem(), "Right-click must start eating starfish");
        helper.assertTrue(!player.getCooldowns().isOnCooldown(raw.getItem()), "Eating starfish must not use the projectile cooldown");
        ItemStack remaining = raw.getItem().finishUsingItem(player.getMainHandItem(), helper.getLevel(), player);
        helper.assertTrue(remaining.getCount() == 1 && player.getFoodData().getFoodLevel() > 10, "Eating must consume one starfish and restore hunger");
        for (var type : List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING)) {
            helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(type).stream().anyMatch(recipe ->
                            recipe.matches(new SimpleContainer(raw), helper.getLevel())
                                    && recipe.getResultItem(helper.getLevel().registryAccess()).is(DimensionFoods.get("cooked_starfish"))),
                    "Missing starfish cooking path: " + type);
        }
        helper.succeed();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
    }
}
