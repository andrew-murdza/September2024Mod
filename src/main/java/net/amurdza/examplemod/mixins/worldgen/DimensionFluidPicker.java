package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
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

/** Dimension-specific fluid rules keep lava out of the custom overworld dimensions. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class DimensionFluidPicker {
    @Shadow @Final @Mutable private Supplier<Aquifer.FluidPicker> globalFluidPicker;
    @Unique private boolean aoemod$netherRamps;
    @Unique private boolean aoemod$snowy;
    @Unique private boolean aoemod$desert;
    @Unique private Aquifer.FluidStatus aoemod$snowySurface;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$dimensionFluids(BiomeSource biomes, Holder<NoiseGeneratorSettings> settings, CallbackInfo ci) {
        if (!(biomes instanceof net.amurdza.examplemod.worldgen.DimensionBiomeSource)) return;
        String name = settings.unwrapKey().map(key -> key.location().getPath()).orElse("");
        int cutoff = switch (name) {
            // Snowy uses vanilla aquifers throughout its full build height.
            case "snowy_dimension" -> Integer.MIN_VALUE;
            // Restored vanilla oceans can have floors below Y44. Their water
            // remains connected to those floors; the deeper caves stay dry.
            case "plains_dimension" -> 0;
            case "desert_dimension" -> 48;
            default -> Integer.MIN_VALUE;
        };
        Aquifer.FluidStatus surface = new Aquifer.FluidStatus(settings.value().seaLevel(), settings.value().defaultFluid());
        Aquifer.FluidStatus dry = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        if (name.equals("snowy_dimension")) {
            aoemod$snowy = true;
            aoemod$snowySurface = surface;
            this.globalFluidPicker = () -> (x, y, z) -> dry;
            return;
        }
        if (name.equals("nether_dimension")) {
            aoemod$netherRamps = true;
            // Only the basalt-delta layer has lava lakes. Every higher gallery stays dry.
            Aquifer.FluidStatus lava = new Aquifer.FluidStatus(DimensionTerrain.BASALT_LAVA_LEVEL, Blocks.LAVA.defaultBlockState());
            this.globalFluidPicker = () -> (x, y, z) -> y < 32 ? lava : dry;
            return;
        }
        aoemod$desert = name.equals("desert_dimension");
        this.globalFluidPicker = () -> (x, y, z) -> y < cutoff ? dry : surface;
    }

    @WrapOperation(method = {"createNoiseChunk", "iterateNoiseColumn"}, at = @At(value = "INVOKE",
            target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;"))
    private Object aoemod$keepSeededRampsDry(Supplier<?> supplier, Operation<Object> original,
                                            @Local(argsOnly = true) RandomState randomState) {
        Object value = original.call(supplier);
        if (!aoemod$netherRamps && !aoemod$snowy && !aoemod$desert) return value;
        Aquifer.FluidPicker fluids = (Aquifer.FluidPicker) value;
        Aquifer.FluidStatus dry = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        return (Aquifer.FluidPicker) (x, y, z) -> {
            if (aoemod$desert) {
                // Surface water belongs only to actual lake columns. A dry
                // desert mouth below sea level must not become a new pond.
                double habitat = randomState.router().continents().compute(new DensityFunction.SinglePointContext(x, 0, z));
                return y < DimensionTerrain.DESERT_LAKE_CARVE_CEILING || habitat != 2 ? dry : fluids.computeFluid(x, y, z);
            }
            if (aoemod$snowy) {
                int floor = net.amurdza.examplemod.worldgen.SnowyOceanFloor.floor(randomState, x, z);
                return y > floor && net.amurdza.examplemod.worldgen.SnowyOceanFloor.submerged(floor) ? aoemod$snowySurface : dry;
            }
            if (y < 32) {
                // Ridges exposes exactly the seeded detail field used by DimensionTerrain.
                double detail = randomState.router().ridges().compute(new DensityFunction.SinglePointContext(x, 0, z));
                if (DimensionTerrain.netherRampDistance(z, detail) <= 12) return dry;
            }
            return fluids.computeFluid(x, y, z);
        };
    }
}
