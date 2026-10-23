package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep vanilla conversion timing, immunity and NoAI handling; only the unsafe region changes. */
@Mixin(Hoglin.class)
public abstract class HoglinWarpedConversion {
    @Redirect(method = "isConverting", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;piglinSafe()Z"))
    private boolean aoemod$warpedForestIsUnsafe(DimensionType dimensionType) {
        Hoglin hoglin = (Hoglin) (Object) this;
        if (GameplayDimensions.NETHER.equals(hoglin.level().dimension())) {
            return !GameplayDimensions.biomeIs(hoglin.level(), hoglin.blockPosition(), "warped_forest");
        }
        return dimensionType.piglinSafe();
    }
}
