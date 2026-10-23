package net.amurdza.examplemod.mixins.othermods.iceandfire;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Locale;

/** ClassLoader paths have no leading slash; the packaged dragon models are lowercase. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.client.model.util.TabulaModelHandlerHelper", remap = false)
public abstract class DragonEyeModelPaths {
    @ModifyArg(method = "loadTabulaModel", at = @At(value = "INVOKE",
            target = "Ljava/lang/ClassLoader;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"),
            index = 0, require = 1)
    private static String aoe$resolvePackagedModel(String path) {
        String relative = path.startsWith("/") ? path.substring(1) : path;
        return relative.startsWith("assets/iceandfire/models/tabula/")
                ? relative.toLowerCase(Locale.ROOT) : relative;
    }
}
