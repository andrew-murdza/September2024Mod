package net.amurdza.examplemod.mixins.block_behavior.entity_inside;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.CactusBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CactusBlock.class)
public class CactusDoesntBreakItemsOrHurtMobs {
    @Redirect(method = "entityInside",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean hi(Entity instance, DamageSource pSource, float pAmount){
        return false;
    }
}
