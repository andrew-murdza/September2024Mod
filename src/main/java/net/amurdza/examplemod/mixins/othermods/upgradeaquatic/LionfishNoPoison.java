package net.amurdza.examplemod.mixins.othermods.upgradeaquatic;

import com.teamabnormals.upgrade_aquatic.common.entity.animal.Lionfish;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Lionfish.class)
public class LionfishNoPoison {
    @Redirect(method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"))
    private boolean cancelPoison(LivingEntity instance){
        return false;
    }
}
