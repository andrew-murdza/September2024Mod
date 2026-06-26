package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.enums.EnumTroll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnumTroll.class, remap = false)
public abstract class EnumTrollMixin {

    @Inject(
            method = "getWeaponForType",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void aoemod$alwaysUseAxe(
            EnumTroll troll,
            CallbackInfoReturnable<EnumTroll.Weapon> cir
    ) {
        cir.setReturnValue(EnumTroll.Weapon.AXE);
    }
}