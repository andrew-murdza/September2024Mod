package net.amurdza.examplemod.mixins.growth_rate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(TurtleEggBlock.class)
public class TurtleEggGrowthRate {
    @Shadow
    private boolean shouldUpdateHatchLevel(Level level) {
        throw new AssertionError();
    }

    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/TurtleEggBlock;shouldUpdateHatchLevel(Lnet/minecraft/world/level/Level;)Z"))
    private boolean shouldHatchProgress(TurtleEggBlock instance, Level pLevel, BlockState pState, ServerLevel level, BlockPos pPos, RandomSource pRandom){
        return ForgeHooks.onCropsGrowPre(level, pPos, pState, this.shouldUpdateHatchLevel(pLevel));
    }
}
