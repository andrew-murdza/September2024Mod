package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.worldgen.RedMushroomCapSpace;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.TreeGrowthCollisionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Caps are tagged as leaves for harvesting, but ordinary foliage cannot consume them. */
@Mixin(FoliagePlacer.class)
public abstract class FoliagePreservesGiantMushrooms {
    @Inject(method = "tryPlaceLeaf", at = @At("HEAD"), cancellable = true, require = 1)
    private static void aoemod$keepMushroomBlocks(LevelSimulatedReader level, FoliagePlacer.FoliageSetter setter,
            RandomSource random, TreeConfiguration config, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof GrowthPlacementBuffer buffer && level.isStateAtPosition(pos,
                state -> state.getBlock() instanceof HugeMushroomBlock && TreeGrowthCollisionRules.isWood(state))) {
            // Merely skipping this occupied cell lets the remaining crown grow
            // around a stem from an earlier neighbouring chunk. Reject this
            // whole tree attempt when its intended foliage crosses that stem.
            buffer.reject();
        }
        if (level.isStateAtPosition(pos, state -> state.getBlock() instanceof HugeMushroomBlock)
                || RedMushroomCapSpace.isInsideRedCap(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
