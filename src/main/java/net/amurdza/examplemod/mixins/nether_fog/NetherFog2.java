package net.amurdza.examplemod.mixins.nether_fog;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FogRenderer.class)
public class NetherFog2 {
    @Unique
    private static final ResourceLocation september2024Mod$AOEDIM = new ResourceLocation(AOEMod.MOD_ID, "aoedim");

    @Unique
    private static boolean aoemod$isNetherLayer(ClientLevel level, Camera camera) {
        return level.dimension().location().equals(september2024Mod$AOEDIM)
                && camera.getPosition().y < 1.0D;
    }
}