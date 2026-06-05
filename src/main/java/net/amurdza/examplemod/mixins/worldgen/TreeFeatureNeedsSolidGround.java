package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeFeature.class)
public class TreeFeatureNeedsSolidGround {

    @Inject(
            method = "place",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoemod$failIfNotAboveSolidBlock(
            FeaturePlaceContext<TreeConfiguration> context,
            CallbackInfoReturnable<Boolean> cir
    ) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos groundPos = origin.below();

        BlockState groundState = level.getBlockState(groundPos);

        if (!groundState.is(BlockTags.DIRT)) {
            cir.setReturnValue(false);
        }
    }
}