package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.entity.model.CubozoaEntityModel;
import net.amurdza.examplemod.entity.model.EndFishEntityModel;
import net.amurdza.examplemod.entity.model.SeaSerpentEntityModel;
import net.amurdza.examplemod.entity.model.future.BoggedModel;
import net.amurdza.examplemod.entity.model.future.BreezeModel;
import net.amurdza.examplemod.entity.model.future.BreezeWindModel;
import net.amurdza.examplemod.entity.model.future.CreakingModel;
import net.amurdza.examplemod.entity.model.future.NautilusModel;
import net.amurdza.examplemod.entity.model.future.ParchedModel;
import net.amurdza.examplemod.entity.render.future.*;
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

    public static final ModelLayerLocation BOGGED_MODEL =
            new ModelLayerLocation(AOEMod.makeID("bogged"), "main");
    public static final ModelLayerLocation BOGGED_OVERLAY_MODEL =
            new ModelLayerLocation(AOEMod.makeID("bogged"), "overlay");
    public static final ModelLayerLocation BREEZE_MODEL =
            new ModelLayerLocation(AOEMod.makeID("breeze"), "main");
    public static final ModelLayerLocation BREEZE_WIND_MODEL =
            new ModelLayerLocation(AOEMod.makeID("breeze"), "wind");
    public static final ModelLayerLocation CREAKING_MODEL =
            new ModelLayerLocation(AOEMod.makeID("creaking"), "main");
    public static final ModelLayerLocation NAUTILUS_MODEL =
            new ModelLayerLocation(AOEMod.makeID("nautilus"), "main");
    public static final ModelLayerLocation PARCHED_MODEL =
            new ModelLayerLocation(AOEMod.makeID("parched"), "main");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ARCHLICH.get(), ArchlichRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIDER_QUEEN.get(), SpiderQueenRenderer::new);
        event.registerEntityRenderer(ModEntities.ILLAGER_LORD.get(), IllagerLordRenderer::new);
        event.registerEntityRenderer(ModEntities.BOGGED.get(), BoggedRenderer::new);
        event.registerEntityRenderer(ModEntities.PARCHED.get(), ParchedRenderer::new);
        event.registerEntityRenderer(ModEntities.BREEZE.get(), BreezeRenderer::new);
        event.registerEntityRenderer(ModEntities.BREEZE_WIND_CHARGE.get(), BreezeWindChargeRenderer::new);
        event.registerEntityRenderer(ModEntities.CREAKING.get(), CreakingRenderer::new);
        event.registerEntityRenderer(ModEntities.CAMEL_HUSK.get(), CamelHuskRenderer::new);
        event.registerEntityRenderer(ModEntities.CAMEL_HUSK_JOCKEY.get(), CamelHuskRenderer::new);
        event.registerEntityRenderer(ModEntities.ZOMBIE_HORSEMAN.get(), ZombieHorsemanRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON_HORSEMAN.get(), SkeletonHorsemanRenderer::new);
        event.registerEntityRenderer(ModEntities.NAUTILUS.get(), NautilusRenderer::new);
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
        event.registerLayerDefinition(BOGGED_MODEL, BoggedModel::createBodyLayer);
        event.registerLayerDefinition(BOGGED_OVERLAY_MODEL, BoggedModel::createBodyLayer);
        event.registerLayerDefinition(BREEZE_MODEL, BreezeModel::createBodyLayer);
        event.registerLayerDefinition(BREEZE_WIND_MODEL, BreezeWindModel::createBodyLayer);
        event.registerLayerDefinition(CREAKING_MODEL, CreakingModel::createBodyLayer);
        event.registerLayerDefinition(NAUTILUS_MODEL, NautilusModel::createBodyLayer);
        event.registerLayerDefinition(PARCHED_MODEL, ParchedModel::createBodyLayer);

    }
}
