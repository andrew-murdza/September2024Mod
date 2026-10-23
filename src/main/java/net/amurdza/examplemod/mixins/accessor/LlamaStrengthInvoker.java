package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.world.entity.animal.horse.Llama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Llama.class)
public interface LlamaStrengthInvoker {
    @Invoker("setStrength")
    void aoe$setStrength(int strength);
}
