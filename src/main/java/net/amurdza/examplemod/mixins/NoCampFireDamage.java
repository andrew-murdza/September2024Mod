package net.amurdza.examplemod.mixins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.CampfireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(CampfireBlock.class)
public class NoCampFireDamage {
    @Redirect(method = "entityInside",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;hasFrostWalker(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    public boolean canSurvive(LivingEntity entity) {
        return true;
    }
}
