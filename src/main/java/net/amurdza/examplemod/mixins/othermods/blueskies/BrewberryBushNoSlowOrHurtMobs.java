package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.blocks.natural.BrewberryBushBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrewberryBushBlock.class)
public class BrewberryBushNoSlowOrHurtMobs {
    @Redirect(method = "entityInside",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getType()Lnet/minecraft/world/entity/EntityType;"))
    private EntityType<?> cancelSlow(Entity instance){
        return EntityType.FOX;
    }
}
