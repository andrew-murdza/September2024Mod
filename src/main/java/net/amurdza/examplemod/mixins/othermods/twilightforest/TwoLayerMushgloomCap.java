package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.world.components.feature.BigMushgloomFeature;

/** Keep Twilight's native cap geometry and texture, always using both cap layers. */
@Mixin(BigMushgloomFeature.class)
public abstract class TwoLayerMushgloomCap {
    @Redirect(method = "makeCap", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextBoolean()Z"))
    private boolean aoemod$alwaysUseBothCapLayers(RandomSource random) {
        random.nextBoolean();
        return false;
    }
}
