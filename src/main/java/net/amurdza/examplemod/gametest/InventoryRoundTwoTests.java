package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.CreativeCatalog;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.util.ObtainableContent;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.violetmoon.quark.content.tools.item.SeedPouchItem;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InventoryRoundTwoTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void removedDependenciesAndNetherBrickCabinetStayUnavailable(GameTestHelper helper) {
        for (String namespace : List.of("ramadandelight", "floral_flair")) {
            helper.assertFalse(ModList.get().isLoaded(namespace), "Removed dependency must not load: " + namespace);
            helper.assertTrue(ForgeRegistries.ITEMS.getKeys().stream().noneMatch(id -> id.getNamespace().equals(namespace))
                            && ForgeRegistries.BLOCKS.getKeys().stream().noneMatch(id -> id.getNamespace().equals(namespace)),
                    "Removed dependency registries must be absent: " + namespace);
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream()
                            .noneMatch(recipe -> recipe.getId().getNamespace().equals(namespace)),
                    "Removed dependency recipes must be absent: " + namespace);
        }
        ResourceLocation cabinet = new ResourceLocation("mynethersdelight:nether_bricks_cabinet");
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(cabinet), "Use the installed cabinet ID");
        helper.assertFalse(RetainedContent.isKeptItem(stack(cabinet.toString())), "Nether brick cabinet must be hidden");
        helper.assertTrue(RetainedContent.isExplicitlyRemovedItem(cabinet), "Cabinet recipes must be suppressed");
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().noneMatch(recipe ->
                        cabinet.equals(ForgeRegistries.ITEMS.getKey(recipe.getResultItem(helper.getLevel().registryAccess()).getItem()))),
                "No recipe may produce the removed cabinet");
        helper.assertFalse(RetainedContent.isKeptItem(stack("mynethersdelight:blackstone_bricks_cabinet")),
                "The remaining blackstone cabinet must also be removed");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void actualHedgePathsPouchesAndRetiredHarvestItemsStayOutOfCatalogs(GameTestHelper helper) {
        for (String id : List.of("quark:blue_blossom_hedge", "quark:lavender_blossom_hedge", "quark:orange_blossom_hedge",
                "quark:yellow_blossom_hedge", "quark:red_blossom_hedge", "quark:ancient_hedge",
                "vintagedelight:wild_peanuts", "farmersdelight:rice_panicle", "pineapple_delight:huge_pineapple")) {
            helper.assertTrue(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id)), "Regression fixture must use the real installed ID: " + id);
            helper.assertFalse(RetainedContent.isKeptItem(stack(id)), "Requested removal still appears: " + id);
        }
        helper.assertTrue(RetainedContent.isKeptItem(stack("quark:oak_hedge")), "Ordinary tree hedges were not requested for removal");
        var hugePineapple = stack("pineapple_delight:huge_pineapple");
        var hugePineappleBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pineapple_delight:huge_pineapple"));
        helper.assertTrue(DocumentedPlacementRules.sourceMask(hugePineappleBlock) == null,
                "The retired huge pineapple must not remain a tropical acquisition source");
        ObtainableContent.rebuild(helper.getLevel().getRecipeManager(), helper.getLevel().registryAccess());
        helper.assertFalse(ObtainableContent.contains(hugePineapple.getItem()),
                "Rebuilding recipe/source reachability must not restore huge pineapple");
        helper.assertTrue(CreativeCatalog.items("pineapple_delight").noneMatch(item -> item.is(hugePineapple.getItem())),
                "Huge pineapple must stay out of the rebuilt source catalog");
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().noneMatch(recipe ->
                        recipe.getResultItem(helper.getLevel().registryAccess()).is(hugePineapple.getItem())),
                "No loaded recipe may recreate huge pineapple");
        for (String id : List.of("pineapple_delight:pineapple", "pineapple_delight:pineapple_crop",
                "pineapple_delight:pineapple_wild_crop", "pineapple_delight:pineapple_pie")) {
            helper.assertTrue(CreativeCatalog.items("pineapple_delight").anyMatch(item -> item.is(stack(id).getItem())),
                    "Ordinary pineapple crops and foods must remain available: " + id);
        }
        var pouch = stack("quark:seed_pouch");
        helper.assertTrue(RetainedContent.isKeptItem(pouch), "Keep the usable empty pouch");
        SeedPouchItem.mutateContents(pouch, contents -> {
            contents.setContents(stack("minecraft:wheat_seeds"));
            contents.setCount(64);
            return null;
        });
        helper.assertFalse(RetainedContent.isKeptItem(pouch), "Preloaded seed-pouch variants must no longer crowd the creative tab");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cookedSeafoodsBelongToAoeAndLavaFishKeepOnlyLiveForms(GameTestHelper helper) {
        for (String id : List.of("aoemod:cooked_crab_meat", "aoemod:cooked_starfish")) {
            helper.assertTrue(stack(id).isEdible() && CreativeCatalog.owner(stack(id)).equals("aoemod"),
                    "Cooked seafood must be edible and assigned to AoE: " + id);
            helper.assertTrue(RetainedContent.isKeptItem(stack(id)), "Cooked seafood must be visible: " + id);
        }
        helper.assertTrue(CreativeCatalog.owner(stack("hybrid_aquatic:cooked_crab")).equals("hybrid_aquatic"),
                "The dependency crab must retain its original ownership");
        for (String id : List.of("hybrid_aquatic:cooked_crab", "hybrid_aquatic:anglerfish",
                "aoemod:sulfur_end_fish", "aoemod:cooked_sulfur_end_fish")) {
            helper.assertFalse(RetainedContent.isKeptItem(stack(id)), "Retired seafood must not remain in the catalog: " + id);
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().noneMatch(recipe ->
                            recipe.getResultItem(helper.getLevel().registryAccess()).is(stack(id).getItem())),
                    "A recipe must not recreate retired seafood: " + id);
        }
        for (String id : List.of("aoemod:end_fish", "aoemod:cooked_end_fish", "aoemod:bucket_end_fish",
                "aoemod:spawn_egg_end_fish", "aoemod:raw_squid", "aoemod:raw_glow_squid",
                "aoemod:cooked_squid", "aoemod:cooked_glow_squid", "aoemod:raw_mahi_mahi")) {
            helper.assertFalse(RetainedContent.isKeptItem(stack(id)), "Retired seafood must stay hidden: " + id);
        }
        var retiredFish = ModEntities.END_FISH.get().create(helper.getLevel());
        var joined = new net.minecraftforge.event.entity.EntityJoinLevelEvent(retiredFish, helper.getLevel(), true);
        net.amurdza.examplemod.event_handlers.RetainedMobSpawns.joined(joined);
        helper.assertTrue(joined.isCanceled(), "Retired End Fish must not enter a world, including saved entities");
        retiredFish.discard();
        int removedFoods = 0;
        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            if (!RetainedContent.isRemovedLavaFishFood(entry.getKey().location())) continue;
            removedFoods++;
            helper.assertFalse(RetainedContent.isKeptItem(new ItemStack(entry.getValue())), "Lava-fish meat/meal leaked: " + entry.getKey().location());
        }
        helper.assertTrue(removedFoods >= 4, "Use real installed lava-fish food fixtures");
        for (String fish : List.of("blazefish", "glowdine", "magmacubefish", "wither_bonefish")) {
            for (String suffix : List.of("_bucket", "_spawn_egg")) {
                helper.assertTrue(RetainedContent.isKeptItem(stack("netherdepthsupgrade:" + fish + suffix)),
                        "Live lava-fish form must remain available: " + fish + suffix);
            }
        }
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            var result = recipe.getResultItem(helper.getLevel().registryAccess());
            helper.assertFalse(RetainedContent.isRemovedLavaFishFood(ForgeRegistries.ITEMS.getKey(result.getItem())),
                    "Removed lava meat cannot be produced by " + recipe.getId());
        }
        helper.succeed();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
    }
}
