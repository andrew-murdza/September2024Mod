package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.util.CreativeCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.ModList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AOEMod.MOD_ID);
    private static final Map<String, RegistryObject<CreativeModeTab>> CATALOGS = new LinkedHashMap<>();

    public static final RegistryObject<CreativeModeTab> AOE_MOD = CREATIVE_MODE_TABS.register("aoe_mod",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aoemod"))
                    .icon(() -> new ItemStack(ModBlocks.PALE_OAK_SAPLING.get()))
                    .displayItems((parameters, output) -> CreativeCatalog.items(AOEMod.MOD_ID).forEach(output::accept))
                    .build());

    public static final RegistryObject<CreativeModeTab> UPGRADE_AQUATIC = curated("upgrade_aquatic", "Upgrade Aquatic", "star_coral");
    public static final RegistryObject<CreativeModeTab> ENVIRONMENTAL = curated("environmental", "Environmental", "blue_delphinium");
    public static final RegistryObject<CreativeModeTab> QUARK = curated("quark", "Quark", "ancient_tome");
    public static final RegistryObject<CreativeModeTab> LEGUME_DELIGHT = curated("legumedelight", "Legume Delight", "peanuts");

    static {
        RetainedContent.namespaces().stream().sorted()
                .filter(namespace -> !namespace.equals("minecraft") && !namespace.equals(AOEMod.MOD_ID)
                        && !namespace.equals("betterend") && !CATALOGS.containsKey(namespace))
                .forEach(namespace -> curated(namespace, displayName(namespace), null));
        // Keep the native Backport tab: its identity activates the per-update sidebar.
    }

    private static RegistryObject<CreativeModeTab> curated(String namespace, String title, String icon) {
        RegistryObject<CreativeModeTab> tab = CREATIVE_MODE_TABS.register(namespace, () -> CreativeModeTab.builder()
                .title(Component.literal(title))
                .icon(() -> icon == null ? CreativeCatalog.items(namespace).findFirst().orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER))
                        : new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, icon))))
                .displayItems((parameters, output) -> CreativeCatalog.items(namespace).forEach(output::accept))
                .build());
        CATALOGS.put(namespace, tab);
        return tab;
    }

    private static String displayName(String namespace) {
        if (namespace.equals("collectorsreap")) return "Collector's Reap";
        if (namespace.equals("culturaldelights")) return "Cultural Delights";
        if (namespace.equals("hearthandharvest")) return "Hearth and Harvest";
        if (namespace.equals("netherdepthsupgrade")) return "Nether Depths Delight";
        return ModList.get().getModContainerById(namespace).map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace.replace('_', ' '));
    }

    public static String namespaceFor(ResourceLocation tab) {
        if (tab.equals(new ResourceLocation("vanillabackport", "vanilla_backport"))) return "vanillabackport";
        if (tab.equals(AOE_MOD.getId())) return AOEMod.MOD_ID;
        return CATALOGS.entrySet().stream().filter(entry -> entry.getValue().getId().equals(tab))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static Map<String, RegistryObject<CreativeModeTab>> catalogs() {
        return Map.copyOf(CATALOGS);
    }

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
