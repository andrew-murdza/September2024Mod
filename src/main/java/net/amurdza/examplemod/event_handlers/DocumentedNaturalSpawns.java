package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
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
        var species = DocumentedFauna.find(event.getLevel(), event.getPos(), event.getEntityType());
        if (species != null) {
            event.setResult(DocumentedFauna.hasSpace(event.getLevel(), event.getPos(), species) ? Event.Result.ALLOW : Event.Result.DENY);
        } else if (event.getEntityType().getCategory() != MobCategory.MONSTER && event.getEntityType().getCategory() != MobCategory.MISC) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void position(MobSpawnEvent.PositionCheck event) {
        if (!natural(event.getSpawnType()) || !GameplayDimensions.isCustom(event.getLevel())) return;
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        var species = DocumentedFauna.find(event.getLevel(), pos, event.getEntity().getType());
        if (species == null) return;
        // Retain collision safety while replacing source-mod sea-level/biome/brightness assumptions.
        boolean allowed = DocumentedFauna.hasSpace(event.getLevel(), pos, species)
                && event.getLevel().noCollision(event.getEntity(), event.getEntity().getBoundingBox());
        event.setResult(allowed ? Event.Result.ALLOW : Event.Result.DENY);
    }

    @SubscribeEvent
    public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (natural(event.getSpawnType()) && GameplayDimensions.isCustom(event.getLevel())) {
            event.getEntity().getPersistentData().putBoolean("aoemod:documented_natural_spawn", true);
        }
    }

    @SubscribeEvent
    public static void finishedSpawn(EntityJoinLevelEvent event) {
        if (!event.getEntity().getPersistentData().getBoolean("aoemod:documented_natural_spawn")) return;
        event.getEntity().getPersistentData().remove("aoemod:documented_natural_spawn");
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SAVANNA) && event.getEntity() instanceof Sheep sheep) {
            sheep.setColor(DyeColor.BROWN);
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SNOWY)
                && event.getEntity() instanceof net.minecraft.world.entity.animal.Cow cow
                && cow.getType() == net.minecraft.world.entity.EntityType.COW) {
            ColdCowYaks.makeCold(cow);
        }
        if (GameplayDimensions.is(event.getLevel(), GameplayDimensions.SNOWY)
                && event.getEntity() instanceof org.betterx.betterend.entity.CubozoaEntity jellyfish) {
            CompoundTag state = new CompoundTag();
            jellyfish.addAdditionalSaveData(state);
            state.putByte("Variant", (byte) 0);
            jellyfish.readAdditionalSaveData(state);
        }
    }
}
