package net.amurdza.examplemod.mixins.othermods.embeddium;

import net.amurdza.examplemod.worldgen.TropicalPlantLighting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.world.WorldSlice", remap = false)
public abstract class WorldSlicePlantLightContext implements TropicalPlantLighting.DimensionContext {
    @Shadow @Final public ClientLevel world;

    @Override
    public ResourceKey<Level> aoemod$lightingDimension() { return world.dimension(); }
}
