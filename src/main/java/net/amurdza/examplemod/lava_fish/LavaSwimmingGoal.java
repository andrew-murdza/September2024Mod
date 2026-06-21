package net.amurdza.examplemod.lava_fish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LavaSwimmingGoal extends RandomStrollGoal {
    public LavaSwimmingGoal(PathfinderMob mob) {
        super(mob, 1.0D, 40);
    }

    @Override
    public boolean canUse() {
        if ((!this.mob.isInLava() && !this.mob.isInWater())
                || this.mob.isPassenger()
                || this.mob.getTarget() != null) {
            return false;
        }

        if (!this.forceTrigger && this.mob.getRandom().nextInt(this.interval) >= 100) {
            return false;
        }

        Vec3 pos = this.getPosition();
        if (pos == null) {
            return false;
        }

        this.wantedX = pos.x;
        this.wantedY = pos.y;
        this.wantedZ = pos.z;
        this.forceTrigger = false;
        return true;
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        Vec3 pos = DefaultRandomPos.getPos(this.mob, 10, 7);

        for (int i = 0;
             pos != null
                     && !this.mob.level().getFluidState(BlockPos.containing(pos)).is(FluidTags.LAVA)
                     && i++ < 10;
             pos = DefaultRandomPos.getPos(this.mob, 10, 7)) {
        }

        return pos;
    }
}
