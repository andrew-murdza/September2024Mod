package net.amurdza.examplemod.worldgen.feature;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityLeafcutterAnthill;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class LeafcutterAnthillFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = 3;

    private static final int X_OFFSET = 8;
    private static final int Z_OFFSET = 8;

    private static final int OUT_OF_GROUND_INITIAL = 3;
    private static final int OUT_OF_GROUND = 2;

    private static final int MOUND_RADIUS_X = 4;
    private static final int MOUND_RADIUS_Z = 4;
    private static final float MOUND_RADIUS = (MOUND_RADIUS_X + MOUND_RADIUS_Z) * 0.333F;

    private static final int SIDE_COLUMN_DEPTH = 2;

    private static final int AIR_CLEAR_ABOVE = 2;

    private static final int SHAFT_DEPTH = 2;

    private static final int CHAMBER_RADIUS_X = 1;
    private static final int CHAMBER_RADIUS_Y = 1;
    private static final int CHAMBER_RADIUS_Z = 1;
    private static final float CHAMBER_RADIUS = (CHAMBER_RADIUS_X + CHAMBER_RADIUS_Y + CHAMBER_RADIUS_Z) * 0.333F + 0.5F;

    private static final int ANTS = 5;

    public LeafcutterAnthillFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        int y = level.getHeight(
                Heightmap.Types.WORLD_SURFACE_WG,
                origin.getX() + X_OFFSET,
                origin.getZ() + Z_OFFSET
        );

        BlockPos heightPos = new BlockPos(
                origin.getX() + X_OFFSET,
                y,
                origin.getZ() + Z_OFFSET
        );

        if (!level.getFluidState(heightPos.below()).isEmpty()) {
            return false;
        }

        placeMaximumMound(level, heightPos);

        if (level.getBlockState(heightPos.above(OUT_OF_GROUND + 1)).getBlock() != AMBlockRegistry.LEAFCUTTER_ANTHILL.get()
                && level.getBlockState(heightPos.above(OUT_OF_GROUND - 1)).getBlock() != AMBlockRegistry.LEAFCUTTER_ANTHILL.get()) {
            BlockPos anthillPos = heightPos.above(OUT_OF_GROUND);
            placeAnthill(level, anthillPos);

            placeMossColumn(level, heightPos.north());
            placeMossColumn(level, heightPos.east());
            placeMossColumn(level, heightPos.south());
            placeMossColumn(level, heightPos.west());

            for (int i = 1; i <= AIR_CLEAR_ABOVE; i++) {
                level.setBlock(heightPos.above(OUT_OF_GROUND + i), Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }

        for (int i = OUT_OF_GROUND - 1; i >= -SHAFT_DEPTH; i--) {
            placeChamber(level, heightPos.above(i));
        }

        BlockPos chamberCenter = heightPos.below(SHAFT_DEPTH + CHAMBER_RADIUS_Y);

        for (BlockPos pos : BlockPos.betweenClosed(
                chamberCenter.offset(-CHAMBER_RADIUS_X, -CHAMBER_RADIUS_Y, -CHAMBER_RADIUS_Z),
                chamberCenter.offset(CHAMBER_RADIUS_X, CHAMBER_RADIUS_Y, CHAMBER_RADIUS_Z)
        )) {
            if (pos.distSqr(chamberCenter) < CHAMBER_RADIUS * CHAMBER_RADIUS) {
                placeChamber(level, pos);
            }
        }

        return true;
    }

    private static void placeMaximumMound(WorldGenLevel level, BlockPos heightPos) {
        for (int i = 0; i < OUT_OF_GROUND_INITIAL; i++) {
            for (BlockPos pos : BlockPos.betweenClosed(
                    heightPos.offset(-MOUND_RADIUS_X, 0, -MOUND_RADIUS_Z),
                    heightPos.offset(MOUND_RADIUS_X, 3, MOUND_RADIUS_Z)
            )) {
                if (pos.distSqr(heightPos) <= MOUND_RADIUS * MOUND_RADIUS) {
                    placeMoss(level, pos);
                }
            }
        }
    }

    private static void placeAnthill(WorldGenLevel level, BlockPos pos) {
        level.setBlock(pos, AMBlockRegistry.LEAFCUTTER_ANTHILL.get().defaultBlockState(), FLAGS);
        placeMossAroundAnthill(level, pos);

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof TileEntityLeafcutterAnthill anthillBlockEntity && anthillBlockEntity.hasNoAnts()) {
            for (int i = 0; i < ANTS; i++) {
                EntityLeafcutterAnt ant = new EntityLeafcutterAnt(
                        AMEntityRegistry.LEAFCUTTER_ANT.get(),
                        level.getLevel()
                );

                ant.setQueen(i == 0);
                anthillBlockEntity.tryEnterHive(ant, false, 598);
            }
        }
    }

    private static void placeChamber(WorldGenLevel level, BlockPos pos) {
        level.setBlock(pos, AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().defaultBlockState(), FLAGS);
        placeMossAroundChamber(level, pos);
    }

    private static void placeMossColumn(WorldGenLevel level, BlockPos basePos) {
        for (int i = 0; i <= SIDE_COLUMN_DEPTH; i++) {
            placeMoss(level, basePos.above(LeafcutterAnthillFeature.OUT_OF_GROUND - i));
        }
    }

    private static void placeMossAroundAnthill(WorldGenLevel level, BlockPos anthillPos) {
        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP) {
                placeMossIfNotAnthillOrChamber(level, anthillPos.relative(direction));
            }
        }
    }

    private static void placeMossAroundChamber(WorldGenLevel level, BlockPos chamberPos) {
        for (Direction direction : Direction.values()) {
            placeMossIfNotAnthillOrChamber(level, chamberPos.relative(direction));
        }
    }

    private static void placeMossIfNotAnthillOrChamber(WorldGenLevel level, BlockPos pos) {
        if (!isAnthillOrChamber(level, pos)) {
            placeMoss(level, pos);
        }
    }

    private static void placeMoss(WorldGenLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.MOSS_BLOCK.defaultBlockState(), FLAGS);
    }

    private static boolean isAnthillOrChamber(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(AMBlockRegistry.LEAFCUTTER_ANTHILL.get())
                || level.getBlockState(pos).is(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get());
    }
}