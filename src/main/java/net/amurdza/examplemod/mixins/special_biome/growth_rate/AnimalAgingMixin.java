package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AgeableMob.class)
public abstract class AnimalAgingMixin {
    @Shadow public abstract int getAge();

    @Shadow protected int age;

    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;isAlive()Z"))
    private boolean hi(AgeableMob ageableMob){
        String mobName= ageableMob.getEncodeId();
        if(mobName!=null){
            int index= Config.SLOWER_GROWTH_MOBS.indexOf(mobName);
            int index1= Config.FASTER_GROWTH_MOBS.indexOf(mobName);
            if(ageableMob.isAlive()&&Helper.isSpecialBiome(ageableMob)){
                if(index>=0&& Helper.withChance(ageableMob.level(),Config.SLOWER_GROWTH_CHANCES.get(index))){
                    return false;
                }
                else if(index1>=0&&ageableMob.isAlive()){//Helper.withChance(level,Config.FASTER_GROWTH_CHANCE.get(index1))
                    int i=getAge();
                    int delta=-Math.min(Config.FASTER_GROWTH_AMOUNT.get(index1),Math.abs(i))*(int)Math.signum(i);
                    ageableMob.setAge(i+delta);
                }
            }
        }
        return ageableMob.isAlive();
    }
}
