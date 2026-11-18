package net.amurdza.examplemod.entity.render.future;

import net.amurdza.examplemod.entity.future.CreakingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Legacy saved AOE entities share the native model, gaze animation, and active-eye rendering. */
public class CreakingRenderer extends com.blackgear.vanillabackport.client.level.renderer.entity.mob.CreakingRenderer<CreakingEntity> {
    public CreakingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
