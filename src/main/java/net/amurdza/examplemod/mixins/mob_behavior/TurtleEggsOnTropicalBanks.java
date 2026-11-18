package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tropical shores use moss and dirt, so nearby nests must also hatch on those banks. */
@Mixin(TurtleEggBlock.class)
public abstract class TurtleEggsOnTropicalBanks {
    @Inject(method = "isSand", at = @At("RETURN"), cancellable = true)
    private static void aoemod$allowTropicalBanks(BlockGetter level, BlockPos pos,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof LevelReader reader && GameplayDimensions.is(reader, GameplayDimensions.TROPICAL)
                && (level.getBlockState(pos).is(Blocks.MOSS_BLOCK) || level.getBlockState(pos).is(BlockTags.DIRT))) {
            cir.setReturnValue(true);
        }
    }
}
