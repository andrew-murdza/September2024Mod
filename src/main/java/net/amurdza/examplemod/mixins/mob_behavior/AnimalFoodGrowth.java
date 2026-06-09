package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Animal.class)
public abstract class AnimalFoodGrowth extends AgeableMob{
    protected AnimalFoodGrowth(EntityType<? extends AgeableMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void ageUp(int amount, boolean forced) {
        int i = this.getAge();
        float multiplier = MobConfig.mobGrowthChance(this);
        i += amount * Mth.ceil(20*multiplier);
        if (i > 0) {
            i = 0;
        }

        this.setAge(i);
        if (forced) {
            if (this.forcedAgeTimer == 0) {
                this.forcedAgeTimer = 40;
            }
        }
    }
}