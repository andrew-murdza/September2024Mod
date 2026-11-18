package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.SurfaceWaterBiome;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the lake classifier attached to this world's seed, including independently sampled test worlds. */
@Mixin(RandomState.class)
public final class SurfaceWaterBiomeSampling {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$bindSurfaceWater(NoiseGeneratorSettings settings, HolderGetter<NormalNoise.NoiseParameters> noises,
                                       long seed, CallbackInfo callback) {
        RandomState random = (RandomState) (Object) this;
        SurfaceWaterBiome.bindTogether(random, settings, random.router().vegetation(), random.sampler().humidity());
    }
}
