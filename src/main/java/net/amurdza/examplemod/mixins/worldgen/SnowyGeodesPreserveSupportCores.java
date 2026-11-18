package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyCaveColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.GeodeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/** Geode chambers and cracks may not sever an existing Snowy pillar core. */
@Mixin(GeodeFeature.class)
public abstract class SnowyGeodesPreserveSupportCores {
    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/GeodeFeature;safeSetBlock(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/function/Predicate;)V"), require = 1)
    private void aoemod$keepSolidCore(GeodeFeature feature, WorldGenLevel level, BlockPos pos,
                                    BlockState state, Predicate<BlockState> replaceable, Operation<Void> original) {
        if (state.isAir() && GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                && level.getBlockState(pos).isSolid()
                && SnowyCaveColumns.supports(level.getLevel().getChunkSource().randomState())
                        .clearance(pos.getX(), pos.getZ()) <= 0) return;
        original.call(feature, level, pos, state, replaceable);
    }
}
