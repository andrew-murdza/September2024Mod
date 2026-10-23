package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
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
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class ModDimensionEffects extends DimensionSpecialEffects {

    public static final ResourceLocation OVERWORLD_REGIONS = new ResourceLocation(AOEMod.MOD_ID, "overworld_regions");
    public static final ResourceLocation NETHER_REGIONS = new ResourceLocation(AOEMod.MOD_ID, "nether_regions");
    private final boolean nether;

    public ModDimensionEffects(boolean nether) {
        super(nether ? Float.NaN : 256.0F, !nether, nether ? SkyType.NONE : SkyType.NORMAL, nether, false);
        this.nether = nether;
    }

    @Override
    public @NotNull Vec3 getBrightnessDependentFogColor(@NotNull Vec3 fogColor, float brightness) {
        if (nether) {
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
        return nether;
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(OVERWORLD_REGIONS, new ModDimensionEffects(false));
        event.register(NETHER_REGIONS, new ModDimensionEffects(true));
    }
}
