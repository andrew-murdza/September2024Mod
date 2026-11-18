package net.amurdza.examplemod.mixins.client;

import net.amurdza.examplemod.client.SnowyGrassColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class SnowyForestGrassTint {
    @Inject(method = "calculateBlockTint", at = @At("HEAD"), cancellable = true)
    private void aoemod$forestGrass(BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> result) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (SnowyGrassColors.unblended(level))
            result.setReturnValue(resolver.getColor(level.getBiome(pos).value(), pos.getX(), pos.getZ()));
    }
}
