package net.amurdza.examplemod.mixins.othermods.trailsandtalesplus;

import com.belgieyt.trailsandtalesplus.Objects.utils.TTItemProperties;
import com.belgieyt.trailsandtalesplus.TrailsandTalesPlusClient;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TrailsandTalesPlusClient.class, remap = false)
public abstract class TrailsAndTalesPlusItemPropertiesThreadSafe {

    @Redirect(
            method = "onClientSetup",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/belgieyt/trailsandtalesplus/Objects/utils/TTItemProperties;RegisterProperies()V"
            )
    )
    private static void aoemod$enqueueItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(TTItemProperties::RegisterProperies);
    }
}