package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.amurdza.examplemod.event_handlers.GoblinFortressProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.entity.projectile.FallingIce;

/** The native falling-ice attack must not consume the fortress's permanent ice ceiling. */
@Mixin(AlphaYeti.class)
public abstract class GoblinFortressYetiTerrain {
    @Inject(method = "makeBlockFall", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$keepFortressCeiling(BlockPos pos, int hangTime, CallbackInfo callback) {
        var yeti = (AlphaYeti) (Object) this;
        if (!(yeti.level() instanceof ServerLevel level) || GoblinFortressProtection.boundaryAt(level, pos) == null) return;
        var ice = new FallingIce(level, pos.getX() + .5, pos.getY(), pos.getZ() + .5,
                level.getBlockState(pos), hangTime);
        // Start wholly beneath the retained ceiling, with the native warning
        // delay, gravity and damage. Never put the projectile inside masonry.
        ice.setPos(ice.getX(), pos.getY() - ice.getBbHeight() - .01, ice.getZ());
        ice.xo = ice.getX(); ice.yo = ice.getY(); ice.zo = ice.getZ();
        if (level.noCollision(ice)) level.addFreshEntity(ice);
        callback.cancel();
    }
}
