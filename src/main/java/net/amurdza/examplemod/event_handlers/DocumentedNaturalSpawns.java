package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.amurdza.examplemod.util.TropicalUmbrellaVariants;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.amurdza.examplemod.worldgen.AquaticSpawnDistanceRules;
import net.amurdza.examplemod.worldgen.SpawnCollisionSafety;
import net.amurdza.examplemod.worldgen.TropicalSpawnSurfaces;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import dev.hybridlabs.aquatic.entity.crustacean.FiddlerCrabEntity;
import net.amurdza.examplemod.mixins.othermods.hybridaquatic.MaximumAquaticSize;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.FrogVariant;
import net.mcreator.biodiverse.entity.CassowaryEntity;
import net.mcreator.biodiverse.ai.goal.RestingGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class DocumentedNaturalSpawns {
    private DocumentedNaturalSpawns() { }

    public static boolean natural(MobSpawnType reason) {
        return reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!natural(event.getSpawnType()) || !GameplayDimensions.isCustom(event.getLevel())) return;
        if (net.amurdza.examplemod.worldgen.SavannaCampSpawnRules.blocked(event.getLevel(), event.getPos(), event.getEntityType())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (!RetainedContent.isKeptEntity(event.getEntityType())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (TropicalSpawnSurfaces.blocked(event.getLevel(), event.getPos())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        var species = DocumentedFauna.find(event.getLevel(), event.getPos(), event.getEntityType());
        if (species != null) {
            event.setResult(DocumentedFauna.hasSpace(event.getLevel(), event.getPos(), species) ? Event.Result.ALLOW : Event.Result.DENY);
        } else {
            event.setResult(DocumentedMonsters.canSpawn(event.getLevel(), event.getPos(), event.getEntityType())
                    ? Event.Result.ALLOW : Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void position(MobSpawnEvent.PositionCheck event) {
        if (!natural(event.getSpawnType()) || !GameplayDimensions.isCustom(event.getLevel())) return;
        if (net.amurdza.examplemod.worldgen.SavannaCampSpawnRules.blocked(event.getLevel(),
                BlockPos.containing(event.getX(), event.getY(), event.getZ()), event.getEntity())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (!RetainedContent.isKeptEntity(event.getEntity().getType())
                || excludedNaturalVariant(event.getLevel(), event.getEntity())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (TropicalSpawnSurfaces.blocked(event.getLevel(), pos)) {
            event.setResult(Event.Result.DENY);
            return;
        }
        var species = DocumentedFauna.find(event.getLevel(), pos, event.getEntity().getType());
        if (species == null) {
            boolean allowed = DocumentedMonsters.canSpawn(event.getLevel(), pos, event.getEntity().getType())
                    && SpawnCollisionSafety.canInspect(event.getLevel(), event.getEntity().getBoundingBox())
                    && SpawnCollisionSafety.hasNoPowderCane(event.getLevel(), event.getEntity().getBoundingBox())
                    && event.getLevel().noCollision(event.getEntity());
            event.setResult(allowed ? Event.Result.ALLOW : Event.Result.DENY);
            return;
        }
        // Turtles live indefinitely. Limit each stretch of coast even when the global water cap
        // has reopened after players move away, and allow the remaining slots to go to other species.
        if (event.getEntity() instanceof net.minecraft.world.entity.animal.Turtle
                && event.getSpawnType() == MobSpawnType.NATURAL
                && GameplayDimensions.is(event.getLevel(), GameplayDimensions.TROPICAL)
                && event.getLevel().getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Turtle.class,
                        event.getEntity().getBoundingBox().inflate(96, 32, 96)).size() >= 2) {
            event.setResult(Event.Result.DENY);
            return;
        }
        maximizeAquaticSize(event.getEntity());
        BlockPos ground = DocumentedFauna.groundedSpawnPos(event.getLevel(), pos, species);
        var bounds = event.getEntity().getBoundingBox().move(0, ground.getY() - pos.getY(), 0);
        // Retain collision safety while replacing source-mod sea-level/biome/brightness assumptions.
        boolean allowed = SpawnCollisionSafety.canInspect(event.getLevel(), bounds)
                && SpawnCollisionSafety.hasNoPowderCane(event.getLevel(), bounds)
                && DocumentedFauna.hasSpace(event.getLevel(), pos, species)
                && event.getLevel().noCollision(event.getEntity(), bounds);
        if (allowed && event.getSpawnType() == MobSpawnType.NATURAL && !ground.equals(pos)) {
            // NaturalSpawner already checked the original random Y. Moving a bottom
            // animal must not bypass its player/world-spawn exclusion or distance cap.
            allowed = groundedNaturalPositionAllowed(event.getLevel(), event.getEntity(),
                    event.getX(), ground.getY(), event.getZ());
        }
        if (allowed && !ground.equals(pos)) event.getEntity().setPos(event.getX(), ground.getY(), event.getZ());
        event.setResult(allowed ? Event.Result.ALLOW : Event.Result.DENY);
    }

    private static boolean groundedNaturalPositionAllowed(ServerLevelAccessor level, Mob mob, double x, double y, double z) {
        var player = level.getNearestPlayer(x, y, z, -1, false);
        if (player == null) return false;
        double distance = player.distanceToSqr(x, y, z);
        if (distance <= 24 * 24 || level.getLevel().getSharedSpawnPos().closerToCenterThan(new Vec3(x, y, z), 24)) return false;
        int maximumDistance = AquaticSpawnDistanceRules.category(level, mob.getType()).getDespawnDistance();
        return distance <= maximumDistance * maximumDistance
                || mob.getType().canSpawnFarFromPlayer() && !mob.removeWhenFarAway(distance);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void packSize(net.minecraftforge.event.entity.living.LivingPackSizeEvent event) {
        var species = DocumentedFauna.find(event.getEntity().level(), event.getEntity().blockPosition(), event.getEntity().getType());
        if (species != null && DocumentedFauna.isWaterSpecies(species.type())) {
            event.setMaxPackSize(DocumentedFauna.groupSize(species).maximum);
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Custom spawners such as PhantomSpawner do not run the ordinary
        // placement checks. Cancel the spawn itself, not just initialization.
        boolean campSpawn = natural(event.getSpawnType()) || switch (event.getSpawnType()) {
            case SPAWNER, REINFORCEMENT, EVENT, JOCKEY -> event.getEntity().getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
            default -> false;
        };
        if (campSpawn && net.amurdza.examplemod.worldgen.SavannaCampSpawnRules.blocked(event.getLevel(),
                BlockPos.containing(event.getX(), event.getY(), event.getZ()), event.getEntity())) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
            return;
        }
        if (event.getEntity().getType() == net.minecraft.world.entity.EntityType.CHICKEN
                && event.getSpawnType() == MobSpawnType.JOCKEY
                && GameplayDimensions.is(event.getLevel(), GameplayDimensions.SAVANNA)) {
            event.getEntity().getPersistentData().putBoolean("aoemod:excluded_savanna_jockey", true);
        }
        if (event.getEntity() instanceof BaseWaterAnimal) {
            event.getEntity().getPersistentData().putBoolean("aoemod:maximize_spawn_size", true);
        }
        if (natural(event.getSpawnType()) && GameplayDimensions.isCustom(event.getLevel())) {
            event.getEntity().getPersistentData().putBoolean("aoemod:documented_natural_spawn", true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finishedSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (GameplayDimensions.isCustom(event.getLevel()) && entity instanceof blueduck.outer_end.entity.Stalker stalker) {
            // Also normalizes saved residents, eggs and offspring, not just the initial natural spawn.
            stalker.setColor("cobalt");
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SAVANNA)
                && entity instanceof net.minecraft.world.entity.animal.Chicken chicken
                && (chicken.isChickenJockey() || entity.getPersistentData().getBoolean("aoemod:excluded_savanna_jockey"))) {
            entity.ejectPassengers();
            event.setCanceled(true);
            return;
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SNOWY)) {
            if (entity instanceof com.github.alexthe666.alexsmobs.entity.EntityLobster lobster) {
                lobster.setVariant(0);
            } else if (entity instanceof net.minecraft.world.entity.animal.Fox fox
                    && GameplayDimensions.biomeIs(event.getLevel(), entity.blockPosition(), "snowy_forest")) {
                fox.setVariant(net.minecraft.world.entity.animal.Fox.Type.SNOW);
            }
        }
        if (entity instanceof CassowaryEntity cassowary) {
            cassowary.goalSelector.getAvailableGoals().removeIf(goal -> goal.getGoal() instanceof RestingGoal);
            cassowary.setResting(false);
        }
        // Worldgen entities pass through chunk NBT before joining, while HAPI breeding
        // and creature-net releases can bypass FinalizeSpawn entirely.
        boolean generated = entity.getPersistentData().getBoolean("aoemod:documented_natural_spawn");
        if (generated && (!RetainedContent.isKeptEntity(entity.getType())
                || excludedNaturalVariant(event.getLevel(), entity))) {
            event.setCanceled(true);
            return;
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.TROPICAL)
                || !event.loadedFromDisk() || entity.getPersistentData().getBoolean("aoemod:maximize_spawn_size")) {
            maximizeAquaticSize(entity);
            entity.getPersistentData().remove("aoemod:maximize_spawn_size");
        }
        if ((!event.loadedFromDisk() || generated) && GameplayDimensions.is(event.getLevel(), GameplayDimensions.TROPICAL)
                && entity instanceof Frog frog) frog.setVariant(FrogVariant.COLD);
        if (!entity.getPersistentData().getBoolean("aoemod:documented_natural_spawn")) return;
        entity.getPersistentData().remove("aoemod:documented_natural_spawn");
        if (entity instanceof Rabbit rabbit && GameplayDimensions.is(event.getLevel(), GameplayDimensions.SNOWY)
                && GameplayDimensions.biomeIs(event.getLevel(), rabbit.blockPosition(), "snowy_forest")
                && rabbit.getVariant() != Rabbit.Variant.WHITE && rabbit.getVariant() != Rabbit.Variant.WHITE_SPLOTCHED) {
            // A pack can cross the forest boundary and carry the first rabbit's warm coat with it.
            rabbit.setVariant(rabbit.getRandom().nextInt(100) < 80 ? Rabbit.Variant.WHITE : Rabbit.Variant.WHITE_SPLOTCHED);
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SNOWY) && entity instanceof MushroomCow mooshroom) {
            mooshroom.setVariant(mooshroom.getRandom().nextBoolean()
                    ? MushroomCow.MushroomType.RED : MushroomCow.MushroomType.BROWN);
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SAVANNA) && event.getEntity() instanceof Sheep sheep) {
            sheep.setColor(DyeColor.BROWN);
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SNOWY)
                && GameplayDimensions.biomeIs(event.getLevel(), entity.blockPosition(), "dappled_forest")
                && event.getEntity() instanceof net.minecraft.world.entity.animal.Cow cow
                && cow.getType() == net.minecraft.world.entity.EntityType.COW) {
            ColdCowYaks.makeCold(cow);
        }

    }

    public static void maximizeAquaticSize(Entity entity) {
        if (entity instanceof BaseWaterAnimal aquatic && entity instanceof MaximumAquaticSize
                && !AquaticMobSizes.usesNativeSize(aquatic)) {
            aquatic.setSize(AquaticMobSizes.maximum(aquatic));
            aquatic.refreshDimensions();
        }
        if (GameplayDimensions.is(entity.level(), GameplayDimensions.TROPICAL)) {
            if (entity instanceof com.github.alexthe666.alexsmobs.entity.EntityTriops triops) {
                triops.setTriopsScale(1.1F);
                triops.refreshDimensions();
            } else if (entity instanceof com.teamabnormals.upgrade_aquatic.common.entity.animal.jellyfish.ColoredSizableJellyfish jellyfish) {
                jellyfish.setSize(jellyfish.getNaturalSizeMap().lastKey(), false);
                jellyfish.refreshDimensions();
            } else if (entity instanceof net.amurdza.examplemod.entity.EndFishEntity
                    || entity instanceof net.amurdza.examplemod.entity.CubozoaEntity) {
                // Retained fish restore their size metadata without refreshing the
                // cached bounding box. The tropical getter now supplies the maximum.
                entity.refreshDimensions();
            }
        }
    }

    public static boolean excludedNaturalVariant(net.minecraft.world.level.LevelReader level, Entity entity) {
        if (entity instanceof FiddlerCrabEntity && GameplayDimensions.is(level, GameplayDimensions.DESERT)
                && GameplayDimensions.biomeIs(level, entity.blockPosition(), "desert_lake")) {
            // Hybrid Aquatic draws blue, purple or red from the entity's UUID on the client.
            return kotlin.random.RandomKt.Random(entity.getUUID().getLeastSignificantBits()).nextInt(3) != 2;
        }
        return TropicalUmbrellaVariants.excludedNaturalVariant(entity);
    }
}
