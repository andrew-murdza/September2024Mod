package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.entity.SpiderQueenEntity;
import net.amurdza.examplemod.entity.model.SpiderQueenModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class SpiderQueenRenderer extends GeoEntityRenderer<SpiderQueenEntity> {
    public SpiderQueenRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderQueenModel());
        this.shadowRadius = 0.9F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
