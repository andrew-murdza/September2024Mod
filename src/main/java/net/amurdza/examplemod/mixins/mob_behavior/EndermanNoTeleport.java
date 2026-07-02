package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EndermanNoTeleport {
    @Inject(
            method = {
                    "teleport()Z",
                    "teleportTowards(Lnet/minecraft/world/entity/Entity;)Z",
                    "teleport(DDD)Z"
            },
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void aoemod$disableEndermanTeleport(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
