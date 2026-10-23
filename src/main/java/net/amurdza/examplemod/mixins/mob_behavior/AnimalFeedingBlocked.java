package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.future.FutureMobCompatibility;
import net.amurdza.examplemod.util.AnimalFoodOverrides;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

        ItemStack stack = player.getItemInHand(hand);

        if (AnimalFoodOverrides.hasConfiguredFoodOverride(animal)
                && animal.isFood(stack)
                && !AnimalFoodOverrides.isConfiguredFood(animal, stack)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (!animal.isFood(stack)) {
            return;
        }

        boolean blocked = animal.isBaby()
                ? MobConfig.mobGrowthChance(animal) == 0.0F || FutureMobCompatibility.isAgeLocked(animal)
                : MobConfig.mobTwinsChance(animal) == 0.0F;
        if (blocked) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
