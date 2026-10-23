package net.amurdza.examplemod.worldgen.feature.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

/** A sulfur deposit is anchored to lava, while the rest of its blob may continue into the rock. */
public final class LavaBorderSulfurFeature extends Feature<NoneFeatureConfiguration> {
    public LavaBorderSulfurFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var sulfur = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", "sulfur"));
        if (sulfur == null || sulfur == Blocks.AIR) return false;
        BlockPos anchor = null;
        for (BlockPos pos : BlockPos.betweenClosed(context.origin().offset(-4, -4, -4), context.origin().offset(4, 4, 4))) {
            if (!host(level.getBlockState(pos))) continue;
            for (Direction direction : Direction.values()) {
                if (level.getFluidState(pos.relative(direction)).is(FluidTags.LAVA)) {
                    anchor = pos.immutable();
                    break;
                }
            }
            if (anchor != null) break;
        }
        if (anchor == null) return false;
        boolean placed = false;
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-3, -2, -3), anchor.offset(3, 2, 3))) {
            if (pos.distSqr(anchor) <= 9 && host(level.getBlockState(pos))) {
                placed |= level.setBlock(pos, sulfur.defaultBlockState(), 2);
            }
        }
        return placed;
    }

    private static boolean host(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE);
    }
}
