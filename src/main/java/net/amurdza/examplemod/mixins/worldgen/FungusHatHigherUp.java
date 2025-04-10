package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HugeFungusFeature.class)
public class FungusHatHigherUp {
    @ModifyConstant(method = "placeHat",constant = @Constant(intValue = 5))
    private int hi(int constant){
        return 10;
    }
    @ModifyConstant(method = "place",constant = @Constant(intValue = 4,ordinal = 0))
    private int hi1(int constant){
        return 10;
    }
    @Redirect(method = "place",at= @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int hi(RandomSource instance, int i){
        return 1;
    }
}
