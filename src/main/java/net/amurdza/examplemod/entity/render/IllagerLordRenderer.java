package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.entity.IllagerLordEntity;
import net.amurdza.examplemod.entity.model.IllagerLordModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IllagerLordRenderer extends GeoEntityRenderer<IllagerLordEntity> {
    public IllagerLordRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerLordModel());
        this.shadowRadius = 0.6F;
    }
}
