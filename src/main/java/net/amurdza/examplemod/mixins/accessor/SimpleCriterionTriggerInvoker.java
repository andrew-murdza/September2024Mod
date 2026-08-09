package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(SimpleCriterionTrigger.class)
public interface SimpleCriterionTriggerInvoker<T extends AbstractCriterionTriggerInstance> {
    @Invoker("trigger")
    void aoe$trigger(ServerPlayer player, Predicate<T> test);
}
