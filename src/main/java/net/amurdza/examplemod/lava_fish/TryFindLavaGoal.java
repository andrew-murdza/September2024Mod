package net.amurdza.examplemod.lava_fish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class TryFindLavaGoal extends Goal {
    private final PathfinderMob mob;
    private double wantedX;
    private double wantedY;
    private double wantedZ;

    public TryFindLavaGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isInLava()) {
            return false;
        }

        Vec3 pos = this.findNearestLava();
        if (pos == null) {
            return false;
        }

        this.wantedX = pos.x;
        this.wantedY = pos.y;
        this.wantedZ = pos.z;
        return true;
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, 1.0D);
    }

    private Vec3 findNearestLava() {
        LevelReader level = this.mob.level();
        BlockPos origin = this.mob.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(8, 4, 8))) {
            if (level.getFluidState(pos).is(FluidTags.LAVA)) {
                return Vec3.atCenterOf(pos);
            }
        }

        return null;
    }
}