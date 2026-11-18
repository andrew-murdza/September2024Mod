package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.event_handlers.SavannaCampProtection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class SavannaCampBoundaryMovement {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 aoemod$campBoundary(Vec3 movement) {
        return SavannaCampProtection.restrictMovement((Entity) (Object) this, movement);
    }
}
