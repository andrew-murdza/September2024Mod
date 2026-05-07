package net.amurdza.examplemod.worldgen.dimension;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class NetherFog extends DimensionSpecialEffects.OverworldEffects {
    private static boolean isNetherLayer() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return camera.getPosition().y < 1.0D;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        if (isNetherLayer()) {
            return fogColor; // Nether behavior
        }

        return super.getBrightnessDependentFogColor(fogColor, brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return isNetherLayer(); // Nether behavior below y=1
    }

    @Override
    public boolean renderSky(
            ClientLevel level,
            int ticks,
            float partialTick,
            PoseStack poseStack,
            Camera camera,
            Matrix4f projectionMatrix,
            boolean isFoggy,
            Runnable setupFog
    ) {
        if (isNetherLayer()) {
            return true;
        }

        return super.renderSky(level, ticks, partialTick, poseStack, camera, projectionMatrix, isFoggy, setupFog);
    }

    @Override
    public boolean renderClouds(
            ClientLevel level,
            int ticks,
            float partialTick,
            PoseStack poseStack,
            double camX,
            double camY,
            double camZ,
            Matrix4f projectionMatrix
    ) {
        if (isNetherLayer()) {
            return true;
        }

        return super.renderClouds(level, ticks, partialTick, poseStack, camX, camY, camZ, projectionMatrix);
    }
}