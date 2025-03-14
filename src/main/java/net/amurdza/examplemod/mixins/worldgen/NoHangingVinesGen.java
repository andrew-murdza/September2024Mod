package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.LeaveVineDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LeaveVineDecorator.class)
public class NoHangingVinesGen {
    @Redirect(method = "lambda$place$1",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/treedecorators/LeaveVineDecorator;addHangingVine(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/properties/BooleanProperty;Lnet/minecraft/world/level/levelgen/feature/treedecorators/TreeDecorator$Context;)V"))
    private void hi(BlockPos blockpos, BooleanProperty pPos, TreeDecorator.Context pSideProperty){

    }
}
