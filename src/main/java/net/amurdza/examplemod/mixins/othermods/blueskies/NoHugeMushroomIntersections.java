package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.world.general_features.BaseLargeMushroomFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BaseLargeMushroomFeature.class,remap = false)
public class NoHugeMushroomIntersections {

    @Inject(method = "isReplaceableByMushroom", at = @At("HEAD"), cancellable = true)
    private void aoe$dontReplaceLogs(WorldGenLevel world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state=world.getBlockState(pos);
        cir.setReturnValue(state.canBeReplaced() || state.is(BlockTags.REPLACEABLE_BY_TREES));
    }
}
