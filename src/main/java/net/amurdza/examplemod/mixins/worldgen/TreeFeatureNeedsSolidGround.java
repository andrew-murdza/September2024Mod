package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

import java.util.function.BiConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeFeature.class)
public class TreeFeatureNeedsSolidGround {

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/TreeFeature;doPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;Lnet/minecraft/world/level/levelgen/feature/configurations/TreeConfiguration;)Z"), require = 1)
    private boolean aoemod$requireTrunkBeforeDecorating(TreeFeature tree, WorldGenLevel level,
            RandomSource random, BlockPos origin, BiConsumer<BlockPos, BlockState> roots,
            BiConsumer<BlockPos, BlockState> logs, FoliagePlacer.FoliageSetter leaves,
            TreeConfiguration configuration, Operation<Boolean> original) {
        if (!GameplayDimensions.isCustom(level)) {
            return original.call(tree, level, random, origin, roots, logs, leaves, configuration);
        }
        boolean[] placedLog = {false};
        BiConsumer<BlockPos, BlockState> checkedLogs = (pos, state) -> {
            placedLog[0] = true;
            logs.accept(pos, state);
        };
        boolean placed = original.call(tree, level, random, origin, roots, checkedLogs, leaves, configuration);
        // Clearance accepts existing logs, but trunk placement does not replace them. Such an
        // overlapping tree can produce only foliage; cocoa and beehive decorators require a log.
        // Returning false here lets the enclosing atomic buffer discard the entire attempt.
        return placed && placedLog[0];
    }

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
