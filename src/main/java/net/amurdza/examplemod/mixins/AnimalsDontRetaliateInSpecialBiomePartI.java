package net.amurdza.examplemod.mixins;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class AnimalsDontRetaliateInSpecialBiomePartI {
    @Redirect(method = "hurt", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z",ordinal = 5))
    protected boolean hi(DamageSource instance, TagKey<DamageType> pDamageTypeKey){
        return instance.is(pDamageTypeKey);
    }
}
