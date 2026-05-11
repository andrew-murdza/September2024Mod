package net.amurdza.examplemod.lava_fish;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public class LavaGroundPathNavigation extends GroundPathNavigation {

    public LavaGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected boolean hasValidPathType(BlockPathTypes pathType) {
        if (pathType == BlockPathTypes.WATER) {
            return false;
        }

        if (pathType == BlockPathTypes.LAVA) {
            return true;
        }

        return pathType != BlockPathTypes.OPEN;
    }
}