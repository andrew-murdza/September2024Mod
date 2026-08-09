package net.amurdza.examplemod.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class CloudsRequirePrecipitation {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void aoemod$hideCloudsInDryBiomes(
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            float partialTick,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockPos cameraPos = BlockPos.containing(cameraX, cameraY, cameraZ);
        if (!level.getBiome(cameraPos).value().hasPrecipitation()) {
            ci.cancel();
        }
    }
}
