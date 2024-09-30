package net.amurdza.examplemod.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(Entity.class)
public class DayTimeDoesntHurtMobs {
    @Redirect(method = "getLightLevelDependentMagicValue",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasChunkAt(II)Z"))
    boolean hi(Level instance, int i, int j){
        return false;
    }
}
