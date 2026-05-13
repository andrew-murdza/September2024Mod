package net.amurdza.examplemod.lava_fish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class LavaGroundPathNavigation extends GroundPathNavigation {

    public LavaGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new LavaWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.onGround() || this.mob.isInLava() || this.mob.isPassenger();
    }

    @Override
    protected @NotNull Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), Mth.floor(this.mob.getY() + 0.5D), this.mob.getZ());
    }

    @Override
    public Path createPath(@NotNull BlockPos pos, int accuracy) {
        if (this.level.getFluidState(pos).is(FluidTags.LAVA)) {
            return super.createPath(pos, accuracy);
        }

        return super.createPath(pos, accuracy);
    }

    @Override
    public Path createPath(Entity entity, int accuracy) {
        return this.createPath(entity.blockPosition(), accuracy);
    }

    @Override
    protected boolean hasValidPathType(@NotNull BlockPathTypes pathType) {
        if (pathType == BlockPathTypes.LAVA) {
            return true;
        }

        return super.hasValidPathType(pathType);
    }
}