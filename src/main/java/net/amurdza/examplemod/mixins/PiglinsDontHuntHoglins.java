package net.amurdza.examplemod.mixins;

import com.mojang.datafixers.kinds.App;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.StartHuntingHoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(StartHuntingHoglin.class)
public class PiglinsDontHuntHoglins {
    @Redirect(method = "create",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder;create(Ljava/util/function/Function;)Lnet/minecraft/world/entity/ai/behavior/OneShot;"))
    private static<E extends LivingEntity> OneShot<E> hi(Function<BehaviorBuilder.Instance<E>, ? extends App<BehaviorBuilder.Mu<E>, Trigger<E>>> pInitializer) {
        //Can probably simplify later
        return BehaviorBuilder.create((p_259791_) -> p_259791_.group(p_259791_.present(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN), p_259791_.absent(MemoryModuleType.ANGRY_AT), p_259791_.absent(MemoryModuleType.HUNTED_RECENTLY), p_259791_.registered(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)).apply(p_259791_, (p_259255_, p_260214_, p_259562_, p_259156_) -> (p_259918_, p_259191_, p_259772_) -> false));
    }
}
