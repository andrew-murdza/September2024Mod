package net.amurdza.examplemod.mixins.despawning;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Peaceful discards run before vanilla checks persistence; fixed rosters must survive them. */
@Mixin(Mob.class)
public abstract class AuthoredDefendersSurvivePeaceful {
    @ModifyExpressionValue(method = "checkDespawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;shouldDespawnInPeaceful()Z"))
    private boolean aoemod$preserveAuthoredDefender(boolean original) {
        Mob mob = (Mob) (Object) this;
        return original && !(mob.isPersistenceRequired() && AuthoredEncounterProgress.isDefender(mob));
    }
}
