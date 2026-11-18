package net.amurdza.examplemod.mixins.othermods.hearthandharvest;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep native section/light checks, but root all corn in cultivated farmland. */
@Mixin(CornStalkBlock.class)
public abstract class CornRequiresFarmland {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void aoemod$requireCultivatedSoil(BlockState state, LevelReader level, BlockPos pos,
                                             CallbackInfoReturnable<Boolean> result) {
        if (!net.amurdza.examplemod.util.CornCultivation.hasFarmland(level, pos)) result.setReturnValue(false);
    }
}
