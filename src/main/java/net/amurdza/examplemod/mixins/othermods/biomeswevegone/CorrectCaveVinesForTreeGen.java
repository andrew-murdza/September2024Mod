package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.material.FluidState;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.treedecorators.GlowBerryDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GlowBerryDecorator.class, remap = false)
public class CorrectCaveVinesForTreeGen {
    @Redirect(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;randomBetweenInclusive(Lnet/minecraft/util/RandomSource;II)I"
            ),
            remap = true
    )
    private int aoemod$bottomCaveVineAge25(net.minecraft.util.RandomSource random, int min, int max) {
        return 25;
    }

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/treedecorators/TreeDecorator$Context;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            ),
            remap = true
    )
    private void aoemod$skipCaveVinesTooCloseToMoss(TreeDecorator.Context context, BlockPos pos, BlockState state, Operation<Void> original) {
        if ((state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT))) {
            if(!september2024Mod$hasMossWithinFourBelow(context, pos)){
                state=state.setValue(BlockStateProperties.WATERLOGGED,!context.level().isFluidAtPosition(pos, FluidState::isEmpty))
                        .setValue(CaveVinesBlock.BERRIES,true);
                if(state.is(Blocks.CAVE_VINES)){
                    state=state.setValue(CaveVinesBlock.AGE,25);
                }
                context.setBlock(pos,state);
            }
            return;
        }
        original.call(context,pos,state);
    }

    @Unique
    private boolean september2024Mod$hasMossWithinFourBelow(TreeDecorator.Context context, BlockPos pos) {
        for (int dy = 1; dy <= 4; dy++) {
            if (context.level().isStateAtPosition(pos.below(dy), state -> state.is(Blocks.MOSS_BLOCK)
                    ||!state.getFluidState().isEmpty())) {
                return true;
            }
        }

        return false;
    }
}