package net.amurdza.examplemod.mixins.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.amurdza.examplemod.block.BlockHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.HugeBrownMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.util.RandomSource;
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
    private boolean aoemod$allowLeavesInMushroomClearance(
            BlockState instance, TagKey<Block> tagKey, LevelAccessor level, BlockPos pos,
            int height, BlockPos.MutableBlockPos cursor, HugeMushroomFeatureConfiguration config
    ) {
        // Caps share the harvesting leaves tag; only actual leafy crowns can
        // be consumed by the new mushroom's cap. The stem is checked separately.
        return aoemod$capReplacesLeaves(config, pos) && instance.is(tagKey)
                && !(instance.getBlock() instanceof HugeMushroomBlock);
    }

    @Inject(method = "isValidPosition", at = @At("HEAD"), cancellable = true)
    private void aoemod$checkStemAndSpecialCaps(LevelAccessor level, BlockPos pos, int height,
            BlockPos.MutableBlockPos cursor, HugeMushroomFeatureConfiguration config,
            CallbackInfoReturnable<Boolean> cir) {
        // Check the entire actual stem before any cap or stem block is written.
        // Vanilla accepts leaves throughout its clearance volume and would
        // otherwise grow a partial stem through them.
        for (int y = 0; y < height; y++) {
            if (!level.getBlockState(cursor.setWithOffset(pos, 0, y, 0)).isAir()) {
                cir.setReturnValue(false);
                return;
            }
        }
        if ((Object) this instanceof HugeBrownMushroomFeature) {
            aoemod$requireBrownMushroomCanopyClearance(level, pos, height, cursor, config, cir);
            return;
        }
        if (!((Object) this instanceof twilightforest.world.components.feature.BigMushgloomFeature)) return;
        if (pos.getY() < level.getMinBuildHeight() + 1 || pos.getY() + height + 1 >= level.getMaxBuildHeight()) {
            cir.setReturnValue(false);
            return;
        }
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(BlockTags.MUSHROOM_GROW_BLOCK)
                && !(level instanceof ServerLevel serverLevel
                && BlockHelper.canGrowGiantMushroom(serverLevel, pos)
                && ground.isFaceSturdy(level, pos.below(), Direction.UP))) {
            cir.setReturnValue(false);
            return;
        }
        // Twilight's height-2 shape has a cap where its native radius check is
        // still zero, and vanilla never checks the extra top cap at height+1.
        for (int y = 0; y <= height + 1; y++) {
            int radius = y >= height ? config.foliageRadius : 0;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                BlockState state = level.getBlockState(cursor.setWithOffset(pos, dx, y, dz));
                if (!state.isAir()) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
        cir.setReturnValue(true);
    }

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
        if (!isValidHugeMushroomGround(ground)
                && !(level instanceof ServerLevel serverLevel
                && BlockHelper.canGrowGiantMushroom(serverLevel, pos)
                && ground.isFaceSturdy(level, pos.below(), Direction.UP))) {
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
                    if (!state.isAir() && !(aoemod$capReplacesLeaves(config, pos) && state.is(BlockTags.LEAVES)
                            && !(state.getBlock() instanceof HugeMushroomBlock))) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        cir.setReturnValue(true);
    }

    @Redirect(method = "isValidPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z",
            ordinal = 0))
    private boolean aoemod$allowBonemealOnAnySolidGround(BlockState state, TagKey<Block> tag,
            LevelAccessor level, BlockPos pos, int height, BlockPos.MutableBlockPos cursor,
            HugeMushroomFeatureConfiguration config) {
        return state.is(tag) || level instanceof ServerLevel serverLevel
                && BlockHelper.canGrowGiantMushroom(serverLevel, pos)
                && state.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private boolean isValidHugeMushroomGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM);
    }

    private static boolean aoemod$capReplacesLeaves(HugeMushroomFeatureConfiguration config, BlockPos pos) {
        // The native red/brown caps are constant providers. Custom mushroom caps
        // do not inherit that exception merely by extending the brown feature.
        if (!(config.capProvider instanceof SimpleStateProvider)) return false;
        BlockState cap = config.capProvider.getState(RandomSource.create(0), pos);
        return cap.is(Blocks.RED_MUSHROOM_BLOCK) || cap.is(Blocks.BROWN_MUSHROOM_BLOCK);
    }
}
