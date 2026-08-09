package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.model.BabyParrotModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

final class BabyParrotGeoRenderer extends GeoReplacedEntityRenderer<Parrot, BabyReplacedEntity<Parrot>> {
    BabyParrotGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new BabyParrotModel(), new BabyReplacedEntity<>(EntityType.PARROT));
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(
            Parrot parrot,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        this.animatable.setCurrentEntity(parrot);
        super.render(parrot, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
