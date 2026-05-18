//package net.amurdza.examplemod.entity.render;
//
//import net.amurdza.examplemod.AOEMod;
//import net.amurdza.examplemod.entity.SeaSerpentEntity;
//import net.amurdza.examplemod.entity.model.ModModelLayers;
//import net.amurdza.examplemod.entity.model.SeaSerpentModel;
//import net.minecraft.client.renderer.entity.EntityRendererProvider;
//import net.minecraft.client.renderer.entity.MobRenderer;
//import net.minecraft.resources.ResourceLocation;
//import org.jetbrains.annotations.NotNull;
//
//
//public class RendererSeaSerpent extends MobRenderer<SeaSerpentEntity, SeaSerpentModel<SeaSerpentEntity>> {
//    private static final ResourceLocation TEXTURE =
//            AOEMod.makeID("textures/entity/sea_serpent.png");
//
//    public RendererSeaSerpent(EntityRendererProvider.Context context) {
//        super(context, new SeaSerpentModel<>(context.bakeLayer(ModModelLayers.SEA_SERPENT)), 0.0F);
//    }
//
//    @Override
//    public @NotNull ResourceLocation getTextureLocation(@NotNull SeaSerpentEntity entity) {
//        return TEXTURE;
//    }
//}
