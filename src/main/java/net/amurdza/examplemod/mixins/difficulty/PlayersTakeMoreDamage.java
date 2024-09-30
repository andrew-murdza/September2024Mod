package net.amurdza.examplemod.mixins.difficulty;

import net.amurdza.examplemod.Helper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayersTakeMoreDamage extends LivingEntity{

    @Shadow public int experienceLevel;

    protected PlayersTakeMoreDamage(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Redirect(method = "hurt",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean hi(LivingEntity instance, DamageSource source, float ev){
        float f= (float) (ev*Math.pow(1.05,-1+Helper.getDiffLevel(this)-0.9*experienceLevel));
        return instance.hurt(source,f);
    }
}
