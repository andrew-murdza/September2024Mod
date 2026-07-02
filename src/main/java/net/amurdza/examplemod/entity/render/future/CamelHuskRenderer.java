package net.amurdza.examplemod.entity.render.future;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.CamelHuskEntity;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CamelHuskRenderer<T extends CamelHuskEntity> extends MobRenderer<T, CamelModel<T>> {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/camel_husk.png");

    public CamelHuskRenderer(EntityRendererProvider.Context context) {
        super(context, new CamelModel<>(context.bakeLayer(ModelLayers.CAMEL)), 0.7F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
