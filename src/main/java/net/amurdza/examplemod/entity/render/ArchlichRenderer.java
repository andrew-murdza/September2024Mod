package net.amurdza.examplemod.entity.render;

import net.amurdza.examplemod.entity.ArchlichEntity;
import net.amurdza.examplemod.entity.model.ArchlichModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ArchlichRenderer extends GeoEntityRenderer<ArchlichEntity> {
    public ArchlichRenderer(EntityRendererProvider.Context context) {
        super(context, new ArchlichModel());
        this.shadowRadius = 0.5F;
    }
}
