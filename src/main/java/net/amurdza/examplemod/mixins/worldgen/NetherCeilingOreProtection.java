package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.surface_rule.BandlandsSurfaceRuleSource;
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
public class NetherCeilingOreProtection {
    private static final int SAND_DEPTH = 2;
    private static final int SANDSTONE_DEPTH = 2;
    private static final int STONE_DEPTH = 4;
    private static final int CEILING_DEPTH = SAND_DEPTH + SANDSTONE_DEPTH + STONE_DEPTH;

    @Inject(method = "canPlaceOre", at = @At("RETURN"), cancellable = true)
    private static void aoemod$preserveNetherCeilingStone(
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
        int firstStoneDepth = SAND_DEPTH + SANDSTONE_DEPTH;
        for (int depthBelowSurface = firstStoneDepth;
             depthBelowSurface < CEILING_DEPTH;
             depthBelowSurface++) {
            if (isProtectedCeilingLayer(
                    adjacentStateAccessor,
                    stonePos,
                    depthBelowSurface
            )) {
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
        if (!BandlandsSurfaceRuleSource.usesSandySurfaceAt(
                surface.getX(),
                surface.getZ()
        )) {
            return false;
        }

        return stateAccessor.apply(surface).is(Blocks.SAND)
                && stateAccessor.apply(surface.below()).is(Blocks.SAND)
                && stateAccessor.apply(surface.below(2)).is(Blocks.SANDSTONE)
                && stateAccessor.apply(surface.below(3)).is(Blocks.SANDSTONE)
                && !stateAccessor.apply(surface.below(CEILING_DEPTH)).isSolid();
    }
}
