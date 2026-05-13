package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.client.renders.entities.passive.HorizofinTunidRenderer;
import com.legacy.blue_skies.entities.passive.fish.HorizofinTunidEntity;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HorizofinTunidRenderer.class)
public class HorizofinTunidInLava {
    @Redirect(
            method = "setupRotations*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/legacy/blue_skies/entities/passive/fish/HorizofinTunidEntity;isInWater()Z"
            ),
            remap = false
    )
    private boolean aoemod$treatLavaAsWaterForRender(HorizofinTunidEntity entity) {
        return LavaMobs.isLavaMob(entity) ? entity.isInLava() || entity.isInWater() : entity.isInWater();
    }
}
