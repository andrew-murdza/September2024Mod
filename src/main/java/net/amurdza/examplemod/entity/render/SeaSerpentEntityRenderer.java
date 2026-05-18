package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.model.SeaSerpentEntityModel;
import net.amurdza.examplemod.entity.sea_serpent.SeaSerpentEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class SeaSerpentEntityRenderer extends MobRenderer<SeaSerpentEntity, SeaSerpentEntityModel<SeaSerpentEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AOEMod.MOD_ID, "textures/entity/sea_serpent.png");

    public SeaSerpentEntityRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new SeaSerpentEntityModel<>(context.bakeLayer(EntityRenderers.SEA_SERPENT_MODEL)),
                0.0F
        );
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SeaSerpentEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(@NotNull SeaSerpentEntity living, @NotNull PoseStack poseStack, float partialTick) {
        super.scale(living, poseStack, partialTick);

        float x = Mth.cos(((float)(living.tickCount + living.randNumTick) + partialTick) * 0.16F);
        float y = Mth.sin(((float)(living.tickCount + living.randNumTick) + partialTick) * 0.12F);
        float z = Mth.sin(((float)(living.tickCount + living.randNumTick) + partialTick) * 0.08F);

        if (living.isInWater() && !living.isVehicle()) {
            poseStack.translate(x * 0.04F, y * 0.04F, z * 0.04F);
        }
    }

    @Override
    protected void setupRotations(
            @NotNull SeaSerpentEntity living,
            @NotNull PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTicks
    ) {
        super.setupRotations(living, poseStack, ageInTicks, rotationYaw, partialTicks);

        if (living.getRotatePitch()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-living.getCurrentPitch(partialTicks)));
        }
    }
}