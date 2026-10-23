package net.amurdza.examplemod.mixins.othermods.backport;

import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Completes the health changes accompanying the backport's doubled tame-wolf health. */
@Mixin(Wolf.class)
public abstract class ArmoredPawsWolfHealth {
    @ModifyArg(method = "mobInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Wolf;heal(F)V"), index = 0)
    private float aoemod$doubleFoodHealing(float healing) {
        return healing * 2F;
    }

    @ModifyConstant(method = "hurt", constant = @Constant(floatValue = 1F))
    private float aoemod$removeOldDamageOffset(float value) {
        return 0F;
    }

    @ModifyConstant(method = "hurt", constant = @Constant(floatValue = 2F))
    private float aoemod$removeOldDamageHalving(float value) {
        return 1F;
    }
}
