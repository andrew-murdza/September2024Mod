package net.amurdza.examplemod.mixins.othermods.netherific;

import net.mcreator.nourishednether.block.SoulWeedsBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SoulWeedsBlock.class)
public class SoulWeedsOnMoss {
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/mcreator/nourishednether/block/SoulWeedsBlock;mayPlaceOn(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hi(SoulWeedsBlock instance, BlockState state, BlockGetter level, BlockPos pos){
        return instance.mayPlaceOn(state,level,pos)||state.is(Blocks.MOSS_BLOCK);
    }
}
