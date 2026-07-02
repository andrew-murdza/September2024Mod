package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.IllagerLordEntity;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class IllagerLordRenderer extends MobRenderer<IllagerLordEntity, IllagerModel<IllagerLordEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "minecraft",
            "textures/entity/illager/vindicator.png"
    );

    public IllagerLordRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.VINDICATOR)), 0.6F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(IllagerLordEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.15F, 1.15F, 1.15F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(IllagerLordEntity entity) {
        return TEXTURE;
    }
}
