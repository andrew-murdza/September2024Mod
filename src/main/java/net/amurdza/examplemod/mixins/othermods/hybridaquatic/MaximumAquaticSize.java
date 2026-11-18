package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Uses each installed species' own size limit rather than a guessed common scale. */
@Mixin(value = BaseWaterAnimal.class, remap = false)
public interface MaximumAquaticSize {
    @Invoker("getMaxSize")
    int aoemod$maximumSize();
}
