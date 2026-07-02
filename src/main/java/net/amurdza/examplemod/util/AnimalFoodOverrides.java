package net.amurdza.examplemod.util;

import com.github.alexthe666.alexsmobs.entity.EntityBananaSlug;
import com.github.alexthe666.alexsmobs.entity.EntityMoose;
import com.github.alexthe666.alexsmobs.entity.EntityWarpedToad;
import com.starfish_studios.naturalist.core.registry.NaturalistEntityTypes;
import dev.xkmc.fruitsdelight.init.food.FruitType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import samebutdifferent.ecologics.registry.ModEntityTypes;

public final class AnimalFoodOverrides {
    private AnimalFoodOverrides() {
    }

    public static Item blueberry() {
        return FruitType.BLUEBERRY.getFruit();
    }

    public static Item cabbageSeeds() {
        return vectorwing.farmersdelight.common.registry.ModItems.CABBAGE_SEEDS.get();
    }

    public static Item tomatoSeeds() {
        return vectorwing.farmersdelight.common.registry.ModItems.TOMATO_SEEDS.get();
    }

    public static Item ashenWheatSeeds() {
        return net.amurdza.examplemod.registry.ModItems.ASHEN_WHEAT_SEEDS.get();
    }

    public static boolean isBoar(Entity entity) {
        return entity.getType() == NaturalistEntityTypes.BOAR.get();
    }

    public static boolean isSquirrel(Entity entity) {
        return entity.getType() == ModEntityTypes.SQUIRREL.get();
    }

    public static boolean isExtraFood(Entity entity, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (entity instanceof EntityWarpedToad) {
            return stack.is(Items.WARPED_FUNGUS);
        }

        if (entity instanceof EntityBananaSlug) {
            return stack.is(Items.SEAGRASS) || stack.is(Items.KELP);
        }

        if (isHerbivoreFoodTarget(entity)) {
            return isHerbivoreFood(stack);
        }

        if (entity instanceof Fox) {
            return isBlueberry(stack);
        }

        if (entity instanceof Frog) {
            return stack.is(Items.KELP);
        }

        if (entity instanceof Parrot || entity instanceof Chicken) {
            return isParrotChickenSeed(stack);
        }

        if (isSquirrel(entity)) {
            return isSquirrelSeed(stack);
        }

        return false;
    }

    public static boolean isHerbivoreFoodTarget(Entity entity) {
        return entity instanceof Goat
                || entity instanceof EntityMoose
                || entity instanceof Pig
                || entity instanceof Rabbit
                || isBoar(entity);
    }

    public static boolean isHerbivoreFood(ItemStack stack) {
        return stack.is(Items.WHEAT)
                || stack.is(net.amurdza.examplemod.registry.ModItems.ASHEN_WHEAT.get())
                || stack.is(Items.SWEET_BERRIES)
                || isBlueberry(stack);
    }

    public static boolean isBlueberry(ItemStack stack) {
        return stack.is(blueberry());
    }

    public static boolean isParrotChickenSeed(ItemStack stack) {
        return stack.is(cabbageSeeds())
                || stack.is(tomatoSeeds())
                || stack.is(ashenWheatSeeds());
    }

    public static boolean isSquirrelSeed(ItemStack stack) {
        return isParrotChickenSeed(stack)
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS);
    }
}
