package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class PlayerStartsInAOEDim {

    public static final ResourceKey<Level> AOE_DIMENSION =
            ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(AOEMod.MOD_ID, "aoedim")
            );

    private static final String HAS_SPAWNED_IN_AOE_DIM = "aoemod_has_spawned_in_aoedim";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only move the player to aoedim the first time they join.
        if (player.getPersistentData().getBoolean(HAS_SPAWNED_IN_AOE_DIM)) {
            return;
        }

        ServerLevel aoeLevel = player.server.getLevel(AOE_DIMENSION);
        if (aoeLevel == null) {
            AOEMod.LOGGER.warn("Could not find dimension aoemod:aoedim");
            return;
        }

        BlockPos spawnPos = findSafeSpawnPos(aoeLevel);

        player.setRespawnPosition(
                AOE_DIMENSION,
                spawnPos,
                0.0F,
                true,
                false
        );

        player.teleportTo(
                aoeLevel,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        player.getPersistentData().putBoolean(HAS_SPAWNED_IN_AOE_DIM, true);
    }

    private static BlockPos findSafeSpawnPos(ServerLevel level) {
        BlockPos sharedSpawn = level.getSharedSpawnPos();

        int centerX = sharedSpawn.getX();
        int centerZ = sharedSpawn.getZ();

        // First try the dimension's shared spawn X/Z.
        BlockPos exact = findSurfaceSpawnAt(level, centerX, centerZ);
        if (isSafeSpawn(level, exact)) {
            return exact;
        }

        // Then scan nearby positions.
        int radius = 32;

        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Only check the edge of the current square ring.
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }

                    BlockPos pos = findSurfaceSpawnAt(level, centerX + dx, centerZ + dz);

                    if (isSafeSpawn(level, pos)) {
                        return pos;
                    }
                }
            }
        }

        // Fallback: use heightmap position even if the safety scan failed.
        return exact;
    }

    private static BlockPos findSurfaceSpawnAt(ServerLevel level, int x, int z) {
        int y = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );

        return new BlockPos(x, y, z);
    }

    private static boolean isSafeSpawn(ServerLevel level, BlockPos feetPos) {
        BlockPos floorPos = feetPos.below();
        BlockPos headPos = feetPos.above();

        BlockState floor = level.getBlockState(floorPos);
        BlockState feet = level.getBlockState(feetPos);
        BlockState head = level.getBlockState(headPos);

        if (!floor.isFaceSturdy(level, floorPos, net.minecraft.core.Direction.UP)) {
            return false;
        }

        if (!feet.isAir() || !head.isAir()) {
            return false;
        }

        // Avoid obviously bad spawn blocks.
        return !floor.is(Blocks.LAVA)
                && !floor.is(Blocks.WATER)
                && !floor.is(Blocks.CACTUS)
                && !floor.is(Blocks.MAGMA_BLOCK)
                && !floor.is(Blocks.FIRE)
                && !floor.is(Blocks.SOUL_FIRE)
                && !floor.is(Blocks.POWDER_SNOW);
    }
}