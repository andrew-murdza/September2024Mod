package net.amurdza.examplemod.mixins.othermods.snowrealmagic;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Util.class)
public class SnowEntityDatafixerFallback {
    @Inject(method = "fetchChoiceType", at = @At("HEAD"), cancellable = true)
    private static void aoemod$skipBadSnowRealMagicEntityDatafixer(DSL.TypeReference type, String choiceName, CallbackInfoReturnable<Type<?>> cir) {
        if ("snowrealmagic.snow".equals(choiceName) || "minecraft:snowrealmagic.snow".equals(choiceName)) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(
            method = "doFetchChoiceType",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/datafixers/schemas/Schema;getChoiceType(Lcom/mojang/datafixers/DSL$TypeReference;Ljava/lang/String;)Lcom/mojang/datafixers/types/Type;"
            )
    )
    private static Type<?> aoemod$allowMissingModdedEntityDatafixers(Schema schema, DSL.TypeReference type, String choiceName) {
        try {
            return schema.getChoiceType(type, choiceName);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("Data fixer not registered for:")) {
                return null;
            }

            throw exception;
        }
    }
}
