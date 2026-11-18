package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Animal.class)
public abstract class GroundAnimalMovement extends AgeableMob {
    protected GroundAnimalMovement(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
    }

    // Alex's Caves replaces Entity.collide, and portals have a separate collision implementation.
    // Override the shared Forge getter so every implementation receives the effective step limit.
    @Override
    public float getStepHeight() {
        float step = super.getStepHeight();
        return getNavigation() instanceof FlyingPathNavigation || getNavigation() instanceof WaterBoundPathNavigation
                ? step : Math.min(step, 1.0F);
    }
}
