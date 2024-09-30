package net.amurdza.examplemod.mixins;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.goat.GoatAi;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
@Mixin(GoatAi.class)
public class GoatNoJumpOrRam {
    @Redirect(method = "initLongJumpActivity",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/entity/ai/Brain;addActivityWithConditions(Lnet/minecraft/world/entity/schedule/Activity;Lcom/google/common/collect/ImmutableList;Ljava/util/Set;)V"))
    private static <E>void hi(Brain instance, Activity pActivity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> pTasks, Set<Pair<MemoryModuleType<?>, MemoryStatus>> pMemoryStatuses){

    }
    @Redirect(method = "initRamActivity",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/entity/ai/Brain;addActivityWithConditions(Lnet/minecraft/world/entity/schedule/Activity;Lcom/google/common/collect/ImmutableList;Ljava/util/Set;)V"))
    private static<E> void hi1(Brain instance, Activity pActivity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> pTasks, Set<Pair<MemoryModuleType<?>, MemoryStatus>> pMemoryStatuses){

    }
}
