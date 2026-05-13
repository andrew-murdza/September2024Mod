package net.amurdza.examplemod.worldgen.dimension;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class NetherFog extends DimensionSpecialEffects {
    public NetherFog() {
        super(192.0F, true, SkyType.NORMAL, false, false);
    }

    private static boolean isNetherLayer() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return camera.getPosition().y < 1.0D;
    }

    @Override
    public @NotNull Vec3 getBrightnessDependentFogColor(@NotNull Vec3 fogColor, float brightness) {
        if (isNetherLayer()) {
            return fogColor;
        }

        return fogColor.multiply(
                brightness * 0.94F + 0.06F,
                brightness * 0.94F + 0.06F,
                brightness * 0.91F + 0.09F
        );
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return y < 1;
    }
}