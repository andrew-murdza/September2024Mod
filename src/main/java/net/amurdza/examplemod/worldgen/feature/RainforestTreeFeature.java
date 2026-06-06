package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RainforestTreeFeature extends Feature<RainforestTreeFeatureConfig> {

    private static final ResourceLocation CANOPY_TEMPLATE =
            new ResourceLocation("aoemod", "rainforest/tree/canopy");

    public RainforestTreeFeature(Codec<RainforestTreeFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RainforestTreeFeatureConfig> context) {
        WorldGenLevel level = context.level();
        RainforestTreeFeatureConfig cfg = context.config();
        BlockPos origin = context.origin();

        ServerLevel serverLevel = level.getLevel();

        StructureTemplate template = serverLevel.getStructureManager().getOrCreate(CANOPY_TEMPLATE);

        int topY = cfg.topY();
        int canopyBottomY = topY - 7;
        int trunkTopY = topY - 8;

        BlockPos canopyOrigin = new BlockPos(origin.getX(), canopyBottomY, origin.getZ());

        Set<BlockPos> vineColumns = placeCanopyAndCollectBottomVines(
                level,
                template,
                canopyOrigin,
                cfg
        );

        placeTrunk(level, origin, trunkTopY, cfg.logState());

        extendVines(level, vineColumns, canopyBottomY, cfg.caveVinesDistance());

        return true;
    }


    private static Set<BlockPos> placeCanopyAndCollectBottomVines(
            WorldGenLevel level,
            StructureTemplate template,
            BlockPos canopyOrigin,
            RainforestTreeFeatureConfig cfg
    ) {
        Set<BlockPos> vineColumns = new HashSet<>();

        StructurePlaceSettings settings = new StructurePlaceSettings();

        List<StructureTemplate.StructureBlockInfo> blocks =
                new java.util.ArrayList<>();

        blocks.addAll(template.filterBlocks(BlockPos.ZERO, settings, Blocks.OAK_LOG));
        blocks.addAll(template.filterBlocks(BlockPos.ZERO, settings, Blocks.OAK_LEAVES));
        blocks.addAll(template.filterBlocks(BlockPos.ZERO, settings, Blocks.CAVE_VINES));
        blocks.addAll(template.filterBlocks(BlockPos.ZERO, settings, Blocks.CAVE_VINES_PLANT));

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockPos rel = info.pos();
            BlockState original = info.state();

            BlockPos worldPos = canopyOrigin.offset(rel);
            BlockState replacement = replaceTemplateState(original, cfg);

            level.setBlock(worldPos, replacement, Block.UPDATE_ALL);

            if (rel.getY() == 0 && isCaveVine(original)) {
                vineColumns.add(worldPos);
            }
        }

        return vineColumns;
    }

    private static BlockState replaceTemplateState(
            BlockState original,
            RainforestTreeFeatureConfig cfg
    ) {
        if (isLog(original)) {
            return copyLogAxis(original, cfg.logState());
        }

        if (isLeaves(original)) {
            return copyLeafProperties(original, cfg.leavesState());
        }

        if (isCaveVine(original)) {
            return makeTemplateVineState(original);
        }

        return original;
    }

    private static boolean isLog(BlockState state) {
        return state.is(Blocks.OAK_LOG);
    }

    private static boolean isLeaves(BlockState state) {
        return state.is(Blocks.OAK_LEAVES);
    }

    private static boolean isCaveVine(BlockState state) {
        return state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT);
    }

    private static BlockState copyLogAxis(BlockState original, BlockState replacement) {
        if (
                original.hasProperty(RotatedPillarBlock.AXIS)
                        && replacement.hasProperty(RotatedPillarBlock.AXIS)
        ) {
            return replacement.setValue(
                    RotatedPillarBlock.AXIS,
                    original.getValue(RotatedPillarBlock.AXIS)
            );
        }

        return replacement;
    }

    private static BlockState copyLeafProperties(BlockState original, BlockState replacement) {
        BlockState result = replacement;

        if (
                original.hasProperty(LeavesBlock.DISTANCE)
                        && result.hasProperty(LeavesBlock.DISTANCE)
        ) {
            result = result.setValue(
                    LeavesBlock.DISTANCE,
                    original.getValue(LeavesBlock.DISTANCE)
            );
        }

        if (result.hasProperty(LeavesBlock.PERSISTENT)) {
            result = result.setValue(LeavesBlock.PERSISTENT, false);
        }

        return result;
    }

    private static BlockState makeTemplateVineState(BlockState original) {
        BlockState result = original;

        if (result.hasProperty(CaveVines.BERRIES)) {
            result = result.setValue(CaveVines.BERRIES, true);
        }

        return result;
    }

    private static void placeTrunk(
            WorldGenLevel level,
            BlockPos origin,
            int trunkTopY,
            BlockState logState
    ) {
        for (int dx = 7; dx <= 8; dx++) {
            for (int dz = 7; dz <= 8; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                int startY = firstNonSolidY(level, x, origin.getY(), z, trunkTopY);

                for (int y = startY; y <= trunkTopY; y++) {
                    level.setBlock(
                            new BlockPos(x, y, z),
                            verticalLog(logState),
                            Block.UPDATE_ALL
                    );
                }
            }
        }
    }

    private static int firstNonSolidY(
            WorldGenLevel level,
            int x,
            int startY,
            int z,
            int maxY
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);

        for (int y = startY; y <= maxY; y++) {
            pos.set(x, y, z);

            if (!isSolid(level, pos)) {
                return y;
            }
        }

        return maxY + 1;
    }

    private static BlockState verticalLog(BlockState state) {
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            return state.setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        }

        return state;
    }

    private static void extendVines(
            WorldGenLevel level,
            Set<BlockPos> vineColumns,
            int canopyBottomY,
            int caveVinesDistance
    ) {
        for (BlockPos anchor : vineColumns) {
            int groundOrWaterY = findClosestSolidOrWaterBelow(level, anchor);

            if (groundOrWaterY == Integer.MIN_VALUE) {
                continue;
            }

            int bottomVineY = groundOrWaterY + caveVinesDistance + 1;
            int firstExtensionY = canopyBottomY - 1;

            if (bottomVineY > firstExtensionY) {
                continue;
            }

            for (int y = firstExtensionY; y >= bottomVineY; y--) {
                BlockPos pos = new BlockPos(anchor.getX(), y, anchor.getZ());

                if (!level.isEmptyBlock(pos) && !isCaveVine(level.getBlockState(pos))) {
                    break;
                }

                boolean bottom = y == bottomVineY;

                level.setBlock(
                        pos,
                        bottom ? bottomVineState() : vinePlantState(),
                        Block.UPDATE_ALL
                );
            }
        }
    }

    private static int findClosestSolidOrWaterBelow(WorldGenLevel level, BlockPos anchor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = anchor.getY() - 1; y >= level.getMinBuildHeight(); y--) {
            pos.set(anchor.getX(), y, anchor.getZ());

            if (isSolid(level, pos) || isWater(level, pos)) {
                return y;
            }
        }

        return Integer.MIN_VALUE;
    }

    private static boolean isSolid(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos);
    }

    private static boolean isWater(WorldGenLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(net.minecraft.tags.FluidTags.WATER);
    }

    private static BlockState vinePlantState() {
        BlockState state = Blocks.CAVE_VINES_PLANT.defaultBlockState();

        if (state.hasProperty(CaveVines.BERRIES)) {
            state = state.setValue(CaveVines.BERRIES, true);
        }

        return state;
    }

    private static BlockState bottomVineState() {
        BlockState state = Blocks.CAVE_VINES.defaultBlockState();

        if (state.hasProperty(CaveVines.BERRIES)) {
            state = state.setValue(CaveVines.BERRIES, true);
        }

        if (state.hasProperty(CaveVinesBlock.AGE)) {
            state = state.setValue(CaveVinesBlock.AGE, 25);
        } else if (state.hasProperty(BlockStateProperties.AGE_25)) {
            state = state.setValue(BlockStateProperties.AGE_25, 25);
        }

        return state;
    }
}