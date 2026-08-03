package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
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
        boolean isSoulTree = context.config().foliageProvider
                .getState(context.random(), context.origin())
                .is(ModBlocks.PALE_OAK_LEAVES.get());

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockState nearbyState = level.getBlockState(origin.offset(dx, dy, dz));
                    if (nearbyState.is(Blocks.SWEET_BERRY_BUSH)
                            || nearbyState.is(ModBlocks.BLUE_BERRY_BUSH.get())) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        if (isSoulTree) {
            for (int dx = -2; dx <= 3; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 3; dz++) {
                        BlockState nearbyState = level.getBlockState(origin.offset(dx, dy, dz));
                        if (nearbyState.is(Blocks.NETHER_WART)
                                || nearbyState.is(ModBlocks.SOUL_BERRY_BUSH.get())
                                || nearbyState.is(ModBlocks.CLOSED_EYEBLOSSOM.get())
                                || nearbyState.is(ModBlocks.OPEN_EYEBLOSSOM.get())) {
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
            }
        }

        if (!level.getFluidState(origin).isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        if(isSoulTree){
            for(int i=0; i<2; i++){
                for(int j=0; j<2; j++){
                    BlockPos groundPos = origin.below().relative(Direction.EAST,i).relative(Direction.SOUTH,j);
                    BlockState groundState = level.getBlockState(groundPos);
                    if(!groundState.is(ModTags.Blocks.soulSediments)){
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
        else {
            BlockPos groundPos = origin.below();
            BlockState groundState = level.getBlockState(groundPos);
            if (!groundState.is(BlockTags.DIRT)&&!isSoulTree) {
                cir.setReturnValue(false);
            }
        }
    }
}
