package net.amurdza.examplemod.mixins.othermods.wildernature;

import net.minecraft.resources.ResourceLocation;
import net.satisfy.wildernature.client.render.entity.FlamingoRenderer;
import net.satisfy.wildernature.entity.FlamingoEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The source renderer selects colors from transient entity IDs, not a saved variant. */
@Mixin(value = FlamingoRenderer.class, remap = false)
public abstract class FlamingoColors {
    @Inject(method = "getTextureLocation(Lnet/satisfy/wildernature/entity/FlamingoEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$whiteOrPink(FlamingoEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        String color = (entity.getUUID().getLeastSignificantBits() & 1) == 0 ? "white" : "pink";
        cir.setReturnValue(new ResourceLocation("wildernature", "textures/entity/flamingo_" + color + ".png"));
    }
}
