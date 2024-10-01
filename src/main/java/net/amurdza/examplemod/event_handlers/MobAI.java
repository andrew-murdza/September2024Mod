package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.iceandfire.entity.EntityPixie;
import com.github.alexthe666.iceandfire.entity.ai.PixieAIFlee;
import com.github.alexthe666.iceandfire.entity.ai.PixieAISteal;
import com.teamabnormals.upgrade_aquatic.common.entity.animal.Lionfish;
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

            if(!event.getLevel().isClientSide&&entity instanceof PathfinderMob) {
                PathfinderMob mob = (PathfinderMob) entity;
                if(mob.getTags().contains("aoe.checkedAI")){
                    return;
                }
                mob.addTag("aoe.checkedAI");

                if(mob instanceof Animal || mob instanceof WaterAnimal){
                    removeAI(mob, NearestAttackableTargetGoal.class, AvoidEntityGoal.class,
                            NonTameRandomTargetGoal.class, HurtByTargetGoal.class,
                            PanicGoal.class, LeapAtTargetGoal.class);
                    removeAI(mob, EntityAINearestTarget3D.class, AnimalAIFleeAdult.class,
                            AnimalAIHurtByTargetNotBaby.class, AnimalAIHerdPanic.class, AnimalAIPanicBaby.class,
                            TameableAIDestroyTurtleEggs.class,CreatureAITargetItems.class,
                            Class.forName("superlord.cherry_shrimp.common.entity.CherryShrimp$ShrimpAvoidEntityGoal"),
                            Class.forName("superlord.cherry_shrimp.common.entity.CherryShrimp$ShrimpPanicGoal"));
                }

                if (entity instanceof Lionfish){
                    removeAI(mob,
                            Class.forName("com.teamabnormals.upgrade_aquatic.common.entity.animal.Lionfish$LionfishAttackGoal"));
                }
                else if(entity instanceof EntityGiantSquid){
                    removeAI(mob, Class.forName("com.github.alexthe666.alexsmobs.entity.EntityGiantSquid$AIAvoidWhales"));
                }
                else if(entity instanceof EntityGrizzlyBear){
                    removeAI(mob,
                            Class.forName("com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear$AttackPlayerGoal"));
                }
                else if(entity instanceof EntityAnteater){
                    removeAI(mob, AnteaterAIRaidNest.class,
                            Class.forName("com.github.alexthe666.alexsmobs.entity.EntityAnteater$AITargetAnts"));
                }
                else if(entity instanceof EntityBison){
                    removeAI(mob,
                            Class.forName("com.github.alexthe666.alexsmobs.entity.EntityBison$AIAttackNearPlayers"),
                            Class.forName("com.github.alexthe666.alexsmobs.entity.EntityBison$AIChargeFurthest"));
                }
                else if(entity instanceof EntityLeafcutterAnt){
                    removeAI(mob, Class.forName("com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt$AngerGoal"));
                }
                else if(entity instanceof EntityRaccoon){
                    removeAI(mob, Class.forName("com.github.alexthe666.alexsmobs.entity.EntityRaccoon$AIStealFromVillagers"));
                }
                else if(entity instanceof EntityRattlesnake){
                    removeAI(mob, Class.forName("com.github.alexthe666.alexsmobs.entity.EntityRattlesnake$ShortDistanceTarget"));
                }
                else if(entity instanceof EntityRhinoceros){
                    removeAI(mob, Class.forName("com.github.alexthe666.alexsmobs.entity.EntityRhinoceros$AIAttackNearPlayers"));
                }
                else if(entity instanceof EntityPixie){
                    removeAI(mob, PixieAIFlee.class, PixieAISteal.class);
                }


                else if (entity instanceof Fox) {
                    removeAI(mob, Fox.FoxEatBerriesGoal.class,Fox.FoxPounceGoal.class,
                            Class.forName("net.minecraft.world.entity.animal.Fox$FoxPanicGoal"),
                            Class.forName("net.minecraft.world.entity.animal.Fox$FoxSearchForItemsGoal"),
                            Class.forName("net.minecraft.world.entity.animal.Fox$PerchAndSearchGoal"),
                            Class.forName("net.minecraft.world.entity.animal.Fox$StalkPreyGoal"));
                    addTempt(mob, Items.CHICKEN,Items.RABBIT,Items.COD,Items.TROPICAL_FISH,Items.SALMON);
                } else if (entity instanceof Ocelot) {
                    addTempt(mob, Items.SALMON, Items.COD, Items.TROPICAL_FISH);
                }
                //Goat Ramming requires brain
                //Axotol hunting passive fish will be fixed with the datapack and requires brain
                //Piglin brute not attacking players with gold armor requires brain
                else if (entity instanceof Rabbit) {
                    removeAI(mob,Class.forName("net.minecraft.world.entity.animal.Rabbit$RabbitAvoidEntityGoal"));
                    removeAI(mob,Class.forName("net.minecraft.world.entity.animal.Rabbit$RaidGardenGoal"));
                    addTempt(mob, ModTags.Items.smallerFlowers);
                } else if (entity instanceof Horse || entity instanceof AbstractChestedHorse) {
                    addTempt(mob, Items.HAY_BLOCK);
                } else if (entity instanceof Cat) {
                    addTempt(mob, Items.SALMON, Items.COD, Items.TROPICAL_FISH);
                }
                else if (entity instanceof PolarBear) {
                    removeAI(mob,Class.forName("net.minecraft.world.entity.animal.PolarBear$PolarBearAttackPlayersGoal"),
                            Class.forName("net.minecraft.world.entity.animal.PolarBear$PolarBearPanicGoal"),
                            Class.forName("net.minecraft.world.entity.animal.PolarBear$PolarBearHurtByTargetGoal"));
//                    addBreeding(mob); handled by rideablepolarbearmod
                    addTempt(mob, Items.MUTTON, Items.BEEF, Items.COD, Items.SALMON);
                }
                else if (entity instanceof Parrot) {
                    addTempt(1, mob, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.APPLE);
                    //parrot breeding
                    addBreeding(0, mob);
                }
                else if (entity instanceof MushroomCow) {
                    addTempt(mob, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM);
                }
                else if (entity instanceof Strider){
                    addTempt(mob,Items.CRIMSON_ROOTS,Items.WARPED_ROOTS);
                }
                else if(entity instanceof Frog) {
                    addTempt(mob, Items.SEAGRASS);
                }
                else if (entity instanceof Zombie) {
                    removeAI(mob, Class.forName("net.minecraft.world.entity.monster.Zombie$ZombieAttackTurtleEggGoal"));
                    removeAI(MobAI::removeTurtleAttack,mob);
                }
                else if(entity instanceof AbstractSkeleton){
                    removeAI(MobAI::removeTurtleAttack,mob);
                }
                else if(entity instanceof Bee){
                    removeAI(p->true,mob.targetSelector);
                }
                else if(entity instanceof Wolf){
                    removeAI(mob, Class.forName("net.minecraft.world.entity.animal.Wolf$WolfPanicGoal"));
                }
                else if(entity instanceof EntityJerboa){
                    addTempt(mob,Items.DEAD_BUSH,Items.GRASS,Items.FERN);
                    addTempt(mob, ModTags.Items.smallerFlowers);
                }
                else if(entity instanceof EntityRoadrunner||entity instanceof EntityBlueJay||entity instanceof EntityMudskipper
                        ||entity instanceof EntityPotoo){
                    addTempt(mob, Items.COD,Items.SALMON,Items.TROPICAL_FISH);
                }
                else if(entity instanceof EntityRainFrog){
                    addTempt(mob, Items.SEAGRASS);
                }
                else if(mob instanceof EntityCapuchinMonkey){
                    addTempt(mob,AMItemRegistry.BANANA.get());
                }
                else if(mob instanceof ZombifiedPiglin|| mob instanceof EntityBunfungus || mob instanceof EntityEnderiophage){
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
        addBreeding(2,mob);
    }
    private static void addBreeding(int priority,PathfinderMob mob) {
        mob.goalSelector.addGoal(priority, new BreedGoal((Animal) mob, 1.0D));
    }
    private static void addTempt(PathfinderMob mob, Item... items){
        addTempt(3,mob,items);
    }
    private static void addTempt(int priority,PathfinderMob mob, Item... items){
        mob.goalSelector.addGoal(priority, new TemptGoal(mob, 1.0D, Ingredient.of(items), false));
    }
    private static void addTempt(int priority, PathfinderMob mob, TagKey<Item> tagKey){
        mob.goalSelector.addGoal(priority, new TemptGoal(mob, 1.0D, Ingredient.of(tagKey), false));
    }
    private static void addTempt(PathfinderMob mob, TagKey<Item> tagKey){
        addTempt(3,mob,tagKey);
    }
    private static void removeAI(Predicate<Goal> shouldRemove, PathfinderMob mob){
        removeAI(shouldRemove,mob.goalSelector);
        removeAI(shouldRemove,mob.targetSelector);
    }
    private static void removeAI(PathfinderMob mob,Class... classes){
        for(Class class1:classes){
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
