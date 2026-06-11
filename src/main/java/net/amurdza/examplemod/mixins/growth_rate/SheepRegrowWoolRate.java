package net.amurdza.examplemod.mixins.growth_rate;

import net.amurdza.examplemod.config.MobConfig;
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
        float multiplier = MobConfig.mobGrowthChance(this);
        int ageAmount = Helper.computeIncrements(getRandom(),multiplier);
        return Math.max(a,b - ageAmount);
    }
}
