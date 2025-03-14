package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import dev.corgitaco.ohthetreesyoullgrow.world.level.levelgen.feature.configurations.treedecorators.TYGLeavesVineDecorator;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TYGLeavesVineDecorator.class)
public class NoHangingVinesGen {
    @Redirect(method = "lambda$place$6",at= @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private float hi(RandomSource instance){
        return 1;
    }
}
