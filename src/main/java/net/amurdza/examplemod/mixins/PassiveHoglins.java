package net.amurdza.examplemod.mixins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HoglinAi.class)
public class PassiveHoglins {
    @Redirect(method = "findNearestValidAttackTarget",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/entity/monster/hoglin/HoglinAi;isPacified(Lnet/minecraft/world/entity/monster/hoglin/Hoglin;)Z"))
    private static boolean hi(Hoglin pHoglin){
        return true;
    }
    @Redirect(method = "maybeRetaliate",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/entity/ai/sensing/Sensor;isEntityAttackable(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private static boolean hi1(LivingEntity pAttacker, LivingEntity pTarget){
        return false;
    }
}
