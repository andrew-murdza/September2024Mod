package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.rootplacers.MangroveRootPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MangroveRootPlacer.class)
public class MangroveRootsOverFlowers {
    @Inject(method = "canPlaceRoot", at = @At("HEAD"), cancellable = true)
    private void aoemod$rootsReplaceFlowers(LevelSimulatedReader level, BlockPos pos,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof WorldGenLevel world && GameplayDimensions.is(world, GameplayDimensions.TROPICAL)
                && level.isStateAtPosition(pos, net.amurdza.examplemod.worldgen.TropicalPlantIntegrity::flowerRootMayReplace)) cir.setReturnValue(true);
    }
}
