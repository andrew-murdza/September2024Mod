package net.amurdza.examplemod.mixins.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.HugeBrownMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHugeMushroomFeature.class)
public abstract class CorrectHugeMushroomPlacementChecks {
    @Shadow
    protected abstract int getTreeRadiusForHeight(
            int pY,
            int pMaxHeight,
            int pFoliageRadius,
            int pCurrentY
    );

    @Redirect(
            method = "isValidPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/AbstractHugeMushroomFeature;getTreeRadiusForHeight(IIII)I"
            )
    )
    private int aoemod$useActualMushroomHeightForRadiusCheck(
            AbstractHugeMushroomFeature instance,
            int pY,
            int wrongMaxHeight,
            int foliageRadius,
            int currentY,
            LevelAccessor level,
            BlockPos pos,
            int maxHeight,
            BlockPos.MutableBlockPos mutablePos,
            HugeMushroomFeatureConfiguration config
    ) {
        return this.getTreeRadiusForHeight(pY, maxHeight, foliageRadius + 1, currentY);
    }

    @Redirect(
            method = "isValidPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z",
                    ordinal = 1
            )
    )
    private boolean aoemod$useActualMushroomHeightForRadiusCheck2(
            BlockState instance, TagKey<Block> tagKey
    ) {
        return false;
    }

    @Inject(method = "isValidPosition", at = @At("HEAD"), cancellable = true)
    private void aoemod$requireBrownMushroomCanopyClearance(
            LevelAccessor level,
            BlockPos pos,
            int maxHeight,
            BlockPos.MutableBlockPos mutablePos,
            HugeMushroomFeatureConfiguration config,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object) this instanceof HugeBrownMushroomFeature)) {
            return;
        }

        int originY = pos.getY();

        if (originY < level.getMinBuildHeight() + 1 || originY + maxHeight + 2 >= level.getMaxBuildHeight()) {
            cir.setReturnValue(false);
            return;
        }

        BlockState ground = level.getBlockState(pos.below());
        if (!isValidHugeMushroomGround(ground)) {
            cir.setReturnValue(false);
            return;
        }

        for (int currentY = 0; currentY <= maxHeight + 2; currentY++) {
            int radius = this.getTreeRadiusForHeight(
                    originY,
                    maxHeight,
                    config.foliageRadius + 1,
                    currentY
            );

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockState state = level.getBlockState(mutablePos.setWithOffset(pos, dx, currentY, dz));
                    if (!state.isAir()) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        cir.setReturnValue(true);
    }

    private boolean isValidHugeMushroomGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM);
    }
}
