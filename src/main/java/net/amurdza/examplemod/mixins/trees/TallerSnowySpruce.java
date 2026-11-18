package net.amurdza.examplemod.mixins.trees;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.amurdza.examplemod.worldgen.SnowySpruceHeight;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shared by natural trees and saplings; other habitats retain their original configurations. */
@Mixin(TreeFeature.class)
public abstract class TallerSnowySpruce {
    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/trunkplacers/TrunkPlacer;getTreeHeight(Lnet/minecraft/util/RandomSource;)I"), require = 1)
    private int aoemod$extendSpruceTrunk(TrunkPlacer placer, RandomSource random, Operation<Integer> original,
                                       @Local(argsOnly = true) WorldGenLevel level,
                                       @Local(argsOnly = true) BlockPos origin,
                                       @Local(argsOnly = true) TreeConfiguration configuration) {
        return SnowySpruceHeight.sampleHeight(level, origin, configuration, random,
                () -> original.call(placer, random)) + net.amurdza.examplemod.worldgen.PlainsTreeHeight.extension(level, origin, configuration);
    }

    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer;foliageHeight(Lnet/minecraft/util/RandomSource;ILnet/minecraft/world/level/levelgen/feature/configurations/TreeConfiguration;)I"), require = 1)
    private int aoemod$preserveSpruceCrownHeight(FoliagePlacer placer, RandomSource random, int treeHeight,
                                               TreeConfiguration configuration, Operation<Integer> original,
                                               @Local(argsOnly = true) WorldGenLevel level,
                                               @Local(argsOnly = true) BlockPos origin) {
        return original.call(placer, random,
                treeHeight - SnowySpruceHeight.extension(level, origin, configuration)
                        - net.amurdza.examplemod.worldgen.PlainsTreeHeight.extension(level, origin, configuration), configuration);
    }

    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer;foliageRadius(Lnet/minecraft/util/RandomSource;I)I"), require = 1)
    private int aoemod$preserveSpruceCrownRadius(FoliagePlacer placer, RandomSource random, int bareTrunkHeight,
                                               Operation<Integer> original,
                                               @Local(argsOnly = true) WorldGenLevel level,
                                               @Local(argsOnly = true) BlockPos origin,
                                               @Local(argsOnly = true) TreeConfiguration configuration) {
        // The increased trunk moves the foliage attachment up by four blocks.
        // Sample crown dimensions from the original height so its shape stays intact.
        return original.call(placer, random,
                bareTrunkHeight - SnowySpruceHeight.extension(level, origin, configuration)
                        - net.amurdza.examplemod.worldgen.PlainsTreeHeight.extension(level, origin, configuration));
    }
}
