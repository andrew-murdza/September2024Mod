package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.DiskFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DiskFeature.class)
public abstract class SedimentDisksPreserveGrassShores {
    @WrapOperation(method = "placeColumn", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 1)
    private boolean aoemod$preserveDryGrass(WorldGenLevel level, BlockPos pos, BlockState state,
                                           int flags, Operation<Boolean> original) {
        // A neighboring chunk's earlier decoration stage can run after this
        // chunk's crops. Disks must not remove their dry grass-bank support.
        if ((GameplayDimensions.is(level, GameplayDimensions.PLAINS) || GameplayDimensions.is(level, GameplayDimensions.SAVANNA))
                && level.getBlockState(pos).is(Blocks.GRASS_BLOCK) && level.getFluidState(pos.above()).isEmpty()) return false;
        return original.call(level, pos, state, flags);
    }
}
