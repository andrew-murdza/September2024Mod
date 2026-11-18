package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.SoulCaveClock;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Creaking.class, remap = false)
public abstract class CreakingNightActivity {
    @Inject(method = "checkCanMove", at = @At("HEAD"), cancellable = true)
    private void aoemod$restDuringDay(CallbackInfoReturnable<Boolean> cir) {
        Creaking creaking = (Creaking) (Object) this;
        if (GameplayDimensions.is(creaking.level(), GameplayDimensions.NETHER) && !SoulCaveClock.isNight(creaking.level())) {
            if (creaking.isActive()) creaking.deactivate();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true, remap = true)
    private void aoemod$noDaytimeAttacks(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Creaking creaking = (Creaking) (Object) this;
        if (GameplayDimensions.is(creaking.level(), GameplayDimensions.NETHER) && !SoulCaveClock.isNight(creaking.level())) {
            cir.setReturnValue(false);
        }
    }
}
