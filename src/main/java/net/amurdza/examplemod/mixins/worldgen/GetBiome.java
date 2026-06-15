package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(BiomeManager.class)
public abstract class GetBiome {

    @Shadow @Final private BiomeManager.NoiseBiomeSource noiseBiomeSource;

    @Inject(
            method = "getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoe$useBlockYForBiome(
            BlockPos pPos,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        cir.setReturnValue(noiseBiomeSource.getNoiseBiome(QuartPos.fromBlock(pPos.getX()), QuartPos.fromBlock(pPos.getY()), QuartPos.fromBlock(pPos.getZ())));
    }
}