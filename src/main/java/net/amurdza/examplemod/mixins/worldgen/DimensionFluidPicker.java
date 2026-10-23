package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** Surface lakes have an explicit floor; dry mining galleries never inherit the global sea or deep lava. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class DimensionFluidPicker {
    @Shadow @Final @Mutable private Supplier<Aquifer.FluidPicker> globalFluidPicker;
    @Unique private boolean aoemod$netherRamps;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$dimensionFluids(BiomeSource biomes, Holder<NoiseGeneratorSettings> settings, CallbackInfo ci) {
        if (!(biomes instanceof net.amurdza.examplemod.worldgen.DimensionBiomeSource)) return;
        String name = settings.unwrapKey().map(key -> key.location().getPath()).orElse("");
        int cutoff = switch (name) {
            case "snowy_dimension" -> 16;
            case "plains_dimension" -> 44;
            case "desert_dimension" -> 54;
            default -> Integer.MIN_VALUE;
        };
        Aquifer.FluidStatus surface = new Aquifer.FluidStatus(settings.value().seaLevel(), settings.value().defaultFluid());
        Aquifer.FluidStatus dry = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        if (name.equals("nether_dimension")) {
            aoemod$netherRamps = true;
            // Each upper gallery has local fishing pools; the bottom lava sea stays at Y12.
            Aquifer.FluidStatus[] lava = new Aquifer.FluidStatus[4];
            for (int band = 0; band < 4; band++) {
                lava[band] = new Aquifer.FluidStatus(band == 0 ? 12 : band * 32 + 7, Blocks.LAVA.defaultBlockState());
            }
            this.globalFluidPicker = () -> (x, y, z) -> lava[Math.max(0, Math.min(3, Math.floorDiv(y, 32)))];
            return;
        }
        this.globalFluidPicker = () -> (x, y, z) -> y < cutoff ? dry : surface;
    }

    @WrapOperation(method = {"createNoiseChunk", "iterateNoiseColumn"}, at = @At(value = "INVOKE",
            target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;"))
    private Object aoemod$keepSeededRampsDry(Supplier<?> supplier, Operation<Object> original,
                                            @Local(argsOnly = true) RandomState randomState) {
        Object value = original.call(supplier);
        if (!aoemod$netherRamps) return value;
        Aquifer.FluidPicker fluids = (Aquifer.FluidPicker) value;
        Aquifer.FluidStatus dry = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        return (Aquifer.FluidPicker) (x, y, z) -> {
            if (y >= 32) {
                // Ridges exposes exactly the seeded detail field used by DimensionTerrain.
                double detail = randomState.router().ridges().compute(new DensityFunction.SinglePointContext(x, 0, z));
                if (DimensionTerrain.netherRampDistance(z, detail) <= 12) return dry;
            }
            return fluids.computeFluid(x, y, z);
        };
    }
}
