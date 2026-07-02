package net.amurdza.examplemod.entity.render.future;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.CreakingEntity;
import net.amurdza.examplemod.entity.model.future.CreakingModel;
import net.amurdza.examplemod.entity.render.EntityRenderers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CreakingRenderer extends MobRenderer<CreakingEntity, CreakingModel> {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/creaking.png");
    private static final ResourceLocation EYES = AOEMod.makeID("textures/entity/creaking_eyes.png");

    public CreakingRenderer(EntityRendererProvider.Context context) {
        super(context, new CreakingModel(context.bakeLayer(EntityRenderers.CREAKING_MODEL)), 0.5F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public @NotNull RenderType renderType() {
                return RenderType.eyes(EYES);
            }
        });
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CreakingEntity entity) {
        return TEXTURE;
    }
}
