package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SonicBoom.class)
public abstract class WardenNoSonicBoom {
    @Inject(
            method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void aoemod$disableEliteWardenRangedAttack(
            ServerLevel level,
            Warden warden,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(false);
    }
}
