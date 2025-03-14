package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.treedecorators.TrunkVineDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TrunkVineDecorator.class)
public class NoHangingVinesGen1 {
//    @Redirect(method = "lambda$place$1",at= @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
//    private static int hi(RandomSource instance, int i){
//        return 1;
//    }
}
