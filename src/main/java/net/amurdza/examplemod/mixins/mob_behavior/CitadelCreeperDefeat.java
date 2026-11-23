package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProgress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A detonating Creeper discards itself without raising LivingDeathEvent. */
@Mixin(Creeper.class)
public abstract class CitadelCreeperDefeat {
    @Inject(method = "explodeCreeper", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/Creeper;discard()V"))
    private void aoemod$countDetonatedCitadelDefender(CallbackInfo callback) {
        var creeper = (Creeper) (Object) this;
        if (!(creeper.level() instanceof ServerLevel level) || !AuthoredEncounterProgress.isDefender(creeper)
                || !NetherCitadelProgress.KIND.equals(AuthoredEncounterProgress.kind(creeper))) return;
        // Forge may cancel the blast, but vanilla still discards its source immediately afterward.
        // Record the removed enemy exactly once, using the same progression gate as ordinary deaths.
        if (AuthoredEncounterProgress.get(level).defeat(creeper)) MinecraftForge.EVENT_BUS.post(
                new AuthoredEncounterProgress.Cleared(level, AuthoredEncounterProgress.encounterId(creeper),
                        NetherCitadelProgress.KIND));
    }
}
