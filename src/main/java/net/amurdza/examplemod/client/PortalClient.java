package net.amurdza.examplemod.client;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.portal.WoodenPortalBlock;
import net.amurdza.examplemod.registry.ModPortals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PortalClient {
    private PortalClient() { }

    public static void open(UUID token, int destinations) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.level != null) {
            minecraft.setScreen(new PortalSelectionScreen(token, destinations));
        }
    }

    @SubscribeEvent
    public static void packs(net.minecraftforge.event.AddPackFindersEvent event) {
        PortalTexturePack.register(event);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var block : ModPortals.blocks()) {
                ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent());
            }
        });
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> ((WoodenPortalBlock) state.getBlock()).destination().color(),
                ModPortals.blocks());
    }
}
