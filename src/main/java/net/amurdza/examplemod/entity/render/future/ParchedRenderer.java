package net.amurdza.examplemod.entity.render.future;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.ParchedEntity;
import net.amurdza.examplemod.entity.model.future.ParchedModel;
import net.amurdza.examplemod.entity.render.EntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ParchedRenderer extends MobRenderer<ParchedEntity, ParchedModel> {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/parched.png");

    public ParchedRenderer(EntityRendererProvider.Context context) {
        super(context, new ParchedModel(context.bakeLayer(EntityRenderers.PARCHED_MODEL)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(ParchedEntity entity) {
        return TEXTURE;
    }
}
