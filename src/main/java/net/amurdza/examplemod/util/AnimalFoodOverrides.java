package net.amurdza.examplemod.util;

import com.github.alexthe666.alexsmobs.entity.EntityBananaSlug;
import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import com.github.alexthe666.alexsmobs.entity.EntityMoose;
import com.github.alexthe666.alexsmobs.entity.EntityWarpedToad;
import com.starfish_studios.naturalist.common.entity.Boar;
import com.starfish_studios.naturalist.common.entity.Deer;
import com.starfish_studios.naturalist.core.registry.NaturalistEntityTypes;
import dev.xkmc.fruitsdelight.init.food.FruitType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import samebutdifferent.ecologics.entity.Squirrel;
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

        if (hasConfiguredFoodOverride(entity)) {
            return isConfiguredFood(entity, stack);
        }

        if (entity instanceof Rabbit) {
            return isHerbivoreFood(stack);
        }

        if (entity instanceof Fox) {
            return isBlueberry(stack);
        }

        if (entity instanceof Parrot) {
            return stack.is(ModTags.Items.leafFruits);
        }

        if (entity instanceof Chicken || entity instanceof Squirrel) {
            return isSeed(stack);
        }

        return false;
    }

    public static boolean hasConfiguredFoodOverride(Entity entity) {
        return entity instanceof EntityGorilla
                || entity instanceof Camel
                || entity instanceof EntityWarpedToad
                || entity instanceof Frog
                || entity instanceof Turtle
                || entity instanceof Axolotl
                || entity instanceof Llama
                || isClearedHerbivoreFoodTarget(entity);
    }

    public static boolean isConfiguredFood(Entity entity, ItemStack stack) {
        if (entity instanceof EntityGorilla) {
            return stack.is(ModTags.Items.leafFruits);
        }

        if (entity instanceof Camel) {
            return stack.is(samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get());
        }

        if (entity instanceof EntityWarpedToad) {
            return stack.is(Items.WARPED_FUNGUS);
        }

        if (entity instanceof Frog || entity instanceof Turtle || entity instanceof Axolotl) {
            return stack.is(Items.SEAGRASS) || stack.is(Items.KELP);
        }

        if (entity instanceof Llama) {
            return stack.is(Items.WHEAT);
        }

        if (isClearedHerbivoreFoodTarget(entity)) {
            return isHerbivoreFood(stack);
        }

        return false;
    }

    public static boolean isHerbivoreFoodTarget(Entity entity) {
        return entity instanceof Goat
                || entity instanceof EntityMoose
                || entity instanceof Pig
                || entity instanceof Rabbit
                || entity instanceof Deer
                || entity instanceof Boar;
    }

    private static boolean isClearedHerbivoreFoodTarget(Entity entity) {
        return isHerbivoreFoodTarget(entity) && !(entity instanceof Rabbit);
    }

    public static boolean isHerbivoreFood(ItemStack stack) {
        return stack.is(Items.WHEAT)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.POTATO)
                || stack.is(Items.CARROT)
                || stack.is(Items.BEETROOT)
                || stack.is(vectorwing.farmersdelight.common.registry.ModItems.ONION.get())
                || isBlueberry(stack)
                || stack.is(ModTags.Items.fruits);
    }

    public static boolean isBlueberry(ItemStack stack) {
        return stack.is(blueberry());
    }

    public static boolean isSeed(ItemStack stack) {
        return stack.is(cabbageSeeds())
                || stack.is(tomatoSeeds())
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS);
    }
}
