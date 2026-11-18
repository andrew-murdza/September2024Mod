package net.amurdza.examplemod.mixins.othermods.mynethersdelight;

import com.soytutta.mynethersdelight.common.block.PowderyCaneBlock;
import com.soytutta.mynethersdelight.common.block.PowderyCannonBlock;
import com.soytutta.mynethersdelight.common.block.PowderyFlowerBlock;
import net.amurdza.examplemod.worldgen.PowderCaneShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PowderyCaneBlock.class, PowderyCannonBlock.class, PowderyFlowerBlock.class})
public abstract class PowderCaneShapeRules {
    @Inject(method = {"randomTick", "tick"}, at = @At("RETURN"))
    private void aoemod$shapeAfterTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        PowderCaneShape.normalize(level, pos);
    }

    @Inject(method = "performBonemeal", at = @At("RETURN"))
    private void aoemod$shapeAfterGrowth(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        PowderCaneShape.normalize(level, pos);
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void aoemod$shapeAfterNeighbor(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(PowderCaneShape.shape(level, pos, cir.getReturnValue()));
    }
}
