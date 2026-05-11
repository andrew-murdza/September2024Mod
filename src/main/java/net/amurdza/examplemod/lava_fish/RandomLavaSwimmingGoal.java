package net.amurdza.examplemod.lava_fish;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class RandomLavaSwimmingGoal extends RandomStrollGoal {

    public RandomLavaSwimmingGoal(PathfinderMob mob, double speedModifier, int interval) {
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
             vec != null && !this.mob.level().getFluidState(BlockPos.containing(vec)).is(net.minecraft.tags.FluidTags.LAVA) && i++ < 10;
             vec = DefaultRandomPos.getPos(this.mob, 10, 7)) {
        }

        return vec;
    }
}