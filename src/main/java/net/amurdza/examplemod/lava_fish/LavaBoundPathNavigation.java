package net.amurdza.examplemod.lava_fish;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;

public class LavaBoundPathNavigation extends WaterBoundPathNavigation {

    public LavaBoundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.isInLava();
    }
}