package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.DocumentedBiomeModifier;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DocumentedEcologyRegistries {
    private DocumentedEcologyRegistries() { }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.FEATURE, new ResourceLocation(AOEMod.MOD_ID, "documented_flora"), DocumentedFloraFeature::new);
        event.register(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, new ResourceLocation(AOEMod.MOD_ID, "documented_ecology"), () -> DocumentedBiomeModifier.CODEC);
    }
}
