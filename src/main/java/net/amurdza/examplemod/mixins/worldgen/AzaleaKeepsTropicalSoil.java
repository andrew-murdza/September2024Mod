package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.BendingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

/** Keep vanilla azalea geometry without introducing Savanna-only rooted dirt in Tropical. */
@Mixin(TrunkPlacer.class)
public class AzaleaKeepsTropicalSoil {
    @Inject(method = "setDirtAt", at = @At("HEAD"), cancellable = true, require = 1)
    private static void aoemod$keepAzaleaSupport(LevelSimulatedReader reader,
            BiConsumer<BlockPos, BlockState> setter, RandomSource random, BlockPos pos,
            TreeConfiguration configuration, CallbackInfo ci) {
        if (reader instanceof LevelReader level
                && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                && configuration.trunkPlacer instanceof BendingTrunkPlacer
                && configuration.forceDirt
                && configuration.dirtProvider instanceof SimpleStateProvider
                && configuration.dirtProvider.getState(random, pos).is(Blocks.ROOTED_DIRT)
                && level.getBlockState(pos).is(BlockTags.DIRT)) {
            ci.cancel();
        }
    }
}
