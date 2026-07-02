package net.amurdza.examplemod.entity.render.future;

import net.amurdza.examplemod.entity.future.SkeletonHorsemanEntity;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SkeletonHorsemanRenderer extends MobRenderer<SkeletonHorsemanEntity, HorseModel<SkeletonHorsemanEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "minecraft",
            "textures/entity/horse/horse_skeleton.png"
    );

    public SkeletonHorsemanRenderer(EntityRendererProvider.Context context) {
        super(context, new HorseModel<>(context.bakeLayer(ModelLayers.SKELETON_HORSE)), 0.75F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(SkeletonHorsemanEntity entity) {
        return TEXTURE;
    }
}
