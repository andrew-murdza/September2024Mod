package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Shulker.class)
public abstract class ShulkerCommonBehavior {
    @Shadow
    @Final
    private static UUID COVERED_ARMOR_MODIFIER_UUID;

    @Shadow
    @Final
    @Mutable
    private static AttributeModifier COVERED_ARMOR_MODIFIER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void aoemod$useCommonMobClosedArmor(CallbackInfo ci) {
        COVERED_ARMOR_MODIFIER = new AttributeModifier(
                COVERED_ARMOR_MODIFIER_UUID,
                "Covered armor bonus",
                4.0D,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Inject(method = "hitByShulkerBullet", at = @At("HEAD"), cancellable = true, require = 0)
    private void aoemod$noShulkerBulletDuplication(CallbackInfo ci) {
        ci.cancel();
    }
}
