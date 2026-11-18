package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Eight distinct shore groups; the legacy tags keep existing saved residents readable. */
public final class TropicalMoundFauna {
    private static final List<String> SPECIES = List.of("biodiverse:capybara", "minecraft:frog");
    private static final int GROUPS_PER_SPECIES = 4;
    private static final String MOUND_TAG = "aoemod:mound_fauna";
    private static final String ORIGIN_TAG = "aoemod:mound_chunk:";
    private TropicalMoundFauna() { }

    public static boolean isMoundAnimal(Entity entity) {
        return entity.getTags().contains(MOUND_TAG)
                || entity.getPersistentData().getBoolean(MOUND_TAG);
    }

    public static boolean belongsToMound(Entity entity, long chunk) {
        return entity.getTags().contains(ORIGIN_TAG + chunk)
                || entity.getPersistentData().getBoolean(MOUND_TAG)
                && entity.getPersistentData().getLong("aoemod:mound_fauna_chunk") == chunk;
    }

    public static String species(TropicalLayout layout, int chunkX, int chunkZ) {
        var mounds = TropicalIslandPlan.forChunk(layout, chunkX, chunkZ).mounds();
        for (int i = 0; i < Math.min(SPECIES.size() * GROUPS_PER_SPECIES, mounds.size()); i++) {
            var mound = mounds.get(i);
            if (mound.x() == chunkX && mound.z() == chunkZ) return SPECIES.get(i / GROUPS_PER_SPECIES);
        }
        return null;
    }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context, TropicalLayout layout) {
        int chunkX = Math.floorDiv(context.origin().getX(), 16), chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        String id = species(layout, chunkX, chunkZ);
        if (id == null) return false;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        if (type == null) return false;
        WorldGenLevel level = context.level();
        List<AABB> occupied = new ArrayList<>();
        // Try the former pair positions first, then other cells on this dry
        // shore. Native mangrove roots can now share the chunk, so one blocked
        // corner must not erase a resident. This only runs during decoration;
        // swimming away never replenishes the group through natural spawning.
        List<int[]> candidates = new ArrayList<>();
        candidates.add(new int[]{6, 6});
        candidates.add(new int[]{10, 10});
        for (int x = 2; x <= 13; x++) for (int z = 2; z <= 13; z++) {
            if (x == 6 && z == 6 || x == 10 && z == 10) continue;
            candidates.add(new int[]{x, z});
        }
        for (int[] offset : candidates) {
            if (occupied.size() == 2) break;
            BlockPos pos = TropicalVegetation.findGround(level, chunkX * 16 + offset[0], chunkZ * 16 + offset[1]);
            if (pos == null || !level.getFluidState(pos).isEmpty()
                    || !net.amurdza.examplemod.util.GameplayDimensions.biomeIs(level, pos, "mangrove_swamp")) continue;
            if (!(type.create(level.getLevel()) instanceof Mob mob)) continue;
            mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, context.random().nextFloat() * 360, 0);
            ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos),
                    MobSpawnType.CHUNK_GENERATION, null, null);
            if (mob instanceof net.minecraft.world.entity.AgeableMob ageable) ageable.setAge(0);
            DocumentedNaturalSpawns.maximizeAquaticSize(mob);
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !SpawnCollisionSafety.canInspect(level, mob.getBoundingBox())
                    || !level.noCollision(mob, mob.getBoundingBox())
                    || occupied.stream().anyMatch(box -> box.intersects(mob.getBoundingBox()))) continue;
            mob.setPersistenceRequired();
            mob.getPersistentData().putBoolean("aoemod:documented_natural_spawn", true);
            mob.getPersistentData().putBoolean("aoemod:mound_fauna", true);
            long origin = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
            mob.getPersistentData().putLong("aoemod:mound_fauna_chunk", origin);
            // Preserve provenance through the protochunk/entity-storage round
            // trip independently of mods' Forge persistent-data handling.
            mob.addTag(MOUND_TAG);
            mob.addTag(ORIGIN_TAG + origin);
            level.addFreshEntityWithPassengers(mob);
            occupied.add(mob.getBoundingBox());
        }
        return !occupied.isEmpty();
    }
}
