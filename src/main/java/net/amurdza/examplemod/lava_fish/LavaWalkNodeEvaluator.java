package net.amurdza.examplemod.lava_fish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class LavaWalkNodeEvaluator extends WalkNodeEvaluator {

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
        BlockPathTypes original = super.getBlockPathType(level, x, y, z, mob);

        if (level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.LAVA)) {
            return BlockPathTypes.WALKABLE;
        }

        return original;
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
        if (level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.LAVA)) {
            return BlockPathTypes.WALKABLE;
        }

        return super.getBlockPathType(level, x, y, z);
    }

    @Override
    protected double getFloorLevel(BlockPos pos) {
        if (this.level.getFluidState(pos).is(FluidTags.LAVA)) {
            return pos.getY();
        }

        return super.getFloorLevel(pos);
    }
}
