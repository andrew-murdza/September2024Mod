package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


public class GlowLichenFeature extends Feature<NoneFeatureConfiguration> {
    public GlowLichenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();

        BlockState target = level.getBlockState(pos);

        // Only replace actual full water source blocks
        if (!target.is(Blocks.WATER)) {
            return false;
        }

        BlockState lichen = Blocks.GLOW_LICHEN.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true);

        boolean anyFace = false;

        for (Direction dir:Direction.values()) {
            BlockPos supportPos = pos.relative(dir);
            BlockState supportState = level.getBlockState(supportPos);

            if (supportState.isFaceSturdy(level, supportPos, dir.getOpposite())) {
                BooleanProperty prop = GlowLichenBlock.getFaceProperty(dir);
                lichen = lichen.setValue(prop, true);
                anyFace = true;
            }
        }

        if (!anyFace) {
            return false;
        }

        if (!lichen.canSurvive(level, pos)) {
            return false;
        }

        level.setBlock(pos, lichen, 3);
        return true;
    }
}
