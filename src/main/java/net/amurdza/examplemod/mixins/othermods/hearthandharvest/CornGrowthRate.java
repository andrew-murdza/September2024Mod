package net.amurdza.examplemod.mixins.othermods.hearthandharvest;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.config.BlockConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The source mod's corn bypasses Forge crop hooks; keep its native section-growth mechanics. */
@Mixin(CornStalkBlock.class)
public abstract class CornGrowthRate {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int aoe$applyConfiguredRate(RandomSource random, int bound, Operation<Integer> original,
                                      BlockState state, ServerLevel level, BlockPos pos, RandomSource tickRandom) {
        int roll = original.call(random, bound);
        if (BlockConfig.blockGrowthChance(level, pos, state) < 0) return roll;
        return ForgeHooks.onCropsGrowPre(level, pos, state, roll == 0) ? 0 : 1;
    }

    @Inject(method = "randomTick", at = @At("RETURN"))
    private void aoe$notifyGrowth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (BlockConfig.blockGrowthChance(level, pos, state) >= 0 && level.getBlockState(pos) != state) {
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }
}
