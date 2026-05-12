package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ModEntities;
import net.amurdza.examplemod.entity.model.CubozoaEntityModel;
import net.amurdza.examplemod.entity.model.EndFishEntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AOEMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class EntityRenders {
    public static final ModelLayerLocation END_FISH_MODEL =
            new ModelLayerLocation(AOEMod.makeID("endfish"), "main");

    public static final ModelLayerLocation CUBOZOA_MODEL =
            new ModelLayerLocation(AOEMod.makeID("cubozoa"), "main");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.END_FISH.get(), RendererEntityEndFish::new);
        event.registerEntityRenderer(ModEntities.CUBOZOA.get(), RendererEntityCubozoa::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(END_FISH_MODEL, EndFishEntityModel::getTexturedModelData);
        event.registerLayerDefinition(CUBOZOA_MODEL, CubozoaEntityModel::getTexturedModelData);
    }
}
