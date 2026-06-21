package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoublePlantBlock.class)
public abstract class DoublePlantsAreNotMissingHalves {

    @Inject(
            method = "placeAt",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void aoemod$onlyPlaceDoublePlantInClearSpace(
            LevelAccessor level,
            BlockState state,
            BlockPos pos,
            int flags,
            CallbackInfo ci
    ) {
        BlockState lowerState = level.getBlockState(pos);
        BlockState upperState = level.getBlockState(pos.above());

        if (!lowerState.canBeReplaced() || !upperState.canBeReplaced()) {
            ci.cancel();
        }
    }
}