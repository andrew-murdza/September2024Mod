package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.worldgen.TropicalPlantIntegrity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class TropicalPlantReplacement {
    @Inject(method = "markAndNotifyBlock", at = @At("HEAD"), remap = false)
    private void aoemod$clearDependentPlants(BlockPos pos, LevelChunk chunk, BlockState oldState,
                                            BlockState newState, int flags, int recursion, CallbackInfo ci) {
        Level level = (Level) (Object) this;
        if (!level.isClientSide && level.getBlockState(pos) == newState) {
            TropicalPlantIntegrity.afterReplacement(level, pos, oldState, newState);
        }
    }
}
