package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.iceandfire.entity.EntitySiren;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.violetmoon.quark.content.mobs.ai.RaveGoal;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class MobAI {
    @SubscribeEvent
    public static void AIChanges(EntityJoinLevelEvent event){
        try{
            Entity entity=event.getEntity();

            if(!event.getLevel().isClientSide&& entity instanceof PathfinderMob mob) {
                if(mob.getTags().contains("aoe.checkedAI")){
                    return;
                }
                mob.addTag("aoe.checkedAI");

                //Breeding and Tempting
                if (entity instanceof Fox) {
                    addTempt(mob, Items.CHICKEN,Items.RABBIT);
                    addTempt(mob, ModTags.Items.rawFish);
                } else if (entity instanceof Rabbit) {
                    addTempt(mob, ModTags.Items.smallerFlowers);
                    addTempt(mob, Items.BEETROOT);
                    addTempt(mob, Items.WHEAT);
                    addTempt(mob, Items.POTATO);
                    addTempt(mob, ModItems.ONION.get());
                } else if (entity instanceof Horse || entity instanceof AbstractChestedHorse) {
                    addTempt(mob, Items.HAY_BLOCK);
                } else if (entity instanceof Cat||entity instanceof Ocelot) {
                    addTempt(mob, ModTags.Items.rawFish);
                }
                else if (entity instanceof PolarBear) {
//                    addBreeding(mob); handled by rideablepolarbearmod
                    addTempt(mob, Items.MUTTON, Items.BEEF);
                    addTempt(mob, ModTags.Items.rawFish);
                } else if (entity instanceof Parrot) {
                    addTempt(1, mob, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.APPLE);
                    //parrot breeding
                    addBreeding(mob);
                }
                else if (entity instanceof MushroomCow) {
                    addTempt(mob, ModTags.Items.mushrooms);
                }
                else if (entity instanceof Strider){
                    addTempt(mob,Items.CRIMSON_ROOTS,Items.WARPED_ROOTS);
                }
                else if(entity instanceof Frog) {
                    addTempt(mob, Items.SEAGRASS);
                }
                else if(entity instanceof Goat){
                    addTempt(mob, Items.BEETROOT);
                    addTempt(mob, Items.POTATO);
                    addTempt(mob, Items.CARROT);
                    addTempt(mob, ModItems.ONION.get());
                }
                else if(entity instanceof EntityMoose) {
                    addTempt(mob, ModTags.Items.smallerFlowers);
                    addTempt(mob, Items.BEETROOT);
                    addTempt(mob, Items.POTATO);
                    addTempt(mob, Items.CARROT);
                    addTempt(mob, Items.WHEAT);
                    addTempt(mob, ModItems.ONION.get());
                }
                //Protect Turtle
                if (entity instanceof Zombie) {
                    removeAI(mob, Class.forName("net.minecraft.world.entity.monster.Zombie$ZombieAttackTurtleEggGoal"));
                    removeAI(MobAI::removeTurtleAttack,mob);
                }
                else if(entity instanceof AbstractSkeleton){
                    removeAI(MobAI::removeTurtleAttack,mob);
                }
                else if (entity instanceof Rabbit) {
                    removeAI(mob,Class.forName("net.minecraft.world.entity.animal.Rabbit$RabbitAvoidEntityGoal"));
                    removeAI(mob,Class.forName("net.minecraft.world.entity.animal.Rabbit$RaidGardenGoal"));
                }

                if(mob instanceof Animal || mob instanceof WaterAnimal || mob instanceof EntitySiren){
                    //Vanilla
                    removeAI(mob, NearestAttackableTargetGoal.class, AvoidEntityGoal.class,
                            NonTameRandomTargetGoal.class, HurtByTargetGoal.class,
                            PanicGoal.class, LeapAtTargetGoal.class);
                    //Alex's Mobs
                    removeAI(mob, EntityAINearestTarget3D.class, AnimalAIFleeAdult.class,
                            AnimalAIHurtByTargetNotBaby.class, AnimalAIHerdPanic.class, AnimalAIPanicBaby.class,
                            TameableAIDestroyTurtleEggs.class,CreatureAITargetItems.class);
                    //Quark
                    removeAI(mob, RaveGoal.class);
                }
                else if(entity instanceof EntityBison){
                    removeAI(mob,
                            Class.forName("com.github.alexthe666.alexsmobs.entity.EntityBison$AIAttackNearPlayers"),
                            Class.forName("com.github.alexthe666.alexsmobs.entity.EntityBison$AIChargeFurthest"));
                }
                else if(entity instanceof Bee){
                    removeAI(p->true,mob.targetSelector);
                }
                else if(entity instanceof Wolf){
                    removeAI(mob, Class.forName("net.minecraft.world.entity.animal.Wolf$WolfPanicGoal"));
                }
                else if (entity instanceof PolarBear) {
                    removeAI(mob,Class.forName("net.minecraft.world.entity.animal.PolarBear$PolarBearAttackPlayersGoal"),
                            Class.forName("net.minecraft.world.entity.animal.PolarBear$PolarBearPanicGoal"),
                            Class.forName("net.minecraft.world.entity.animal.PolarBear$PolarBearHurtByTargetGoal"));
                }
                else if (entity instanceof Fox) {
                    removeAI(mob, Fox.FoxEatBerriesGoal.class,Fox.FoxPounceGoal.class,
                            Class.forName("net.minecraft.world.entity.animal.Fox$FoxPanicGoal"),
                            Class.forName("net.minecraft.world.entity.animal.Fox$FoxSearchForItemsGoal"),
                            Class.forName("net.minecraft.world.entity.animal.Fox$PerchAndSearchGoal"),
                            Class.forName("net.minecraft.world.entity.animal.Fox$StalkPreyGoal"));
                }
                //Make Hostile
                else if(mob instanceof ZombifiedPiglin){
                    makeHostileToPlayers(mob);
                }
            }
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private static boolean removeTurtleAttack(Goal goal) {
        Field getTargetType;
        if(goal instanceof NearestAttackableTargetGoal){
            try {
                getTargetType=NearestAttackableTargetGoal.class.getDeclaredField("targetType");
                getTargetType.setAccessible(true);
                return getTargetType.get(goal)== Turtle.class;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private static void makeHostileToPlayers(PathfinderMob mob){
        new NearestAttackableTargetGoal<>(mob, Player.class, true);
    }
    private static void addBreeding(PathfinderMob mob) {
        mob.goalSelector.addGoal(0, new BreedGoal((Animal) mob, 1.0D));
    }
    private static void addTempt(PathfinderMob mob, Item... items){
        addTempt(3,mob,items);
    }
    private static void addTempt(int priority,PathfinderMob mob, Item... items){
        mob.goalSelector.addGoal(priority, new TemptGoal(mob, 1.0D, Ingredient.of(items), false));
    }
    private static void addTempt(PathfinderMob mob, TagKey<Item> tagKey){
        mob.goalSelector.addGoal(3, new TemptGoal(mob, 1.0D, Ingredient.of(tagKey), false));
    }
    private static void removeAI(Predicate<Goal> shouldRemove, PathfinderMob mob){
        removeAI(shouldRemove,mob.goalSelector);
        removeAI(shouldRemove,mob.targetSelector);
    }
    private static void removeAI(PathfinderMob mob,Class<?>... classes){
        for(Class<?> class1:classes){
            removeAI(p->p.getClass()==class1,mob);
        }
    }
    private static void removeAI(Predicate<Goal> shouldRemove, GoalSelector goals){
        List<Goal> goalsToRemove=new ArrayList<>();
        for(WrappedGoal wrappedGoal: goals.getAvailableGoals()){
            Goal goal=wrappedGoal.getGoal();
            if(shouldRemove.test(goal)){
                goalsToRemove.add(goal);
            }
        }
        for(Goal goal: goalsToRemove){
            goals.removeGoal(goal);
        }
    }
}
