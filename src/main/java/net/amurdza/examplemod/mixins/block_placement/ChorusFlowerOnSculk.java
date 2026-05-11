package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerOnSculk {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void aoe$canSurviveOnSculk(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        BlockState below = level.getBlockState(pos.below());

        if (aoe$isExtraSoil(below)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean aoe$isExtraSoil(BlockState state) {
        return state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_CATALYST);
    }
}
