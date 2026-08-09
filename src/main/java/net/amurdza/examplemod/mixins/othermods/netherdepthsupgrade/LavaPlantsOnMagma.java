package net.amurdza.examplemod.mixins.othermods.netherdepthsupgrade;

import com.scouter.netherdepthsupgrade.blocks.CrimsonSeagrassBlock;
import com.scouter.netherdepthsupgrade.blocks.TallCrimsonSeagrassBlock;
import com.scouter.netherdepthsupgrade.blocks.TallWarpedSeagrassBlock;
import com.scouter.netherdepthsupgrade.blocks.WarpedSeagrassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = {
                CrimsonSeagrassBlock.class,
                WarpedSeagrassBlock.class,
                TallCrimsonSeagrassBlock.class,
                TallWarpedSeagrassBlock.class
        },
        remap = false
)
public class LavaPlantsOnMagma {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void aoemod$allowMagma(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (state.is(Blocks.MAGMA_BLOCK) && state.isFaceSturdy(level, pos, Direction.UP)) {
            cir.setReturnValue(true);
        }
    }
}
