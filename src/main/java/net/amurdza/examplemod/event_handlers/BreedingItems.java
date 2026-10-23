package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import com.github.alexthe666.alexsmobs.entity.EntityWarpedToad;
import net.amurdza.examplemod.util.AnimalFoodOverrides;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.future.FutureMobCompatibility;
import net.minecraft.world.InteractionResult;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.Arrays;
import java.util.function.Function;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BreedingItems {
    @SubscribeEvent
    public static void extraOrCancelBreeding(PlayerInteractEvent.EntityInteract event){
        if(event.getTarget() instanceof Mob mob){
            Player player=event.getEntity();
            ItemStack stack=event.getItemStack();
            boolean blocked = mob instanceof Animal animal && isFeedingBlocked(animal);
            if(useFoodStack(mob, player, stack, foodStack -> AnimalFoodOverrides.isExtraFood(mob, foodStack))){
                finishInteraction(event, mob, blocked);
                return;
            }
            boolean handled = false;
            if(mob instanceof MushroomCow){
                handled = handled || useFood(mob, player, stack, ModTags.Items.mushrooms);
            }
            else if(mob instanceof Rabbit&&!stack.is(Items.DANDELION)){
                handled = handled || useFood(mob,player,stack, ModTags.Items.smallerFlowers);
                handled = handled || useFood(mob,player,stack,Items.BEETROOT);
                handled = handled || useFood(mob,player,stack,Items.WHEAT);
                handled = handled || useFood(mob,player,stack,Items.POTATO);
                handled = handled || useFood(mob, player, stack, ModItems.ONION.get());
            }
            else if(mob instanceof Horse || mob instanceof Donkey || mob instanceof Llama){
                handled = handled || useFood(mob,player,stack,Items.WHEAT);
            }
            else if(mob instanceof Camel){
                handled = handled || useFood(mob, player, stack, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get());
            }
            else if(mob instanceof EntityWarpedToad){
                handled = handled || useFood(mob, player, stack, Items.WARPED_FUNGUS);
            }
            else if(mob instanceof Frog || mob instanceof Turtle || mob instanceof Axolotl){
                handled = handled || useFood(mob,player,stack,Items.SEAGRASS);
                handled = handled || useFood(mob,player,stack,Items.KELP);
            }
            else if(mob instanceof Goat){
                handled = handled || useFood(mob,player,stack,Items.BEETROOT);
                handled = handled || useFood(mob,player,stack,Items.CARROT);
                handled = handled || useFood(mob,player,stack,Items.POTATO);
                handled = handled || useFood(mob, player, stack, ModItems.ONION.get());
            }
            else if((mob instanceof Ocelot ||mob instanceof Cat)&&!stack.is(Items.COD)&&!stack.is(Items.SALMON)){
                handled = handled || useFood(mob, player, stack, ModTags.Items.rawFish);
            }
            else if(mob instanceof Fox){
                handled = handled || useFood(mob,player,stack, Items.CHICKEN,Items.RABBIT);
                handled = handled || useFood(mob, player, stack, ModTags.Items.rawFish);
            }
            else if(mob instanceof Parrot){
                handled = handled || useFood(mob,player,stack, ModTags.Items.leafFruits);
            }
            else if(mob instanceof EntityGorilla){
                handled = handled || useFood(mob, player, stack, ModTags.Items.leafFruits);
            }
            if (handled) finishInteraction(event, mob, blocked);
        }
    }

    private static boolean isFeedingBlocked(Animal animal) {
        return animal.isBaby()
                ? MobConfig.mobGrowthChance(animal) == 0.0F || FutureMobCompatibility.isAgeLocked(animal)
                : MobConfig.mobTwinsChance(animal) == 0.0F;
    }

    private static void finishInteraction(PlayerInteractEvent.EntityInteract event, Mob mob, boolean blocked) {
        event.setCanceled(true);
        event.setCancellationResult(blocked ? InteractionResult.FAIL
                : InteractionResult.sidedSuccess(mob.level().isClientSide));
    }
    private static boolean useFood(Entity entity, Player player, ItemStack stack, Item... items){
        return useFood(entity,player,stack,p-> Arrays.asList(items).contains(p));
    }
    private static boolean useFood(Entity entity, Player player, ItemStack stack, TagKey<Item> itemTag){
        return useFoodStack(entity,player,stack,p->p.is(itemTag));
    }
    private static boolean useFood(Entity entity, Player player, ItemStack stack, Item item){
        return useFood(entity,player,stack,p-> p==item);
    }
    private static boolean useFoodStack(Entity entity, Player player, ItemStack stack, Function<ItemStack,Boolean> func) {
        if (!(entity instanceof Animal animal) || !func.apply(stack)) return false;
        if (animal.isBaby()) {
            if (MobConfig.mobGrowthChance(animal) == 0.0F || FutureMobCompatibility.isAgeLocked(animal)) return true;
            if (!animal.level().isClientSide) {
                useItem(player, stack);
                animal.ageUp((int) ((float) (-animal.getAge() / 20) * 0.1F), true);
                animal.gameEvent(GameEvent.ENTITY_INTERACT, animal);
            }
            return true;
        }
        if (animal.getAge() == 0 && animal.canFallInLove()
                && (!(animal instanceof TamableAnimal tameable) || tameable.isTame())
                && (!(animal instanceof AbstractHorse horse) || horse.isTamed())) {
            if (MobConfig.mobTwinsChance(animal) == 0.0F) return true;
            if (!animal.level().isClientSide) {
                useItem(player, stack);
                animal.setInLove(player);
                animal.gameEvent(GameEvent.ENTITY_INTERACT, animal);
            }
            return true;
        }
        return false;
    }
    private static boolean useFood(Entity entity, Player player, ItemStack stack, Function<Item,Boolean> func){
        return useFoodStack(entity,player,stack,t->func.apply(t.getItem()));
    }
    private static void useItem(Player player, ItemStack stack){
        if(!player.getAbilities().instabuild){
            stack.shrink(1);
        }
    }

}
