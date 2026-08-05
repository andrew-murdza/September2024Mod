package net.amurdza.examplemod.entity.render.future;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.NautilusEntity;
import net.amurdza.examplemod.entity.model.future.NautilusModel;
import net.amurdza.examplemod.entity.render.EntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class NautilusRenderer extends MobRenderer<NautilusEntity, NautilusModel> {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/nautilus.png");

    public NautilusRenderer(EntityRendererProvider.Context context) {
        super(context, new NautilusModel(context.bakeLayer(EntityRenderers.NAUTILUS_MODEL)), 0.7F);
    }

    @Override
    protected void scale(NautilusEntity entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(NautilusEntity entity) {
        return TEXTURE;
    }
}
