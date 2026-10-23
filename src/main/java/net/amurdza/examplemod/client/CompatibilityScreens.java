package net.amurdza.examplemod.client;

import com.teamabnormals.environmental.client.gui.screens.inventory.SlabfishInventoryScreen;
import com.teamabnormals.environmental.core.registry.EnvironmentalMenuTypes;
import com.teamabnormals.environmental.common.inventory.SlabfishInventoryMenu;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.mixins.othermods.environmental.SlabfishMenuAccess;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Complete the upstream screen registry with Environmental's existing inventory UI. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CompatibilityScreens {
    private CompatibilityScreens() { }

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.<SlabfishInventoryMenu, SlabfishInventoryScreen>register(EnvironmentalMenuTypes.SLABFISH_INVENTORY.get(),
                (menu, inventory, title) -> new SlabfishInventoryScreen(menu, inventory,
                        ((SlabfishMenuAccess) menu).aoe$getSlabfish())));
    }
}
