package net.amurdza.examplemod.mixins.mob_behavior;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AgeableMob.class)
public class AnimalAgingRate {
    @WrapOperation(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/AgeableMob;isAlive()Z"
            )
    )
    private boolean aoe$changeNaturalAgingRate(AgeableMob mob, Operation<Boolean> original) {
        if (!mob.isAlive()) {
            return false;
        }

        float multiplier = MobConfig.mobGrowthChance(mob);
        int ageAmount = Helper.computeIncrements(mob.getRandom(),multiplier);

        int age = mob.getAge();
        if(Math.abs(age)<=ageAmount){
            mob.setAge(0);
        }
        else if (age<0) {
            mob.setAge(age+ageAmount);
        }
        else if(age>0){
            mob.setAge(age-ageAmount);
        }
        return false;
    }
}
