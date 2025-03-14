package net.amurdza.examplemod.mixins.othermods.alexscaves;

import com.github.alexmodguy.alexscaves.server.level.structure.DinoBowlStructure;
import net.amurdza.examplemod.worldgen.biome.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DinoBowlStructure.class)
public class Caves {
    @Redirect(method = "<init>",at= @At(value = "FIELD", target = "Lcom/github/alexmodguy/alexscaves/server/level/biome/ACBiomeRegistry;PRIMORDIAL_CAVES:Lnet/minecraft/resources/ResourceKey;"),remap = false)
    private static ResourceKey<Biome> hi(){
        return ModBiomes.RAINFOREST;
    }
}
