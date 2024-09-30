package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Chicken.class)
public class ChickenEggRate {
    @Shadow public int eggTime;

    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Chicken;isAlive()Z"))
    private boolean hi(Chicken instance){
        if(Helper.isSpecialBiome(instance)){
            String mobName=instance.getEncodeId();
            int index= Config.SLOWER_GROWTH_MOBS.indexOf(mobName);
            int index1= Config.FASTER_GROWTH_MOBS.indexOf(mobName);
            if(index>=0&& Helper.withChance(instance.level(),Config.SLOWER_GROWTH_CHANCES.get(index))){
                return false;
            }
            else if(index1>=0&&instance.isAlive()){//Helper.withChance(level,Config.FASTER_GROWTH_CHANCE.get(index1))
                eggTime-=Config.FASTER_GROWTH_AMOUNT.get(index1);
            }
        }
        return instance.isAlive();
    }
}
