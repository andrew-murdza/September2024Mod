package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeManager.class)
public abstract class BiomeManagerPreciseYMixin {

    @Shadow @Final private BiomeManager.NoiseBiomeSource noiseBiomeSource;
    @Shadow @Final private long biomeZoomSeed;

    @Inject(
            method = "getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoe$getBiomeWithPreciseYButFuzzyXZ(
            BlockPos pos,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        /*
         * Vanilla subtracts 2 from X/Y/Z before doing biome Voronoi.
         * We keep that for X/Z so horizontal biome edges stay fuzzy.
         *
         * For Y, we do NOT subtract 2 and we do NOT compare y + 1.
         * This makes vertical biome layers use the exact quart Y value.
         */
        int blockX = pos.getX() - 2;
        int blockZ = pos.getZ() - 2;

        int quartX = blockX >> 2;
        int quartY = QuartPos.fromBlock(pos.getY());
        int quartZ = blockZ >> 2;

        double localX = (double)(blockX & 3) / 4.0D;
        double localZ = (double)(blockZ & 3) / 4.0D;

        int bestCorner = 0;
        double bestDistance = Double.POSITIVE_INFINITY;

        // Only 4 corners: X/Z fuzziness, no Y fuzziness.
        for (int corner = 0; corner < 4; ++corner) {
            boolean useLowerX = (corner & 2) == 0;
            boolean useLowerZ = (corner & 1) == 0;

            int sampleQuartX = useLowerX ? quartX : quartX + 1;
            int sampleQuartZ = useLowerZ ? quartZ : quartZ + 1;

            double xNoise = useLowerX ? localX : localX - 1.0D;
            double zNoise = useLowerZ ? localZ : localZ - 1.0D;

            double distance = aoe$getFiddledDistanceXZ(
                    this.biomeZoomSeed,
                    sampleQuartX,
                    quartY,
                    sampleQuartZ,
                    xNoise,
                    zNoise
            );

            if (distance < bestDistance) {
                bestCorner = corner;
                bestDistance = distance;
            }
        }

        int finalQuartX = (bestCorner & 2) == 0 ? quartX : quartX + 1;
        int finalQuartZ = (bestCorner & 1) == 0 ? quartZ : quartZ + 1;

        cir.setReturnValue(this.noiseBiomeSource.getNoiseBiome(finalQuartX, quartY, finalQuartZ));
    }

    private static double aoe$getFiddledDistanceXZ(
            long seed,
            int quartX,
            int quartY,
            int quartZ,
            double xNoise,
            double zNoise
    ) {
        long mixedSeed = LinearCongruentialGenerator.next(seed, quartX);
        mixedSeed = LinearCongruentialGenerator.next(mixedSeed, quartY);
        mixedSeed = LinearCongruentialGenerator.next(mixedSeed, quartZ);
        mixedSeed = LinearCongruentialGenerator.next(mixedSeed, quartX);
        mixedSeed = LinearCongruentialGenerator.next(mixedSeed, quartY);
        mixedSeed = LinearCongruentialGenerator.next(mixedSeed, quartZ);

        double xFiddle = aoe$getFiddle(mixedSeed);
        mixedSeed = LinearCongruentialGenerator.next(mixedSeed, seed);

        // Skip vanilla Y fiddle entirely.
        mixedSeed = LinearCongruentialGenerator.next(mixedSeed, seed);

        double zFiddle = aoe$getFiddle(mixedSeed);

        return Mth.square(xNoise + xFiddle) + Mth.square(zNoise + zFiddle);
    }

    private static double aoe$getFiddle(long seed) {
        double value = (double)Math.floorMod(seed >> 24, 1024) / 1024.0D;
        return (value - 0.5D) * 0.9D;
    }
}