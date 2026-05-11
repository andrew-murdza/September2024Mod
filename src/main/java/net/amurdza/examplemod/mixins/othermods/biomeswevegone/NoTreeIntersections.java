package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import com.llamalad7.mixinextras.sugar.Local;
import dev.corgitaco.ohthetreesyoullgrow.world.level.levelgen.feature.TreeFromStructureNBTFeature;
import dev.corgitaco.ohthetreesyoullgrow.world.level.levelgen.feature.configurations.TreeFromStructureNBTConfig;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = TreeFromStructureNBTFeature.class,remap = false)
public class NoTreeIntersections {

    @Inject(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/corgitaco/ohthetreesyoullgrow/world/level/levelgen/feature/TreeFromStructureNBTFeature;placeTrunk(Ldev/corgitaco/ohthetreesyoullgrow/world/level/levelgen/feature/configurations/TreeFromStructureNBTConfig;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$Palette;Lnet/minecraft/core/BlockPos;Ljava/util/List;Ljava/util/List;Ljava/util/Set;Ljava/util/Set;I)V"
            ),
            cancellable = true
    )
    private void aoemod$failIfSolidTreeBlocksCollide(
            FeaturePlaceContext<TreeFromStructureNBTConfig> context,
            CallbackInfoReturnable<Boolean> cir,

            @Local TreeFromStructureNBTConfig config,
            @Local WorldGenLevel level,

            @Local(ordinal = 0) BlockPos origin,
            @Local(ordinal = 1) BlockPos centerOffset,

            @Local RandomSource random,
            @Local StructurePlaceSettings placeSettings,

            @Local(ordinal = 0) StructureTemplate.Palette trunkBasePalette,
            @Local(ordinal = 1) StructureTemplate.Palette randomCanopyPalette,

            @Local(ordinal = 0) int trunkLength,
            @Local(ordinal = 1) int maxTrunkBuildingDepth
    ) {
        if (aoemod$hasAnyBadCollision(
                config,
                level,
                origin,
                placeSettings,
                trunkBasePalette,
                centerOffset,
                maxTrunkBuildingDepth,
                config.growableOn(),
                true
        )) {
            cir.setReturnValue(false);
            return;
        }

        BlockPos canopyCenterOffset =
                aoemod$getCanopyCenterOffset(config, randomCanopyPalette, trunkLength);

        if (aoemod$hasAnyBadCollision(
                config,
                level,
                origin,
                placeSettings,
                randomCanopyPalette,
                canopyCenterOffset,
                trunkLength + 1,
                BlockPredicate.matchesBlocks(config.logTarget().toArray(new Block[0])),
                false
        )) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean aoemod$hasAnyBadCollision(
            TreeFromStructureNBTConfig config,
            WorldGenLevel level,
            BlockPos origin,
            StructurePlaceSettings placeSettings,
            StructureTemplate.Palette palette,
            BlockPos centerOffset,
            int maxTrunkBuildingDepth,
            BlockPredicate fillGroundFilter,
            boolean isTrunkBase
    ) {
        if (aoemod$hasBadCollisionFromPaletteBlocks(
                config.placeFromNBT(),
                level,
                origin,
                placeSettings,
                palette,
                centerOffset
        )) {
            return true;
        }

        if (aoemod$hasBadCollisionFromPaletteBlocks(
                config.logTarget(),
                level,
                origin,
                placeSettings,
                palette,
                centerOffset
        )) {
            return true;
        }

        if (aoemod$hasBadCollisionFromPaletteBlocks(
                config.leavesTarget(),
                level,
                origin,
                placeSettings,
                palette,
                centerOffset
        )) {
            return true;
        }

        List<StructureTemplate.StructureBlockInfo> logBuilders;

        if (isTrunkBase) {
            logBuilders = palette.blocks(Blocks.RED_WOOL);
        } else {
            logBuilders = new ArrayList<>(palette.blocks(Blocks.RED_WOOL));
        }

        return aoemod$hasBadFillLogsUnderCollision(
                level,
                origin,
                placeSettings,
                centerOffset,
                logBuilders,
                maxTrunkBuildingDepth,
                fillGroundFilter
        );
    }

    @Unique
    private static boolean aoemod$hasBadCollisionFromPaletteBlocks(
            Iterable<Block> blockTargets,
            WorldGenLevel level,
            BlockPos origin,
            StructurePlaceSettings placeSettings,
            StructureTemplate.Palette palette,
            BlockPos centerOffset
    ) {
        List<StructureTemplate.StructureBlockInfo> blocks =
                TreeFromStructureNBTFeature.getStructureInfosInStructurePalletteFromBlockList(blockTargets, palette);

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockPos pos = TreeFromStructureNBTFeature.getModifiedPos(placeSettings, info, centerOffset, origin);

            if (aoemod$isBadReplacement(info.state(), level.getBlockState(pos))) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private static boolean aoemod$hasBadFillLogsUnderCollision(
            WorldGenLevel level,
            BlockPos origin,
            StructurePlaceSettings placeSettings,
            BlockPos centerOffset,
            List<StructureTemplate.StructureBlockInfo> logBuilders,
            int maxTrunkBuildingDepth,
            BlockPredicate groundFilter
    ) {
        for (StructureTemplate.StructureBlockInfo logBuilder : logBuilders) {
            BlockPos pos = TreeFromStructureNBTFeature.getModifiedPos(placeSettings, logBuilder, centerOffset, origin);
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(pos);

            for (int i = 0; i < maxTrunkBuildingDepth; ++i) {
                if (groundFilter.test(level, mutable) || level.getBlockState(mutable).is(Blocks.BEDROCK)) {
                    break;
                }

                BlockState existingState = level.getBlockState(mutable);

                /*
                 * We do not know the exact logProvider state here without consuming random.
                 * So this treats fillLogsUnder as potentially non-log/non-leaf.
                 */
                if(aoemod$isBadReplacement(logBuilder.state(),existingState)){
                    return true;
                }

                mutable.move(Direction.DOWN);
            }
        }

        return false;
    }

    @Unique
    private static boolean aoemod$isBadReplacement(
            BlockState generatedState,
            BlockState existingState
    ) {
        boolean generatedIsLogOrLeaf =
                !generatedState.is(ModTags.Blocks.mushroomBlocks);

        boolean existingIsAirOrLeaf =
                existingState.isAir() ||
                        existingState.is(BlockTags.LEAVES);

        return !generatedIsLogOrLeaf && !existingIsAirOrLeaf || existingState.is(BlockTags.SCULK_REPLACEABLE);
    }

    @Unique
    private static BlockPos aoemod$getCanopyCenterOffset(
            TreeFromStructureNBTConfig config,
            StructureTemplate.Palette canopyPalette,
            int trunkLength
    ) {
        List<StructureTemplate.StructureBlockInfo> canopyAnchor = canopyPalette.blocks(Blocks.WHITE_WOOL);

        if (canopyAnchor.isEmpty()) {
            throw new IllegalArgumentException(
                    "No canopy anchor was specified for structure NBT palette %s. Canopy anchor is specified with white wool."
                            .formatted(config.canopyLocation())
            );
        }

        if (canopyAnchor.size() > 1) {
            throw new IllegalArgumentException(
                    "There cannot be more than one canopy anchor for structure NBT palette %s. Canopy anchor is specified with white wool."
                            .formatted(config.canopyLocation())
            );
        }

        BlockPos anchor = canopyAnchor.get(0).pos();
        return new BlockPos(-anchor.getX(), trunkLength, -anchor.getZ());
    }
}