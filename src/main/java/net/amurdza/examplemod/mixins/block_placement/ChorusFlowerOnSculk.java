package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerOnSculk {
    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    ordinal = 0
            )
    )
    private boolean aoe$randomTickTreatSculkAsEndStone(BlockState state, Block block) {
        if (block == Blocks.END_STONE) {
            return state.is(Blocks.END_STONE)
                    || state.is(Blocks.SCULK)
                    || state.is(Blocks.SCULK_CATALYST);
        }

        return state.is(block);
    }

    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    ordinal = 3
            )
    )
    private boolean aoe$columnRootEndStoneCheck(BlockState state, Block block) {
        return state.is(block)
                || state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_CATALYST);
    }

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

    @Unique
    private boolean aOEMod1_20_1V2$isSoil(BlockState instance, Block block){
        return instance.is(block)||instance.is(Blocks.SCULK)||instance.is(Blocks.SCULK_CATALYST);
    }
}
