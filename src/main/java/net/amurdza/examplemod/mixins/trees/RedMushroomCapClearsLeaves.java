package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.worldgen.RedMushroomCapSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.HugeRedMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HugeRedMushroomFeature.class)
public abstract class RedMushroomCapClearsLeaves {
    @Inject(method = "makeCap", at = @At("TAIL"), require = 1)
    private void aoemod$clearHollowCap(LevelAccessor level, RandomSource random, BlockPos origin, int height,
            BlockPos.MutableBlockPos cursor, HugeMushroomFeatureConfiguration config, CallbackInfo ci) {
        RedMushroomCapSpace.clearInteriorLeaves(level, origin, height, config.foliageRadius);
    }
}
