package net.amurdza.examplemod.entity.render;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ModEntities;
import net.amurdza.examplemod.entity.model.CubozoaEntityModel;
import net.amurdza.examplemod.entity.model.EndFishEntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

import java.util.function.Function;

public class EntityRenders
{
    public static final ModelLayerLocation END_FISH_MODEL = registerMain("endfish");
    public static final ModelLayerLocation CUBOZOA_MODEL = registerMain("cubozoa");

    private static ModelLayerLocation registerMain(String id) {
        return new ModelLayerLocation(AOEMod.makeID(id), "main");
    }
    public static void register(){
        register(ModEntities.END_FISH.get(), RendererEntityEndFish::new);
        register(ModEntities.CUBOZOA.get(), RendererEntityCubozoa::new);

        EntityModelLayerRegistry.register(END_FISH_MODEL, EndFishEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.register(CUBOZOA_MODEL, CubozoaEntityModel::getTexturedModelData);
    }
    private static void register(EntityType<?> type, Function<Context, MobRenderer> renderer) {
        EntityRendererRegistry.register(()->type, (context) -> renderer.apply(context));
    }
}
