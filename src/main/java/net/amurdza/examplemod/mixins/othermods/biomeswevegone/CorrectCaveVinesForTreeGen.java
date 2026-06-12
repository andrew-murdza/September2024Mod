package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.treedecorators.GlowBerryDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GlowBerryDecorator.class, remap = false)
public class CorrectCaveVinesForTreeGen {

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/treedecorators/TreeDecorator$Context;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            ),
            remap = true
    )
    private void aoemod$correctCaveVinesForTreeGen(
            TreeDecorator.Context context,
            BlockPos pos,
            BlockState state,
            Operation<Void> original
    ) {
        if (aoemod$hasMossWithinFourBelow(context, pos)) {
            return;
        }

        if (state.is(Blocks.CAVE_VINES)) {
            state = state.setValue(CaveVinesBlock.AGE, 25);
        }

        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean inWater = context.level().isFluidAtPosition(pos,state1->state1.is(FluidTags.WATER));

            state = state.setValue(BlockStateProperties.WATERLOGGED, inWater);
        }

        original.call(context, pos, state);
    }

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

    @Unique
    private boolean aoemod$hasMossWithinFourBelow(TreeDecorator.Context context, BlockPos pos) {
        for (int dy = 1; dy <= 4; dy++) {
            if (context.level().isStateAtPosition(pos.below(dy), state -> state.is(Blocks.MOSS_BLOCK)
                    ||!state.getFluidState().isEmpty())) {
                return true;
            }
        }

        return false;
    }
}