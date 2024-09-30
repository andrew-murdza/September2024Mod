package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityEmu.class)
public class EmuEggRate {
    @Shadow public int timeUntilNextEgg;

    @Redirect(method = "tick",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityEmu;isAlive()Z"))
    private boolean hi(EntityEmu instance){
        if(Helper.isSpecialBiome(instance)){
            String mobName=instance.getEncodeId();
            int index= Config.SLOWER_GROWTH_MOBS.indexOf(mobName);
            int index1= Config.FASTER_GROWTH_MOBS.indexOf(mobName);
            if(index>=0&& Helper.withChance(instance.level(),Config.SLOWER_GROWTH_CHANCES.get(index))){
                return false;
            }
            else if(index1>=0&&instance.isAlive()){//Helper.withChance(level,Config.FASTER_GROWTH_CHANCE.get(index1))
                timeUntilNextEgg-=Config.FASTER_GROWTH_AMOUNT.get(index1);
            }
        }
        return instance.isAlive();
    }
}
