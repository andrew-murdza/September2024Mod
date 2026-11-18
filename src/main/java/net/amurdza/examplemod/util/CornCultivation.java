package net.amurdza.examplemod.util;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;

/** Every corn section uses the farmland beneath the whole stalk. */
public final class CornCultivation {
    private CornCultivation() { }

    public static BlockPos soil(LevelReader level, BlockPos section) {
        BlockPos root = section;
        for (int i = 0; i < 2 && level.getBlockState(root.below()).getBlock() instanceof CornStalkBlock; i++) {
            root = root.below();
        }
        return root.below();
    }

    public static boolean hasFarmland(LevelReader level, BlockPos section) {
        return level.getBlockState(soil(level, section)).getBlock() instanceof FarmBlock;
    }

    public static float moistureMultiplier(LevelReader level, BlockPos section) {
        BlockPos soil = soil(level, section);
        return level.getBlockState(soil).isFertile(level, soil) ? 1.0F : 0.5F;
    }
}
