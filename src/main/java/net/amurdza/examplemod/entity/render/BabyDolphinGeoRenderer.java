package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.model.BabyDolphinModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Dolphin;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

final class BabyDolphinGeoRenderer extends GeoReplacedEntityRenderer<Dolphin, BabyReplacedEntity<Dolphin>> {
    BabyDolphinGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new BabyDolphinModel(), new BabyReplacedEntity<>(EntityType.DOLPHIN));
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(
            Dolphin dolphin,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        this.animatable.setCurrentEntity(dolphin);
        super.render(dolphin, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
