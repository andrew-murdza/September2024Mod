package net.amurdza.examplemod.mixins.othermods.forest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.power_umc.forestxreborn.init.ForestModBiomes;

import java.util.Iterator;

@Mixin(ForestModBiomes.class)
public class PreventForestBiomes {
    @Redirect(method = "onServerAboutToStart",at= @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z"),remap = false)
    private static boolean hi(Iterator instance){
        return false;
    }
}
