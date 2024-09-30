package net.amurdza.examplemod.mixins.othermods.chickensshed;

import eu.holmr.chickensshed.ChickensShed;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChickensShed.class)
public class ChickenShedRate {
    @Redirect(method = "shedItem",at= @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private static int hi(RandomSource instance1, int i, LivingEntity instance){
        if(Helper.isSpecialBiome(instance)){
            String mobName=instance.getEncodeId();
            int index= Config.SLOWER_GROWTH_MOBS.indexOf(mobName);
            int index1= Config.FASTER_GROWTH_MOBS.indexOf(mobName);
            if(index>=0&&Helper.withChance(instance.level(),Config.SLOWER_GROWTH_CHANCES.get(index))){
                return 1;
            }
            else if(index1>=0){//Helper.withChance(level,Config.FASTER_GROWTH_CHANCE.get(index1))
                return instance1.nextInt(1+ i / (1+Config.FASTER_GROWTH_AMOUNT.get(index1)));
            }
        }
        return instance1.nextInt(i);
    }
}
