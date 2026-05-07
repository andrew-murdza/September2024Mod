package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional: narrows the vanilla-style global lava aquifer to only -64..-54.
 * This does not create a floor under every lava pocket; that requires density changes
 * or post-processing. Keep this mixin only if you still want lava aquifers in that band.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "createFluidPicker", at = @At("HEAD"), cancellable = true)
    private static void aoemod$aoedimFluidPicker(NoiseGeneratorSettings settings, CallbackInfoReturnable<Aquifer.FluidPicker> cir) {
        Aquifer.FluidStatus lavaBand = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus defaultFluid = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        cir.setReturnValue((x, y, z) -> {
            if (y >= -64 && y <= -54) return lavaBand;
            return defaultFluid;
        });
    }
}
