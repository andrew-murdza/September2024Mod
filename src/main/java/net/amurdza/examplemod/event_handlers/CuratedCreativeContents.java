package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.util.CreativeCatalog;
import net.amurdza.examplemod.registry.ModCreativeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CuratedCreativeContents {
    private CuratedCreativeContents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void filter(BuildCreativeModeTabContentsEvent event) {
        String catalog = ModCreativeTabs.namespaceFor(event.getTabKey().location());
        boolean category = event.getTab().getType() == CreativeModeTab.Type.CATEGORY;
        boolean vanilla = event.getTabKey().location().getNamespace().equals("minecraft");
        var entries = event.getEntries().iterator();
        while (entries.hasNext()) {
            var stack = entries.next().getKey();
            String owner = CreativeCatalog.owner(stack);
            if (!RetainedContent.isKeptItem(stack) || category &&
                    (catalog != null ? !catalog.equals(owner) : !vanilla || !owner.equals("minecraft"))) entries.remove();
        }
    }
}
