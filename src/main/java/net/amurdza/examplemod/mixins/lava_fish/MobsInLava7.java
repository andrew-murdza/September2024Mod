package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GroundPathNavigation.class)
public abstract class MobsInLava7 extends PathNavigation {
    public MobsInLava7(Mob pMob, Level pLevel) {
        super(pMob, pLevel);
    }

    @Inject(method = "hasValidPathType", at = @At("HEAD"), cancellable = true)
    private void aoemod$lavaIsValidGroundPathType(
            BlockPathTypes pathType,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LavaMobs.isLavaMob(mob) && pathType == BlockPathTypes.LAVA) {
            cir.setReturnValue(true);
        }
    }
}
