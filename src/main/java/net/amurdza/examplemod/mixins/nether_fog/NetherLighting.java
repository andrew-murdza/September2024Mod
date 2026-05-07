package net.amurdza.examplemod.mixins.nether_fog;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public class NetherLighting {
    @Unique
    private static final ResourceLocation september2024Mod$AOEDIM =
            new ResourceLocation(AOEMod.MOD_ID, "aoedim");

    @ModifyExpressionValue(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F"
            )
    )
    private float aoemod$useNetherBrightnessBelowY1(float original) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            return original;
        }

        if (!mc.level.dimension().location().equals(september2024Mod$AOEDIM)) {
            return original;
        }

        double y = mc.gameRenderer.getMainCamera().getPosition().y;

        if (y < 1.0D) {
            // Nether dimension_type ambient_light = 0.1
            return Math.max(original, 0.1F);
        }

        return original;
    }
}
