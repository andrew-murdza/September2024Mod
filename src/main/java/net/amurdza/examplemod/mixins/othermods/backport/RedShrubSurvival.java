package net.amurdza.examplemod.mixins.othermods.backport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BushBlock.class)
public abstract class RedShrubSurvival {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void aoe$redShrubRequiresCrimsonNylium(BlockState state, LevelReader level, BlockPos pos,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(new ResourceLocation("minecraft", "red_shrub"))) {
            cir.setReturnValue(level.getBlockState(pos.below()).is(Blocks.CRIMSON_NYLIUM));
        }
    }
}
