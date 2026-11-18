package net.amurdza.examplemod.entity.render;

import com.blackgear.vanillabackport.client.level.model.entity.cow.ColdCowModel;
import net.amurdza.examplemod.entity.YakEntity;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Retains the cold cow's shaggy native geometry while the entity uses horse mechanics. */
public final class YakRenderer extends MobRenderer<YakEntity, CowModel<YakEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/cow/cold_cow.png");
    private final YakSaddleLayer saddleLayer;
    private final YakPannierLayer pannierLayer;
    public YakRenderer(EntityRendererProvider.Context context) {
        super(context, new CowModel<>(ColdCowModel.createBodyLayer().bakeRoot()), .7F);
        saddleLayer = new YakSaddleLayer(this, context);
        addLayer(saddleLayer);
        pannierLayer = new YakPannierLayer(this, context);
        addLayer(pannierLayer);
    }
    public YakSaddleLayer saddleLayer() { return saddleLayer; }
    public YakPannierLayer pannierLayer() { return pannierLayer; }
    @Override public ResourceLocation getTextureLocation(YakEntity yak) { return TEXTURE; }
}
