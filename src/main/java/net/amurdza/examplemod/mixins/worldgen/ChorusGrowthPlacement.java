package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.amurdza.examplemod.util.GrowthPlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChorusFlowerBlock.class)
public class ChorusGrowthPlacement {
    @WrapMethod(method = "randomTick")
    private void aoemod$naturalChorus(BlockState state, ServerLevel level, BlockPos pos,
                                      RandomSource random, Operation<Void> original) {
        GrowthPlacementContext.run(() -> original.call(state, level, pos, random));
    }
}
