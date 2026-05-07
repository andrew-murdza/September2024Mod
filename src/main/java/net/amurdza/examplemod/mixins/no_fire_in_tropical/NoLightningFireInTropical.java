package net.amurdza.examplemod.mixins.no_fire_in_tropical;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class NoLightningFireInTropical {

    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void aoemod$noLightningFireInTropical(int extraIgnitions, CallbackInfo ci) {
        LightningBolt self = (LightningBolt)(Object)this;

        if (self.level().getBiome(self.blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            ci.cancel();
        }
    }
}