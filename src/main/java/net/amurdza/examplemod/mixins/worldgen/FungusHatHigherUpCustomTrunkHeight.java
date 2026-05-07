package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HugeFungusFeature.class)
public class FungusHatHigherUpCustomTrunkHeight {

//    // Change minimum height from 4 to 7.
//    // Vanilla max stays 13.
//    @ModifyConstant(
//            method = "place",
//            constant = @Constant(intValue = 4, ordinal = 0)
//    )
//    private int aoemod$minFungusHeight(int constant) {
//        return 8;
//    }
//
//    // Disable double-height chance.
//    @Redirect(
//            method = "place",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"
//            )
//    )
//    private int aoemod$disableDoubleHeight(RandomSource random, int bound) {
//        return 1;
//    }
//
//    // Make the cap begin exactly 3 blocks above the ground.
//    @ModifyExpressionValue(
//            method = "placeHat",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Ljava/lang/Math;min(II)I"
//            )
//    )
//    private int aoemod$capStartsThreeBlocksUp(
//            int original,
//            WorldGenLevel level,
//            RandomSource random,
//            HugeFungusConfiguration config,
//            BlockPos pos,
//            int height,
//            boolean huge
//    ) {
//        return height - 3;
//    }
}
