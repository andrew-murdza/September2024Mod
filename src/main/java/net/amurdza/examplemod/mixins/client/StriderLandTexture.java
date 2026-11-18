package net.amurdza.examplemod.mixins.client;

import net.minecraft.client.renderer.entity.StriderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Strider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the cold land palette independent of the suppressed shiver and movement penalty. */
@Mixin(StriderRenderer.class)
public abstract class StriderLandTexture {
    private static final ResourceLocation AOEMOD_COLD = new ResourceLocation("textures/entity/strider/strider_cold.png");
    private static final ResourceLocation AOEMOD_WARM = new ResourceLocation("textures/entity/strider/strider.png");

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/monster/Strider;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$landPalette(Strider strider, CallbackInfoReturnable<ResourceLocation> result) {
        result.setReturnValue(strider.isInLava() ? AOEMOD_WARM : AOEMOD_COLD);
    }
}
