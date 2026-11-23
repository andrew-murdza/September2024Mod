package net.amurdza.examplemod.gametest;

import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import dev.hybridlabs.hapi.item.CreatureNetItem;
import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.RetainedCreatureNets;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreatureNetCreativeTests {
    private static final Set<String> EXPECTED_CAPTURE_SPECIES = Set.of(
            "hybrid_aquatic:arrow_squid", "hybrid_aquatic:octopus",
            "hybrid_aquatic:umbrella_octopus",
            "hybrid_aquatic:spider_crab", "hybrid_aquatic:starfish", "hybrid_aquatic:tuna",
            "hybrid_aquatic:vampire_squid", "hybrid_aquatic:hound_shark", "hybrid_aquatic:fiddler_crab");

    private CreatureNetCreativeTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void retiredNetsKeepSavedCreatureDataButStayOutOfCreative(GameTestHelper helper) {
        Set<String> actual = new HashSet<>();
        for (var stack : RetainedCreatureNets.filledNets()) {
            helper.assertTrue(CreatureNetItem.Companion.alreadyHasCreature(stack) && !RetainedContent.isKeptItem(stack),
                    "Saved native net data must remain readable while every filled net is hidden");
            var type = CreatureNetItem.Companion.getEntityFromNBT(stack.getTag()).orElseThrow();
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            helper.assertTrue(id.getNamespace().equals("hybrid_aquatic") && actual.add(id.toString()),
                    "Filled nets must contain each selected Hybrid Aquatic species once");
            var creature = (BaseWaterAnimal) type.create(helper.getLevel());
            try {
                // Native CreatureNetItem releases by creating the selected type
                // and loading this exact storedEntity compound.
                creature.load(stack.getTag().getCompound("storedEntity"));
                helper.assertTrue(creature.isAlive() && creature.getHealth() > 0 && creature.getMoistness() > 0
                                && creature.getHunger() == BaseWaterAnimal.MAX_HUNGER,
                        "Creative net release must produce a living hydrated creature: " + id);
                helper.assertTrue(creature.getFromCreatureNet() && creature.isPersistenceRequired(),
                        "The loaded creature must retain its native captured/persistent state");
            } finally {
                creature.discard();
            }
        }
        helper.assertTrue(actual.equals(EXPECTED_CAPTURE_SPECIES),
                "Filled nets must include exactly the nine retained Hybrid Aquatic species: " + actual);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void allNativeCreativeTabsRespectSelectionAndExposeCaptureForms(GameTestHelper helper) throws Exception {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        Map<String, Set<String>> contents = new TreeMap<>();
        Set<ResourceLocation> displayed = new HashSet<>();
        Set<String> netSpecies = new TreeSet<>();
        var excluded = new ArrayList<String>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            String tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
            Set<String> entries = new TreeSet<>();
            for (ItemStack stack : tab.getDisplayItems()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                displayed.add(id);
                String description = id.toString();
                if (stack.getItem() instanceof CreatureNetItem && stack.hasTag()
                        && CreatureNetItem.Companion.alreadyHasCreature(stack)) {
                    String species = stack.getTag().getCompound("storedEntity").getString("id");
                    description += "[" + species + "]";
                    netSpecies.add(species);
                }
                entries.add(description);
                if (!RetainedContent.isKeptItem(stack)) excluded.add(tabId + ": " + description);
            }
            contents.put(tabId, entries);
        }
        Set<String> missingRetainedItems = new TreeSet<>();
        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            if (id.getNamespace().equals("minecraft") || id.getNamespace().equals(AOEMod.MOD_ID)) continue;
            if (!displayed.contains(id) && RetainedContent.isKeptItem(new ItemStack(entry.getValue())))
                missingRetainedItems.add(id.toString());
        }
        Map<String, Object> report = new TreeMap<>();
        report.put("tabs", contents);
        report.put("excludedItemsStillDisplayed", excluded);
        report.put("retainedDependencyItemsMissingFromAllCategoryTabs", missingRetainedItems);
        report.put("filledCreatureNetSpecies", netSpecies);
        Files.writeString(Path.of("retained-creative-inventory-audit.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        helper.assertTrue(excluded.isEmpty(), "No native creative tab may leak excluded content: " + excluded);
        helper.assertTrue(netSpecies.isEmpty(),
                "Retired Hybrid Aquatic nets must be absent from every creative tab: " + netSpecies);
        for (String tab : Set.of("vanillabackport:vanilla_backport", "aoemod:upgrade_aquatic", "aoemod:quark")) {
            helper.assertTrue(contents.containsKey(tab) && !contents.get(tab).isEmpty(), "Requested creative tab must exist and contain retained items: " + tab);
        }
        helper.succeed();
    }
}
