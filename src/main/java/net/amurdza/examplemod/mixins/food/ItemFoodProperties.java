package net.amurdza.examplemod.mixins.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(Item.class)
public abstract class ItemFoodProperties {
    @Unique
    private static Map<Item, FoodProperties> aoe$foodOverrides;

    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void aoe$getFoodProperties(CallbackInfoReturnable<FoodProperties> cir) {
        FoodProperties properties = aoe$getFoodOverrides().get((Item) (Object) this);

        if (properties != null) {
            cir.setReturnValue(properties);
        }
    }

    @Inject(method = "isEdible", at = @At("HEAD"), cancellable = true)
    private void aoe$isEdible(CallbackInfoReturnable<Boolean> cir) {
        if (aoe$getFoodOverrides().containsKey((Item) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static Map<Item, FoodProperties> aoe$getFoodOverrides() {
        if (aoe$foodOverrides == null) {
            aoe$foodOverrides = new IdentityHashMap<>();
            aoe$registerFoodOverrides(aoe$foodOverrides);
        }

        return aoe$foodOverrides;
    }

    @Unique
    private static void aoe$registerFoodOverrides(Map<Item, FoodProperties> map) {
        /*
         * Make popped chorus fruit edible.
         */

        aoe$add(map, Items.POPPED_CHORUS_FRUIT, aoe$food(3, 0.3F, false));
        aoe$add(map, Items.CHORUS_FRUIT, aoe$food(1, 0.2F, false,false,true));

        /*
         * Placeholder vanilla food values.
         * These mostly match vanilla-style values, but they are here so you can easily edit them later.
         *
         * Warning: for foods with special effects, these placeholders only represent hunger/saturation.
         * Golden apples, rotten flesh, spider eyes, poisonous potatoes, etc. may need effect handling
         * if you want exact vanilla behavior after overriding them.
         */
    }

    @Unique
    private static void aoe$add(Map<Item, FoodProperties> map, Item item, FoodProperties properties) {
        if (item != null && properties != null) {
            map.put(item, properties);
        }
    }

    @Unique
    private static FoodProperties aoe$food(int nutrition, float saturation, boolean meat) {
        return aoe$food(nutrition, saturation, meat, false, false);
    }

    @Unique
    private static FoodProperties aoe$food(
            int nutrition,
            float saturation,
            boolean meat,
            boolean fast,
            boolean alwaysEat
    ) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturation);

        if (meat) {
            builder.meat();
        }

        if (fast) {
            builder.fast();
        }

        if (alwaysEat) {
            builder.alwaysEat();
        }

        return builder.build();
    }
}