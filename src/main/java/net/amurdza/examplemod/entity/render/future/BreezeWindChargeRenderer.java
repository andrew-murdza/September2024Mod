package net.amurdza.examplemod.entity.render.future;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.BreezeWindChargeEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class BreezeWindChargeRenderer extends EntityRenderer<BreezeWindChargeEntity> {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/breeze.png");

    public BreezeWindChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BreezeWindChargeEntity entity) {
        return TEXTURE;
    }
}
