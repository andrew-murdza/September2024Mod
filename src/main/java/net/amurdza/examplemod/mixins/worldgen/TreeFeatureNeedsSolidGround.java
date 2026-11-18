package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.worldgen.SnowySpruceHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

import java.util.function.BiConsumer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.levelgen.feature.trunkplacers.MegaJungleTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeFeature.class)
public class TreeFeatureNeedsSolidGround {

    @Inject(method = "validTreePos", at = @At("HEAD"), cancellable = true)
    private static void aoemod$growThroughTropicalVines(net.minecraft.world.level.LevelSimulatedReader level,
            BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        if (level instanceof WorldGenLevel world && GameplayDimensions.is(world, GameplayDimensions.TROPICAL)
                && net.amurdza.examplemod.worldgen.TropicalPlantIntegrity.isCaveVine(world.getBlockState(pos))) {
            result.setReturnValue(true);
        }
    }

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
        Map<BlockPos, BlockState> placedLogs = new LinkedHashMap<>();
        Map<BlockPos, BlockState> mangroveLeaves = new LinkedHashMap<>();
        BiConsumer<BlockPos, BlockState> checkedLogs = (pos, state) -> {
            placedLog[0] = true;
            if (state.is(BlockTags.LOGS)) placedLogs.put(pos.immutable(), state);
            logs.accept(pos, state);
        };
        FoliagePlacer.FoliageSetter checkedLeaves = leaves;
        int spruceExtension = SnowySpruceHeight.extension(level, origin, configuration);
        if (spruceExtension > 0 || GameplayDimensions.is(level, GameplayDimensions.PLAINS)) {
            int crownFloor = origin.getY() + (spruceExtension > 0 ? spruceExtension : 2);
            checkedLeaves = new FoliagePlacer.FoliageSetter() {
                @Override
                public void set(BlockPos pos, BlockState state) {
                    // Preserve the spruce's clipped basal crown and keep two
                    // clear blocks below Plains foliage on level ground.
                    if (pos.getY() >= crownFloor) leaves.set(pos, state);
                }

                @Override
                public boolean isSet(BlockPos pos) {
                    return pos.getY() >= crownFloor && leaves.isSet(pos);
                }
            };
        } else if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {
            checkedLeaves = new FoliagePlacer.FoliageSetter() {
                @Override
                public void set(BlockPos pos, BlockState state) {
                    if (!state.is(Blocks.MANGROVE_LEAVES)
                            || net.amurdza.examplemod.worldgen.TropicalTreeShapes.mangroveLeafAllowed(level, origin, pos)) {
                        leaves.set(pos, state);
                        if (state.is(Blocks.MANGROVE_LEAVES)) mangroveLeaves.put(pos.immutable(), state);
                    }
                }

                @Override
                public boolean isSet(BlockPos pos) {
                    return leaves.isSet(pos);
                }
            };
        }
        boolean placed = original.call(tree, level, random, origin, roots, checkedLogs, checkedLeaves, configuration);
        // Forking and fancy trunks already choose one route and prune lower duplicates.
        // The tropical two-log elbow rule would add those hanging logs back.
        if (placed && !(configuration.trunkPlacer instanceof ForkingTrunkPlacer)
                && !(configuration.trunkPlacer instanceof FancyTrunkPlacer)
                && !net.amurdza.examplemod.worldgen.TropicalTreeShapes.completeDiagonalBridges(level, placedLogs, checkedLogs)) return false;
        if (placed && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {
            if (configuration.trunkPlacer instanceof MegaJungleTrunkPlacer
                    && !net.amurdza.examplemod.worldgen.TropicalTreeShapes.connectJungleBranches(level, origin, placedLogs, checkedLogs)) return false;
            net.amurdza.examplemod.worldgen.TropicalTreeShapes.fillMangroveColumns(level, origin, mangroveLeaves, leaves::set);
        }
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
        boolean isMangrove = context.config().rootPlacer != null && context.config().rootPlacer.isPresent()
                && context.config().foliageProvider.getState(context.random(), origin).is(Blocks.MANGROVE_LEAVES);
        if (GameplayDimensions.isCustom(level)
                && (level.getBlockState(origin).is(BlockTags.LOGS) || level.getBlockState(origin).is(BlockTags.LEAVES))) {
            cir.setReturnValue(false);
            return;
        }

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
                                || nearbyState.is(ModBlocks.CLOSED_EYEBLOSSOM.get())
                                || nearbyState.is(ModBlocks.OPEN_EYEBLOSSOM.get())) {
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
            }
        }

        // Mangrove root placers are designed to start in shallow water. The
        // blanket dry-origin check previously confined swamps to dry shorelines.
        if (!level.getFluidState(origin).isEmpty()
                && !(isMangrove && level.getFluidState(origin).is(FluidTags.WATER))) {
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
            if (groundState.is(BlockTags.LEAVES) || groundState.is(BlockTags.LOGS)
                    || !groundState.is(BlockTags.DIRT) && !(isMangrove
                    && (groundState.is(Blocks.MUD) || groundState.is(Blocks.CLAY)))) {
                cir.setReturnValue(false);
            }
        }
    }
}
