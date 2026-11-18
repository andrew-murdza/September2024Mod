package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The source breeding routine randomizes its parent as well as creating a new child. */
@Mixin(value = BaseWaterAnimal.class, remap = false)
public abstract class AquaticBreedingSize {
    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("RETURN"))
    private void aoemod$keepMaximumSize(ServerLevel level, BaseWaterAnimal mate, CallbackInfo ci) {
        DocumentedNaturalSpawns.maximizeAquaticSize((BaseWaterAnimal) (Object) this);
        DocumentedNaturalSpawns.maximizeAquaticSize(mate);
    }
}
