package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BouddhasHandFeature extends Feature<NoneFeatureConfiguration> {

    public BouddhasHandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos basePos = context.origin();

        if (!level.getBlockState(basePos).is(Blocks.SOUL_SOIL)) {
            return false;
        }

        BlockPos structureOrigin = basePos.above();

        if (!canGenerate(level, structureOrigin)) {
            return false;
        }

        generate(level, structureOrigin);
        return true;
    }

    private static boolean canGenerate(WorldGenLevel level, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);

                    if (!level.isEmptyBlock(checkPos)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static void generate(WorldGenLevel level, BlockPos pos) {

        place(level, pos, Blocks.BONE_BLOCK.defaultBlockState());

        for (int i = 1; i <= 4; i++) {
            place(level, pos.above(i), Blocks.BONE_BLOCK.defaultBlockState());
        }

        placeDirectional(level, pos.offset(0, 2, -1),
                NethersExoticismModBlocks.BONE_COLUMN.get().defaultBlockState(),
                Direction.NORTH);

        placeDirectional(level, pos.offset(0, 2, 1),
                NethersExoticismModBlocks.BONE_COLUMN.get().defaultBlockState(),
                Direction.NORTH);

        placeDirectional(level, pos.offset(1, 3, 0),
                NethersExoticismModBlocks.BONE_COLUMN.get().defaultBlockState(),
                Direction.WEST);

        placeDirectional(level, pos.offset(2, 3, 0),
                NethersExoticismModBlocks.BONE_COLUMN.get().defaultBlockState(),
                Direction.WEST);

        placeDirectional(level, pos.offset(-1, 4, 0),
                NethersExoticismModBlocks.BONE_COLUMN.get().defaultBlockState(),
                Direction.WEST);

        placeDirectional(level, pos.offset(-2, 4, 0),
                NethersExoticismModBlocks.BONE_COLUMN.get().defaultBlockState(),
                Direction.WEST);

        int[][] soulSandOffsets = {
                {0, 5, 0},
                {1, 5, 0},
                {2, 5, 0},
                {-1, 5, 0},
                {-2, 5, 0},

                {0, 5, 1},
                {0, 5, -1},
                {0, 5, 2},
                {0, 5, -2},

                {1, 5, 1},
                {1, 5, 2},
                {2, 5, 1},

                {-1, 5, -1},
                {-1, 5, -2},
                {-2, 5, -1},

                {1, 5, -1},
                {1, 5, -2},
                {2, 5, -1},

                {-1, 5, 1},
                {-1, 5, 2},
                {-2, 5, 1}
        };

        for (int[] o : soulSandOffsets) {
            place(level,
                    pos.offset(o[0], o[1], o[2]),
                    Blocks.SOUL_SAND.defaultBlockState());
        }

        place(level,
                pos.offset(-2, 3, 0),
                NethersExoticismModBlocks.BOUDDHA_S_HAND_BLOCK.get().defaultBlockState());

        place(level,
                pos.offset(0, 1, -1),
                NethersExoticismModBlocks.BOUDDHA_S_HAND_BLOCK.get().defaultBlockState());
    }

    private static void place(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }

    private static void placeDirectional(WorldGenLevel level,
                                         BlockPos pos,
                                         BlockState state,
                                         Direction direction) {

        level.setBlock(pos, applyDirectionOrAxis(state, direction), 3);
    }

    private static BlockState applyDirectionOrAxis(BlockState state,
                                                   Direction direction) {

        Property<?> facingProperty =
                state.getBlock().getStateDefinition().getProperty("facing");

        if (facingProperty instanceof DirectionProperty directionProperty
                && directionProperty.getPossibleValues().contains(direction)) {

            return state.setValue(directionProperty, direction);
        }

        Property<?> axisProperty =
                state.getBlock().getStateDefinition().getProperty("axis");

        if (axisProperty instanceof EnumProperty<?> enumProperty
                && enumProperty.getPossibleValues().contains(direction.getAxis())) {

            @SuppressWarnings("unchecked")
            EnumProperty<Direction.Axis> axis =
                    (EnumProperty<Direction.Axis>) enumProperty;

            return state.setValue(axis, direction.getAxis());
        }

        return state;
    }
}