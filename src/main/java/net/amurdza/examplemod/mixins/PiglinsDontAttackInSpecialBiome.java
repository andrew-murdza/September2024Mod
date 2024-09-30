package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PiglinSpecificSensor.class)
public class PiglinsDontAttackInSpecialBiome {
    @Redirect(method = "doTick", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;isWearingGold(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean hi(LivingEntity item){
        return PiglinAi.isWearingGold(item) || Helper.isSpecialBiome(item);
    }
}
