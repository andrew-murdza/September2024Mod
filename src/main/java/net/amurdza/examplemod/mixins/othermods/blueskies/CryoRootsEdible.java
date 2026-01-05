package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.registries.SkiesFoods;
import com.legacy.blue_skies.registries.SkiesItems;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SkiesItems.class)
public class CryoRootsEdible {
    @ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/Item$Properties;food(Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/Item$Properties;"
            )
    )
    private static FoodProperties aoemod$replaceCryoRootFood(FoodProperties original) {
        if (original == SkiesFoods.CRYO_ROOT) {
            return Foods.BEETROOT;
        }
        return original;
    }
}
