package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.river.MountainRiverHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MountainRiverFluid {

    @Redirect(
            method = "createNoiseChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseChunk;forChunk(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/levelgen/DensityFunctions$BeardifierOrMarker;Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;Lnet/minecraft/world/level/levelgen/blending/Blender;)Lnet/minecraft/world/level/levelgen/NoiseChunk;"
            )
    )
    private NoiseChunk aoemod$createNoiseChunkWithMountainRiverFluidPicker(
            ChunkAccess chunk,
            RandomState randomState,
            DensityFunctions.BeardifierOrMarker beardifier,
            NoiseGeneratorSettings settings,
            Aquifer.FluidPicker vanillaFluidPicker,
            Blender blender
    ) {
        Aquifer.FluidPicker mountainRiverFluidPicker = (x, y, z) -> {
            double continents = randomState.router()
                    .continents()
                    .compute(new DensityFunction.SinglePointContext(x, 0, z));

            if (MountainRiverHelper.isMountainRiverContinentsValue(continents)) {
                return new Aquifer.FluidStatus(
                        MountainRiverHelper.getMountainRiverWaterLevel(continents),
                        Blocks.WATER.defaultBlockState()
                );
            }

            return vanillaFluidPicker.computeFluid(x, y, z);
        };

        return NoiseChunk.forChunk(
                chunk,
                randomState,
                beardifier,
                settings,
                mountainRiverFluidPicker,
                blender
        );
    }
}