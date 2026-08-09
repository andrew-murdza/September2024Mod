package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.MushroomCaveRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(OreFeature.class)
public class MushroomCeilingOreProtection {
    private static final int CEILING_DEPTH = 6;

    @Inject(method = "canPlaceOre", at = @At("RETURN"), cancellable = true)
    private static void aoemod$preserveMushroomCeilingStone(
            BlockState state,
            Function<BlockPos, BlockState> adjacentStateAccessor,
            RandomSource random,
            OreConfiguration config,
            OreConfiguration.TargetBlockState targetState,
            BlockPos.MutableBlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || !state.is(Blocks.STONE)) {
            return;
        }

        BlockPos stonePos = pos.immutable();
        for (int depthBelowSurface = 2; depthBelowSurface < CEILING_DEPTH; depthBelowSurface++) {
            if (isProtectedCeilingLayer(adjacentStateAccessor, stonePos, depthBelowSurface)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    private static boolean isProtectedCeilingLayer(
            Function<BlockPos, BlockState> stateAccessor,
            BlockPos stonePos,
            int depthBelowSurface
    ) {
        BlockPos surface = stonePos.above(depthBelowSurface);
        if (!MushroomCaveRegion.containsUpperCeilingColumn(surface)) {
            return false;
        }

        return stateAccessor.apply(surface).is(Blocks.GRASS_BLOCK)
                && stateAccessor.apply(surface.below()).is(Blocks.DIRT)
                && !stateAccessor.apply(surface.below(CEILING_DEPTH)).isSolid();
    }
}
