package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiomeManager.class)
public abstract class BiomeManagerPreciseYPosMixin {

    @Redirect(
            method = "getBiome",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/BiomeManager$NoiseBiomeSource;getNoiseBiome(III)Lnet/minecraft/core/Holder;"
            )
    )
    private Holder<Biome> aoe$useBlockYForNoiseBiomeY(
            BiomeManager.NoiseBiomeSource noiseBiomeSource,
            int x,
            int originalY,
            int z,
            BlockPos pos
    ) {
        return noiseBiomeSource.getNoiseBiome(x, QuartPos.fromBlock(pos.getY()), z);
    }
}