package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.iceandfire.entity.EntitySiren;
import com.starfish_studios.naturalist.common.entity.Boar;
import com.starfish_studios.naturalist.common.entity.Deer;
import com.starfish_studios.naturalist.common.entity.core.ai.goal.AlertOthersPanicGoal;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ai.PolarBearOwnerMeleeAttackGoal;
import net.amurdza.examplemod.entity.ai.PolarBearOwnerTargetGoal;
import net.amurdza.examplemod.entity.ai.PolarBearTamingAccess;
import net.amurdza.examplemod.util.AnimalFoodOverrides;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.violetmoon.quark.content.mobs.ai.RaveGoal;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class MobAI {
    private static final Set<PathfinderMob> CHECKED_MOBS =
            Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void AIChanges(EntityJoinLevelEvent event){
        try{
            Entity entity=event.getEntity();

            if(!event.getLevel().isClientSide&& entity instanceof PathfinderMob mob) {
                synchronized (CHECKED_MOBS) {
                    if (!CHECKED_MOBS.add(mob)) {
                        return;
                    }
                }

                //Breeding and Tempting
                if (entity instanceof Fox) {
                    addTempt(mob, Items.CHICKEN,Items.RABBIT, Items.SWEET_BERRIES);
                    addTempt(mob, ModTags.Items.rawFish);
                    addTempt(mob, AnimalFoodOverrides.blueberry());
                } else if (entity instanceof Rabbit) {//Other cases already covered by herbivore section
                    mob.setMaxUpStep(1.5F);
                    addTempt(mob, ModTags.Items.smallerFlowers);
                    addTempt(mob, Items.BEETROOT);
                    addTempt(mob, Items.POTATO);
                    addTempt(mob, Items.CARROT);
                    addTempt(mob, Items.WHEAT);
                    addTempt(mob, Items.SWEET_BERRIES);
                    addTempt(mob, AnimalFoodOverrides.blueberry());
                    addTempt(mob, ModTags.Items.fruits);
                    addTempt(mob, ModItems.ONION.get());
                } else if (entity instanceof Horse || entity instanceof AbstractChestedHorse || entity instanceof Llama) {
                    addTempt(mob, Items.WHEAT);
                } else if (entity instanceof Cat||entity instanceof Ocelot) {
                    addTempt(mob, ModTags.Items.rawFish);
                }
                else if (entity instanceof PolarBear) {
                    addBreeding(mob);
                    addTempt(mob, ModTags.Items.mobMeatDrops);
                    addTempt(mob, ModTags.Items.rawFish);
                } else if (entity instanceof Parrot) {
                    addTempt(1, mob, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS,
                            AnimalFoodOverrides.cabbageSeeds(), AnimalFoodOverrides.tomatoSeeds());
                    //parrot breeding
                    addTempt(mob, ModTags.Items.leafFruits);
                    addBreeding(mob);
                }
                else if (entity instanceof EntityGorilla) {
                    removeAI(goal -> goal instanceof TemptGoal, mob.goalSelector);
                    addTempt(mob, ModTags.Items.leafFruits);
                }
                else if (entity instanceof Camel){
                    removeAI(goal -> goal instanceof TemptGoal, mob.goalSelector);
                    addTempt(mob, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get());
                }
                else if (entity instanceof Deer){
                    mob.setMaxUpStep(0.6F);
                    removeAI(mob, AlertOthersPanicGoal.class);
                    addTempt(mob, Items.WHEAT);
                }
                else if (AnimalFoodOverrides.isSquirrel(entity)||entity instanceof Chicken) {
                    addTempt(mob, AnimalFoodOverrides.cabbageSeeds(), AnimalFoodOverrides.tomatoSeeds(),
                            Items.BEETROOT_SEEDS, Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS);
                }
                else if (entity instanceof MushroomCow) {
                    removeAI(goal -> goal instanceof TemptGoal, mob.goalSelector);
                    addTempt(3, mob, ModTags.Items.mushrooms);
                    addTempt(4, mob, Items.WHEAT);
                }
                else if (entity instanceof EntityWarpedToad) {
                    removeAI(goal -> goal instanceof TemptGoal, mob.goalSelector);
                    addTempt(mob, Items.WARPED_FUNGUS);
                }
                else if(entity instanceof Frog|| entity instanceof Turtle|| entity instanceof Axolotl) {
                    removeAI(goal -> goal instanceof TemptGoal, mob.goalSelector);
                    addTempt(mob, Items.SEAGRASS);
                    addTempt(mob, Items.KELP);
                }
                else if(AnimalFoodOverrides.isHerbivoreFoodTarget(entity)) {
                    removeAI(goal -> goal instanceof TemptGoal, mob.goalSelector);
                    addTempt(mob, Items.BEETROOT);
                    addTempt(mob, Items.POTATO);
                    addTempt(mob, Items.CARROT);
                    addTempt(mob, Items.WHEAT);
                    addTempt(mob, Items.SWEET_BERRIES);
                    addTempt(mob, AnimalFoodOverrides.blueberry());
                    addTempt(mob, ModTags.Items.fruits);
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

                    if (entity instanceof EntityMoose) {
                        removeAI(mob, MooseAIJostle.class, MeleeAttackGoal.class);
                    }

                    if (entity instanceof PolarBear polarBear) {
                        removeAI(goal -> true, mob.targetSelector);
                        removeAI(goal -> goal instanceof MeleeAttackGoal, mob.goalSelector);
                        mob.goalSelector.addGoal(2, new PolarBearOwnerMeleeAttackGoal(polarBear, 1.25D));
                        mob.targetSelector.addGoal(1, new PolarBearOwnerTargetGoal(polarBear, true));
                        mob.targetSelector.addGoal(2, new PolarBearOwnerTargetGoal(polarBear, false));
                        mob.setMaxUpStep(1.0F);
                        clearPolarBearAnger(polarBear);
                    }
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

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (entity instanceof Mob mob) {
            clearForbiddenBrainTargets(level, mob);
            keepStructureMobNearHome(level, mob);
        }

        if (entity instanceof PolarBear polarBear) {
            clearPolarBearAnger(polarBear);
        }

        if (entity instanceof Turtle turtle) {
            redirectPregnantTurtleHome(level, turtle);
        }
    }

    private static void keepStructureMobNearHome(ServerLevel level, Mob mob) {
        if (!StructureSpawnedMobData.isMarked(mob)
                || Math.floorMod(level.getGameTime() + mob.getId(), 20L) != 0L) {
            return;
        }

        BlockPos home = StructureSpawnedMobData.home(mob);
        if (mob instanceof PathfinderMob pathfinderMob) {
            pathfinderMob.restrictTo(home, StructureSpawnedMobData.HOME_RADIUS);
        }

        double distanceSquared = mob.distanceToSqr(
                home.getX() + 0.5D,
                home.getY(),
                home.getZ() + 0.5D
        );
        double returnDistance = StructureSpawnedMobData.HOME_RADIUS;
        if (distanceSquared <= returnDistance * returnDistance) {
            return;
        }

        double teleportDistance = returnDistance * 2.0D;
        if (distanceSquared > teleportDistance * teleportDistance) {
            mob.teleportTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
            mob.getNavigation().stop();
            return;
        }

        mob.getNavigation().moveTo(
                home.getX() + 0.5D,
                home.getY(),
                home.getZ() + 0.5D,
                1.0D
        );
    }

    private static void clearPolarBearAnger(PolarBear polarBear) {
        LivingEntity target = polarBear.getTarget();
        if (!(polarBear instanceof PolarBearTamingAccess taming)
                || !taming.aoe$canAttackOwnerTarget(target)) {
            polarBear.setTarget(null);
        }
        polarBear.setPersistentAngerTarget(null);
        polarBear.setRemainingPersistentAngerTime(0);
        polarBear.setStanding(false);
    }

    private static void clearForbiddenBrainTargets(ServerLevel level, Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target != null && isNotAllowedMobTarget(mob, target)) {
            mob.setTarget(null);
            clearSpecialAnger(mob, target);
        }

        clearLivingMemoryIfForbidden(mob, MemoryModuleType.ATTACK_TARGET);
        clearLivingMemoryIfForbidden(mob, MemoryModuleType.NEAREST_ATTACKABLE);
        clearLivingMemoryIfForbidden(mob, MemoryModuleType.HURT_BY_ENTITY);
        clearLivingMemoryIfForbidden(mob, MemoryModuleType.AVOID_TARGET);
        clearIdleZoglinCombatMemories(mob);

        Brain<?> brain = mob.getBrain();
        getMemorySafely(brain, MemoryModuleType.ANGRY_AT).ifPresent(uuid -> {
            Entity angryAt = level.getEntity(uuid);
            if (angryAt instanceof LivingEntity living && isNotAllowedMobTarget(mob, living)) {
                eraseMemorySafely(brain, MemoryModuleType.ANGRY_AT);
                clearSpecialAnger(mob, living);
            }
        });
    }

    private static <T extends LivingEntity> void clearLivingMemoryIfForbidden(Mob mob, MemoryModuleType<T> memoryType) {
        Brain<?> brain = mob.getBrain();
        getMemorySafely(brain, memoryType).ifPresent(target -> {
            if (isNotAllowedMobTarget(mob, target)) {
                eraseMemorySafely(brain, memoryType);
                clearSpecialAnger(mob, target);
            }
        });
    }

    private static void clearIdleZoglinCombatMemories(Mob mob) {
        if (!(mob instanceof Zoglin) || mob.getTarget() != null) {
            return;
        }

        Brain<?> brain = mob.getBrain();
        eraseMemorySafely(brain, MemoryModuleType.ATTACK_TARGET);
        eraseMemorySafely(brain, MemoryModuleType.NEAREST_ATTACKABLE);
        eraseMemorySafely(brain, MemoryModuleType.HURT_BY_ENTITY);
        eraseMemorySafely(brain, MemoryModuleType.ANGRY_AT);
        eraseMemorySafely(brain, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    private static void eraseMemorySafely(Brain<?> brain, MemoryModuleType<?> memoryType) {
        try {
            brain.eraseMemory(memoryType);
        } catch (IllegalStateException | NullPointerException ignored) {
        }
    }

    private static <T> Optional<T> getMemorySafely(Brain<?> brain, MemoryModuleType<T> memoryType) {
        try {
            return brain.getMemory(memoryType);
        } catch (IllegalStateException | NullPointerException ignored) {
            return Optional.empty();
        }
    }

    private static void clearSpecialAnger(Mob mob, LivingEntity target) {
        if (mob instanceof Warden warden) {
            warden.clearAnger(target);
        }
    }

    private static boolean isNotAllowedMobTarget(Mob attacker, LivingEntity target) {
        if (attacker instanceof PolarBearTamingAccess polarBear
                && polarBear.aoe$canAttackOwnerTarget(target)) {
            return false;
        }
        return isNotProtectedCombatEntity(attacker) && isNotProtectedCombatEntity(target);
    }

    private static boolean isNotProtectedCombatEntity(LivingEntity entity) {
        if (entity instanceof Player) {
            return false;
        }

        if (entity instanceof Villager) {
            return false;
        }

        if (entity instanceof Piglin) {
            return false;
        }

        if (entity instanceof PiglinBrute) {
            return false;
        }

        if (entity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            return false;
        }

        if (entity instanceof AbstractHorse abstractHorse && abstractHorse.isTamed()) {
            return false;
        }

        return true;
    }

    private static void redirectPregnantTurtleHome(ServerLevel level, Turtle turtle) {
        if (!turtle.hasEgg() || turtle.tickCount % 100 != 0) {
            return;
        }

        BlockPos nearestLand = findNearestTurtleLand(level, turtle.blockPosition(), 48);
        if (nearestLand != null) {
            turtle.setHomePos(nearestLand);
        }
    }

    private static BlockPos findNearestTurtleLand(ServerLevel level, BlockPos origin, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

                if (Math.abs(y - origin.getY()) > 24) {
                    continue;
                }

                BlockPos land = new BlockPos(x, y, z);
                if (!isValidTurtleLand(level, land)) {
                    continue;
                }

                double distance = land.distSqr(origin);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = land;
                }
            }
        }

        return best;
    }

    private static boolean isValidTurtleLand(ServerLevel level, BlockPos land) {
        BlockPos eggPos = land.above();
        return level.getBlockState(land).is(BlockTags.SAND)
                && level.isEmptyBlock(eggPos)
                && level.getFluidState(eggPos).isEmpty();
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
        mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, true));
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
        addTempt(3, mob, tagKey);
    }
    private static void addTempt(int priority, PathfinderMob mob, TagKey<Item> tagKey){
        mob.goalSelector.addGoal(priority, new TemptGoal(mob, 1.0D, Ingredient.of(tagKey), false));
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
