package net.amurdza.examplemod.mixins.nether_fog;

import com.mojang.blaze3d.systems.RenderSystem;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class NetherFog2 {
    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @Unique
    private static final ResourceLocation september2024Mod$AOEDIM = new ResourceLocation(AOEMod.MOD_ID, "aoedim");

    @Unique
    private static boolean aoemod$isNetherLayer(ClientLevel level, Camera camera) {
        return level.dimension().location().equals(september2024Mod$AOEDIM)
                && camera.getPosition().y < 1.0D;
    }

    @Inject(
            method = "setupColor",
            at = @At("RETURN")
    )
    private static void aoemod$netherFogColorBelowY1(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            float bossColorModifier,
            CallbackInfo ci
    ) {
        if (!aoemod$isNetherLayer(level, camera)) {
            return;
        }

        // Nether-like reddish fog color.
        fogRed = 0.20F;
        fogGreen = 0.03F;
        fogBlue = 0.03F;
    }

    @Inject(
            method = "setupFog",
            at = @At("RETURN")
    )
    private static void aoemod$netherFogDistanceBelowY1(
            Camera camera,
            FogRenderer.FogMode fogMode,
            float farPlaneDistance,
            boolean nearFog,
            float partialTick,
            CallbackInfo ci
    ) {
        Level level = camera.getEntity().level();

        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        if (!aoemod$isNetherLayer(clientLevel, camera)) {
            return;
        }

        if (fogMode == FogRenderer.FogMode.FOG_SKY) {
            RenderSystem.setShaderFogStart(0.0F);
            RenderSystem.setShaderFogEnd(48.0F);
        } else {
            RenderSystem.setShaderFogStart(0.0F);
            RenderSystem.setShaderFogEnd(96.0F);
        }
    }
}