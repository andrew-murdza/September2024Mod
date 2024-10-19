package net.amurdza.examplemod.mixins.block_behavior.entity_inside;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(SweetBerryBushBlock.class)
public class SweetBerriesDontSlowOrHurtMobs {
    @Redirect(method = "entityInside",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getType()Lnet/minecraft/world/entity/EntityType;"))
    private EntityType<?> cancelSlow(Entity instance){
        return EntityType.FOX;
    }
}
