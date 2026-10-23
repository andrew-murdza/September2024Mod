package net.amurdza.examplemod.client;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.DynamicFluidContainerModel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BoneBucketColors {
    private BoneBucketColors() { }

    @SubscribeEvent
    public static void register(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), ModItems.BONE_WATER_BUCKET.get(), ModItems.BONE_LAVA_BUCKET.get());
    }
}
