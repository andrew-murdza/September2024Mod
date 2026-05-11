package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import dev.corgitaco.ohthetreesyoullgrow.world.level.levelgen.feature.configurations.treedecorators.TYGLeavesVineDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TYGLeavesVineDecorator.class,remap = false)
public class NoHangingVinesGen {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void aoe$cancelVines(TreeDecorator.Context context, CallbackInfo ci) {
        ci.cancel();
    }
}
