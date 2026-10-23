package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.entity.SwordfishEntity;
import net.amurdza.examplemod.entity.model.SwordfishModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SwordfishRenderer extends MobRenderer<SwordfishEntity, SwordfishModel> {
    private static final ResourceLocation MATERIAL = new ResourceLocation("minecraft", "textures/block/white_concrete.png");

    public SwordfishRenderer(EntityRendererProvider.Context context) {
        super(context, new SwordfishModel(context.bakeLayer(EntityRenderers.SWORDFISH_MODEL)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordfishEntity entity) { return MATERIAL; }
}
