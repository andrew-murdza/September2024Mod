package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Sheep.class)
public abstract class SheepRegrowWoolRate extends LivingEntity {
    protected SheepRegrowWoolRate(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Redirect(method="aiStep",at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private int hi(int a, int b){
        if(Helper.isSpecialBiome(this)){
            String mobName=getEncodeId();
            int index= Config.SLOWER_GROWTH_MOBS.indexOf(mobName);
            int index1= Config.FASTER_GROWTH_MOBS.indexOf(mobName);
            if(index>=0&& Helper.withChance(level(),Config.SLOWER_GROWTH_CHANCES.get(index))){
                return Math.max(a,b+1);
            }
            else if(index1>=0){//Helper.withChance(level,Config.FASTER_GROWTH_CHANCE.get(index1))
                return Math.max(a,b-Config.FASTER_GROWTH_AMOUNT.get(index1));
            }
        }
        return Math.max(a,b);
    }
}
