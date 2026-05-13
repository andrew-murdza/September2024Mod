package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.client.renders.entities.passive.CharscaleMokiRenderer;
import com.legacy.blue_skies.entities.passive.fish.CharscaleMokiEntity;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CharscaleMokiRenderer.class)
public class CharscaleMokiInLava {
    @Redirect(
            method = "setupRotations*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/legacy/blue_skies/entities/passive/fish/CharscaleMokiEntity;isInWater()Z"
            ),
            remap = false
    )
    private boolean aoemod$treatLavaAsWaterForRender(CharscaleMokiEntity entity) {
        return LavaMobs.isLavaMob(entity) ? entity.isInLava() || entity.isInWater() : entity.isInWater();
    }
}
