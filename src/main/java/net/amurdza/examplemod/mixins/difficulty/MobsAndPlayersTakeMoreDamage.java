package net.amurdza.examplemod.mixins.difficulty;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class MobsAndPlayersTakeMoreDamage extends Entity{
    public MobsAndPlayersTakeMoreDamage(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    @ModifyVariable(method = "hurt",at=@At(value = "LOAD"),ordinal = 1)
    public float hi(float f, DamageSource source, float pAmount){
        if(Helper.isFromAnimal(this)||Helper.isFromAnimal(source)){
            return f;
        }
        float damageDecreaser=Helper.isFromPlayer(this)? Config.PLAYER_DEFENSE_FACTOR:Config.MONSTER_DEFENSE_FACTOR;
        float damageIncreaser=Helper.isFromPlayer(source)? Config.PLAYER_ATTACK_FACTOR:Config.MONSTER_ATTACK_FACTOR;
        int levelAttacked=Helper.getLevel(this);
        int levelAttacking=Helper.getLevel(source,this);
        return (float) (f*Math.pow(damageIncreaser,levelAttacking)/Math.pow(damageDecreaser,levelAttacked));
    }
}
