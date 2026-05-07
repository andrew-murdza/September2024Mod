package net.amurdza.examplemod.mixins.no_fire_in_tropical;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class NoMobBurningInTropical {
    @Inject(method = "setSecondsOnFire", at = @At("HEAD"), cancellable = true)
    private void aoemod$noFireInTropicalBiomes(int seconds, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;

        if (entity.level().getBiome(entity.blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            entity.clearFire();
            ci.cancel();
        }
    }

    @Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
    private void aoemod$noFireTicksInTropicalBiomes(int ticks, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;

        if (ticks > 0 && entity.level().getBiome(entity.blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            entity.clearFire();
            ci.cancel();
        }
    }
}
