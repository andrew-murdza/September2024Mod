package net.amurdza.examplemod.lava_fish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class BottomLavaStrollGoal extends RandomStrollGoal {

    public BottomLavaStrollGoal(PathfinderMob mob, double speedModifier, int interval) {
        super(mob, speedModifier, interval);
    }

    @Override
    public boolean canUse() {
        return this.mob.isInLava() && super.canUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        Vec3 vec = DefaultRandomPos.getPos(this.mob, 10, 7);

        for (int i = 0;
             vec != null && !this.mob.level().getFluidState(BlockPos.containing(vec)).is(FluidTags.LAVA) && i++ < 10;
             vec = DefaultRandomPos.getPos(this.mob, 10, 7)) {
        }

        if (vec == null) {
            return null;
        }

        int yDrop = 1 + this.mob.getRandom().nextInt(3);
        BlockPos pos = BlockPos.containing(vec);

        while (this.mob.level().getFluidState(pos).is(FluidTags.LAVA) && pos.getY() > this.mob.level().getMinBuildHeight()) {
            pos = pos.below();
        }

        pos = pos.above();

        for (int yUp = 0; this.mob.level().getFluidState(pos).is(FluidTags.LAVA) && yUp < yDrop; ++yUp) {
            pos = pos.above();
        }

        return Vec3.atCenterOf(pos);
    }
}