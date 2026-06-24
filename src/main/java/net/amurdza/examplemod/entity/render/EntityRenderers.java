package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.entity.model.CubozoaEntityModel;
import net.amurdza.examplemod.entity.model.EndFishEntityModel;
import net.amurdza.examplemod.entity.model.SeaSerpentEntityModel;
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
public class EntityRenderers {
    public static final ModelLayerLocation END_FISH_MODEL =
            new ModelLayerLocation(AOEMod.makeID("endfish"), "main");

    public static final ModelLayerLocation CUBOZOA_MODEL =
            new ModelLayerLocation(AOEMod.makeID("cubozoa"), "main");

    public static final ModelLayerLocation SEA_SERPENT_MODEL =
            new ModelLayerLocation(AOEMod.makeID("sea_serpent"), "main");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ARCHLICH.get(), ArchlichRenderer::new);
        event.registerEntityRenderer(ModEntities.END_FISH.get(), EndFishEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBOZOA.get(), CubozoaEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SEA_SERPENT.get(), SeaSerpentEntityRenderer::new);
    }

//    @SubscribeEvent
//    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
//        event.registerLayerDefinition(ModModelLayers.SEA_SERPENT, SeaSerpentModel::createBodyLayer);
//    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(END_FISH_MODEL, EndFishEntityModel::getTexturedModelData);
        event.registerLayerDefinition(CUBOZOA_MODEL, CubozoaEntityModel::getTexturedModelData);
        event.registerLayerDefinition(SEA_SERPENT_MODEL, SeaSerpentEntityModel::createBodyLayer);

    }
}
