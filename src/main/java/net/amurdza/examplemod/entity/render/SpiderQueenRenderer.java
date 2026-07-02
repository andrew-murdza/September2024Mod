package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.SpiderQueenEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SpiderQueenRenderer extends MobRenderer<SpiderQueenEntity, SpiderModel<SpiderQueenEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "minecraft",
            "textures/entity/spider/spider.png"
    );

    public SpiderQueenRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.9F);
    }

    @Override
    protected void scale(SpiderQueenEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.65F, 1.65F, 1.65F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(SpiderQueenEntity entity) {
        return TEXTURE;
    }
}
