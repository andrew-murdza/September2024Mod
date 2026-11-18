package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/** Keeps native camps inhabited when their randomly chosen Orcz roster was removed. */
public final class OrczCampResidents {
    private static final String[] RESIDENTS = {"orcshield", "orcsavagemace"};
    private static final BlockPos[] POSITIONS = {new BlockPos(8, 3, 24), new BlockPos(17, 3, 21)};

    private OrczCampResidents() { }

    public static void ensureSelectedResidents(ServerLevel level, BlockPos marker) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        BlockPos origin = marker.offset(-17, -1, -21);
        AABB bounds = new AABB(origin.below(2), origin.offset(34, 20, 43));
        for (int i = 0; i < RESIDENTS.length; i++) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("orcz", RESIDENTS[i]));
            if (type == null || !level.getEntitiesOfClass(Mob.class, bounds, mob -> mob.getType() == type).isEmpty()
                    || !(type.create(level) instanceof Mob mob)) continue;
            BlockPos pos = findSupportedPosition(level, mob, origin, origin.offset(POSITIONS[i]));
            if (pos == null) continue;
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
            StructureSpawnedMobData.mark(mob, pos);
            level.addFreshEntityWithPassengers(mob);
        }
    }

    private static BlockPos findSupportedPosition(ServerLevel level, Mob mob, BlockPos origin, BlockPos preferred) {
        // Desert and snow templates have a ground layer one block higher than the normal camp.
        // The restored native boss can occupy the preferred guard position.
        // Search the whole authored courtyard, still bounded inside the camp.
        for (int radius = 0; radius <= 32; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy = 0; dy <= 3; dy++) {
                        BlockPos pos = preferred.offset(dx, dy, dz);
                        if (pos.getX() <= origin.getX() || pos.getX() >= origin.getX() + 33
                                || pos.getZ() <= origin.getZ() || pos.getZ() >= origin.getZ() + 41) continue;
                        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                                || !level.getFluidState(pos).isEmpty()) continue;
                        mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                        if (level.noCollision(mob)) return pos;
                    }
                }
            }
        }
        return null;
    }
}
