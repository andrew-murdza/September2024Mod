package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityAlligatorSnappingTurtle.class)
public class SnappingTurtleNotDefensive {
    @Redirect(method = "tick",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean hi(LivingEntity instance, DamageSource entity, float ev){
        return false;
    }
}
