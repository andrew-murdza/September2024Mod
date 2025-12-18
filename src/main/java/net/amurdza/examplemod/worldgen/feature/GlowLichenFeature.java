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
import net.minecraft.world.level.material.Fluids;


public class GlowLichenFeature extends Feature<NoneFeatureConfiguration> {
    public GlowLichenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();

        // Only place if the target is replaceable (air, vines, plants, etc.)
        // Allow water too (glow lichen can be waterlogged).
        BlockState target = level.getBlockState(pos);
        boolean isWater = level.getFluidState(pos).getType() == Fluids.WATER;

        if (!level.isEmptyBlock(pos) && level.getFluidState(pos).getType() != Fluids.WATER)
            return false;

        if (!target.is(Blocks.WATER))
            return false;

        BlockState lichen = Blocks.GLOW_LICHEN.defaultBlockState();
        boolean anyFace = false;

        for (Direction dir : Direction.values()) {
            BlockPos supportPos = pos.relative(dir);
            BlockState supportState = level.getBlockState(supportPos);

            // If we want the lichen face on 'dir', the supporting block is at pos.relative(dir),
            // and the supporting face is dir.getOpposite().
            if (supportState.isFaceSturdy(level, supportPos, dir.getOpposite())) {
                BooleanProperty prop = GlowLichenBlock.getFaceProperty(dir);
                lichen = lichen.setValue(prop, true);
                anyFace = true;
            }
        }

        if (!anyFace) return false;

        // Waterlogging if in water
        lichen = lichen.setValue(BlockStateProperties.WATERLOGGED, isWater);

        // Final safety: vanilla survive check (optional but recommended)
        if (!lichen.canSurvive(level, pos)) return false;

        level.setBlock(pos, lichen, 3);
        return true;
    }
}
