package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.monster.Strider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Strider.class)
public class NoStriderShiver {
    @ModifyVariable(method = "setSuffocating", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean aoemod$remainWarm(boolean suffocating) {
        // Covers cold ticks, passengers and restored NBT, including the speed penalty.
        return false;
    }
}
