package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.worldgen.SmallAquaticPlants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Guard the movement request before modded collision engines can skip no-collision plant blocks. */
@Mixin(Entity.class)
public abstract class SmallAquaticPlantMovement {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 aoemod$plantBoundary(Vec3 movement) {
        return SmallAquaticPlants.restrictMovement((Entity) (Object) this, movement);
    }
}
