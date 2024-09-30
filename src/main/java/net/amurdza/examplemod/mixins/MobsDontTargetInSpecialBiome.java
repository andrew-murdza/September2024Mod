package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(NearestAttackableTargetGoal.class)
public class MobsDontTargetInSpecialBiome {
    @Shadow @Nullable protected LivingEntity target;

    @Redirect(method = "canUse", at= @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ai/goal/target/NearestAttackableTargetGoal;target:Lnet/minecraft/world/entity/LivingEntity;"))
    private LivingEntity hi(NearestAttackableTargetGoal instance){
        return target==null || Helper.isSpecialBiome(target)?null:target;
    }
}
