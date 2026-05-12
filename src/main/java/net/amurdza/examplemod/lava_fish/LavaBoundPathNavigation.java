package net.amurdza.examplemod.lava_fish;

import com.scouter.netherdepthsupgrade.entity.ai.LavaSwimNodeEvaluator;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

public class LavaBoundPathNavigation extends WaterBoundPathNavigation {

    public LavaBoundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new LavaSwimNodeEvaluator(false);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.isInLava();
    }
}