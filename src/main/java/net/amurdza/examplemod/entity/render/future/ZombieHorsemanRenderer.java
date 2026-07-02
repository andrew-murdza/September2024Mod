package net.amurdza.examplemod.entity.render.future;

import net.amurdza.examplemod.entity.future.ZombieHorsemanEntity;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ZombieHorsemanRenderer extends MobRenderer<ZombieHorsemanEntity, HorseModel<ZombieHorsemanEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "minecraft",
            "textures/entity/horse/horse_zombie.png"
    );

    public ZombieHorsemanRenderer(EntityRendererProvider.Context context) {
        super(context, new HorseModel<>(context.bakeLayer(ModelLayers.ZOMBIE_HORSE)), 0.75F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(ZombieHorsemanEntity entity) {
        return TEXTURE;
    }
}
