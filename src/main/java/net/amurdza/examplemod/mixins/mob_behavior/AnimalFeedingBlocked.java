package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class AnimalFeedingBlocked {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void aoe$blockBreedingFoodWhenInfertile(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Animal animal = (Animal)(Object)this;

        if (animal.level().isClientSide) {
            return;
        }

        if (!animal.isFood(player.getItemInHand(hand))) {
            return;
        }

        if (MobConfig.mobTwinsChance(animal) <= 0.0F) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}