package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import com.github.alexthe666.alexsmobs.entity.EntityRoadrunner;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityRoadrunner.class,remap = false)
public class RoadRunnerShedRate {

    @Shadow public int timeUntilNextFeather;

    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityRoadrunner;isAlive()Z"))
    private boolean hi(EntityRoadrunner instance){
        if(Helper.isSpecialBiome(instance)){
            String mobName=instance.getEncodeId();
            int index= Config.SLOWER_GROWTH_MOBS.indexOf(mobName);
            int index1= Config.FASTER_GROWTH_MOBS.indexOf(mobName);
            if(index>=0&& Helper.withChance(instance.level(),Config.SLOWER_GROWTH_CHANCES.get(index))){
                return false;
            }
            else if(index1>=0&&instance.isAlive()){
                timeUntilNextFeather-=Config.FASTER_GROWTH_AMOUNT.get(index1);
            }
        }
        return instance.isAlive();
    }
}
