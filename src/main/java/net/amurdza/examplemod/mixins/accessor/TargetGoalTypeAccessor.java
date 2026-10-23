package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NearestAttackableTargetGoal.class)
public interface TargetGoalTypeAccessor {
    @Accessor("targetType")
    Class<? extends LivingEntity> aoemod$getTargetType();
}
