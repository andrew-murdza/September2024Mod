package net.amurdza.examplemod.mixins.othermods.biodiverse;

import net.mcreator.biodiverse.entity.CapybaraEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapybaraEntity.class)
public abstract class CapybaraWithoutSerenity {
    @Redirect(method = "baseTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean aoemod$omitSerenityAura(LivingEntity target, MobEffectInstance effect) {
        return false;
    }
}
