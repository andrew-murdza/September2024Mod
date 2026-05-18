package net.amurdza.examplemod.worldgen.dimension;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = AOEMod.MOD_ID,
        value = {Dist.CLIENT}
)
public class NetherFog extends DimensionSpecialEffects {

    public NetherFog() {
        super(192.0F, true, SkyType.NORMAL, false, false);
    }

    private static boolean isNetherLayer() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return camera.getPosition().y < 1.0D;
    }

    @Override
    public @NotNull SkyType skyType() {
        return isNetherLayer() ? SkyType.NONE : SkyType.NORMAL;
    }

    @Override
    public boolean constantAmbientLight() {
        return isNetherLayer();
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
        return y<1;
    }

    @SubscribeEvent
    public static void hi(RegisterDimensionSpecialEffectsEvent event){
        event.register(new ResourceLocation(AOEMod.MOD_ID,"aoedim"),new NetherFog());
    }
}