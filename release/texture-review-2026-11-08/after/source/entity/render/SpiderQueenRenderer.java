package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.SpiderQueenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;

public class SpiderQueenRenderer extends SpiderRenderer<SpiderQueenEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/spider/spider.png");
    private static final float KING_SPIDER_SCALE = 1.9F;

    public SpiderQueenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiderQueenEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SpiderQueenEntity entity, PoseStack poseStack, float partialTick) {
        // Twilight Forest's king spider uses the vanilla spider model at this scale.
        poseStack.scale(KING_SPIDER_SCALE, KING_SPIDER_SCALE, KING_SPIDER_SCALE);
    }
}
