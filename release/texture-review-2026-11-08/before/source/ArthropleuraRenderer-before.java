package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.entity.ArthropleuraEntity;
import net.amurdza.examplemod.entity.model.ArthropleuraModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ArthropleuraRenderer extends MobRenderer<ArthropleuraEntity, ArthropleuraModel> {
    private static final ResourceLocation MATERIAL = new ResourceLocation("minecraft", "textures/block/white_concrete.png");

    public ArthropleuraRenderer(EntityRendererProvider.Context context) {
        super(context, new ArthropleuraModel(context.bakeLayer(EntityRenderers.ARTHROPLEURA_MODEL)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(ArthropleuraEntity entity) { return MATERIAL; }
}
