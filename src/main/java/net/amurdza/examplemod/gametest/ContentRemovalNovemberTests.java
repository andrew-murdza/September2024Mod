package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.CreativeCatalog;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentRemovalNovemberTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void intentionalFoodRemovalsDoNotInterruptSavedWorldLoading(GameTestHelper helper) {
        String[] ids = {"aoemod:raw_warped_toad", "aoemod:cooked_warped_toad", "aoemod:cooked_pufferfish",
                "aoemod:cooked_tropical_fish", "aoemod:unrelated_missing_item", "othermod:cooked_pufferfish"};
        boolean[] ignored = new boolean[ids.length];
        var mappings = new java.util.ArrayList<net.minecraftforge.registries.MissingMappingsEvent.Mapping<?>>();
        for (int index = 0; index < ids.length; index++) {
            int slot = index;
            mappings.add(new net.minecraftforge.registries.MissingMappingsEvent.Mapping<net.minecraft.world.item.Item>(
                    ForgeRegistries.ITEMS, ForgeRegistries.ITEMS, new ResourceLocation(ids[index]), index) {
                @Override public void ignore() {
                    ignored[slot] = true;
                    super.ignore();
                }
            });
        }
        var event = new net.minecraftforge.registries.MissingMappingsEvent(net.minecraft.core.registries.Registries.ITEM,
                ForgeRegistries.ITEMS, mappings);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        for (int index = 0; index < ids.length; index++) helper.assertTrue(ignored[index] == (index < 4),
                "Only the four intentionally removed AoE food IDs may bypass the missing-mapping prompt: " + ids[index]);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void removedToadAndCookedVanillaFishHaveNoRegistrationsOrDropOverrides(GameTestHelper helper) {
        for (String name : Set.of("raw_warped_toad", "cooked_warped_toad", "cooked_pufferfish", "cooked_tropical_fish")) {
            var id = new ResourceLocation("aoemod", name);
            helper.assertFalse(ForgeRegistries.ITEMS.containsKey(id), "Removed food must not remain registered: " + id);
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().noneMatch(recipe ->
                            id.equals(ForgeRegistries.ITEMS.getKey(recipe.getResultItem(helper.getLevel().registryAccess()).getItem()))),
                    "Removed food must not remain a recipe result: " + id);
        }
        helper.assertFalse(RetainedContent.isKeptEntity(new ResourceLocation("alexsmobs", "warped_toad")),
                "Removing AoE's toad customization must not restore the excluded native mob");
        for (var type : java.util.List.of(net.minecraft.world.entity.EntityType.PUFFERFISH,
                net.minecraft.world.entity.EntityType.TROPICAL_FISH)) {
            var fish = type.create(helper.getLevel());
            helper.assertTrue(fish != null, "Native fish entities must remain available");
            var nativeFood = type == net.minecraft.world.entity.EntityType.PUFFERFISH
                    ? net.minecraft.world.item.Items.PUFFERFISH : net.minecraft.world.item.Items.TROPICAL_FISH;
            helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(nativeFood)), "Native fish items must remain available");
            helper.assertTrue(net.amurdza.examplemod.config.MobConfig.mobDropEntries(fish).isEmpty(),
                    "AoE must leave native and future compatibility fish loot unchanged");
            var nativeDrop = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(nativeFood));
            var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            drops.add(nativeDrop);
            var event = new net.minecraftforge.event.entity.living.LivingDropsEvent(fish,
                    helper.getLevel().damageSources().generic(), drops, 0, true);
            net.amurdza.examplemod.event_handlers.CustomMobDrops.addConfiguredDrops(event);
            helper.assertTrue(drops.size() == 1 && drops.get(0) == nativeDrop,
                    "The configured drop handler must preserve the original fish loot event");
            nativeDrop.discard();
            fish.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyThreeNativeCulturalFoodsRemainAndMahiIsHidden(GameTestHelper helper) {
        Set<String> foods = ForgeRegistries.ITEMS.getKeys().stream()
                .filter(id -> id.getNamespace().equals("culturaldelights"))
                .map(ResourceLocation::getPath).collect(Collectors.toSet());
        helper.assertTrue(foods.equals(Set.of("squid", "glow_squid", "cooked_squid")),
                "Retain exactly the two native squid foods and their common cooked item: " + foods);
        helper.assertTrue(ForgeRegistries.BLOCKS.getKeys().stream().noneMatch(id -> id.getNamespace().equals("culturaldelights")),
                "Cultural Delights must contribute no other blocks or crops");
        var mahi = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("hybrid_aquatic", "mahi")));
        helper.assertTrue(mahi.isEdible() && !RetainedContent.isKeptItem(mahi)
                        && CreativeCatalog.items("hybrid_aquatic").noneMatch(stack -> stack.is(mahi.getItem())),
                "Dependency mahi stays registered for old saves but must leave the catalog");
        for (String id : Set.of("aoemod:end_fish", "aoemod:cooked_end_fish", "aoemod:bucket_end_fish",
                "aoemod:spawn_egg_end_fish", "aoemod:raw_mahi_mahi", "aoemod:raw_squid", "aoemod:raw_glow_squid",
                "aoemod:cooked_squid", "aoemod:cooked_glow_squid", "orcz:ironscrap", "orcz:scrapblock",
                "orcz:orc_wood_log", "orcz:orc_wood_planks", "orcz:orc_wood_slab", "orcz:orcsymbol", "orcz:orcsymbolblue")) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            helper.assertFalse(RetainedContent.isKeptItem(new ItemStack(item)), "Removed content remains visible: " + id);
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().noneMatch(recipe ->
                            recipe.getResultItem(helper.getLevel().registryAccess()).is(item)),
                    "A recipe recreates removed content: " + id);
        }
        helper.succeed();
    }
}
