package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.function.Function;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BreedingItems {
    @SubscribeEvent
    public static void extraOrCancelBreeding(PlayerInteractEvent.EntityInteract event){
        if(event.getTarget() instanceof Mob mob){
            BlockPos pos=mob.getOnPos();
            if(!Helper.isOkMob(event.getLevel(),pos,mob)){
                event.setCanceled(true);
            }
            else {
                Player player=event.getEntity();
                ItemStack stack=event.getItemStack();
                if(mob instanceof MushroomCow){
                    useFood(mob, player, stack, ModTags.Items.mushrooms);
                }
                else if(mob instanceof Rabbit&&!stack.is(Items.DANDELION)){
                    useFood(mob,player,stack, ModTags.Items.smallerFlowers);
                }
                else if(mob instanceof Horse || mob instanceof Donkey){
                    useFood(mob,player,stack,Items.HAY_BLOCK);
                }
                else if(mob instanceof Strider){
                    useFood(mob,player,stack, Items.CRIMSON_ROOTS,Items.WARPED_ROOTS);
                }
                else if(mob instanceof Frog){
                    useFood(mob,player,stack,Items.SEAGRASS);
                }
                else if(mob instanceof Ocelot ||mob instanceof Cat){
                    useFood(mob, player, stack, ModTags.Items.rawFish);
                }
                else if(mob instanceof Fox){
                    useFood(mob,player,stack, Items.CHICKEN,Items.RABBIT);
                    useFood(mob, player, stack, ModTags.Items.rawFish);
                }
                else if(mob instanceof Parrot){
                    useFood(mob,player,stack,Items.APPLE);
                }
                else if(mob instanceof PolarBear){
                    useFood(mob,player,stack, Items.BEEF,Items.MUTTON);
                    useFood(mob, player, stack, ModTags.Items.rawFish);
                }
                else if(mob instanceof EntityMoose&&!stack.is(Items.DANDELION)){
                    useFood(mob,player,stack,ModTags.Items.smallerFlowers);
                }
                else if(mob instanceof EntityJerboa){
                    useFood(mob,player,stack, Items.DEAD_BUSH,Items.GRASS,Items.FERN);
                    useFood(mob,player,stack,ModTags.Items.smallerFlowers);
                }
                else if(mob instanceof EntityRoadrunner ||mob instanceof EntityBlueJay ||mob instanceof EntityMudskipper
                        ||mob instanceof EntityPotoo){
                    useFood(mob, player, stack, ModTags.Items.rawFish);
                }
                else if(mob instanceof EntityRainFrog){
                    useFood(mob,  player,stack, Items.SEAGRASS);
                }
                else if(mob instanceof EntityBananaSlug&&!stack.is(Items.BROWN_MUSHROOM)){
                    useFood(mob, player, stack, ModTags.Items.mushrooms);
                }
                else if(mob instanceof EntityKangaroo){
                    useFood(mob,  player,stack, Items.FERN);
                }
                else if(mob instanceof EntityCapuchinMonkey){
                    useFood(mob,  player,stack, AMItemRegistry.BANANA.get());
                }
            }
        }
    }
    private static void useFood(Entity entity, Player player, ItemStack stack, Item... items){
        useFood(entity,player,stack,p-> Arrays.asList(items).contains(p));
    }
    private static void useFood(Entity entity, Player player, ItemStack stack, TagKey<Item> itemTag){
        useFoodStack(entity,player,stack,p->p.is(itemTag));
    }
    private static void useFood(Entity entity, Player player, ItemStack stack, Item item){
        useFood(entity,player,stack,p-> p==item);
    }
    private static void useFoodStack(Entity entity, Player player, ItemStack stack, Function<ItemStack,Boolean> func){
        if(entity instanceof Animal animal &&func.apply(stack)){
            if(!animal.level().isClientSide&&animal.getAge()==0&&animal.canFallInLove()){
                if(!(animal instanceof TamableAnimal)||((TamableAnimal)animal).isTame()){
                    if(!(animal instanceof AbstractHorse)||((AbstractHorse)animal).isTamed()){
                        useItem(player,stack);
                        animal.setInLove(player);
                        animal.gameEvent(GameEvent.ENTITY_INTERACT,animal);
                        return;
                    }
                }
            }
            if(animal.isBaby()){
                useItem(player,stack);
                animal.ageUp((int)((float)(-animal.getAge() / 20) * 0.1F), true);
                animal.gameEvent(GameEvent.ENTITY_INTERACT,animal);
            }
        }
    }
    private static void useFood(Entity entity, Player player, ItemStack stack, Function<Item,Boolean> func){
        useFoodStack(entity,player,stack,t->func.apply(t.getItem()));
    }
    private static void useItem(Player player, ItemStack stack){
        if(!player.getAbilities().instabuild){
            stack.shrink(1);
        }
    }

}
