package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.CaveStructureConfig;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class UndergroundCaveSurfaceSpawnGuard {
    private static final CaveFootprint[] CAVE_FOOTPRINTS = {
            new CaveFootprint(
                    CaveStructureConfig.MUSHROOM_CAVE_TARGET_CONTINENTS,
                    CaveStructureConfig.MUSHROOM_CAVE_BIOME_WIDTH,
                    CaveStructureConfig.MUSHROOM_CAVE_X_START,
                    CaveStructureConfig.MUSHROOM_CAVE_X_SPACING,
                    CaveStructureConfig.MUSHROOM_CAVE_HORIZONTAL_RADIUS,
                    CaveStructureConfig.MUSHROOM_CAVE_CENTRAL_PILLAR_DIAMETER,
                    CaveStructureConfig.MUSHROOM_CAVE_LIQUID_RADIUS
            ),
            new CaveFootprint(
                    CaveStructureConfig.NETHER_CAVE_TARGET_CONTINENTS,
                    CaveStructureConfig.NETHER_CAVE_BIOME_WIDTH,
                    CaveStructureConfig.NETHER_CAVE_X_START,
                    CaveStructureConfig.NETHER_CAVE_X_SPACING,
                    CaveStructureConfig.NETHER_CAVE_HORIZONTAL_RADIUS,
                    CaveStructureConfig.NETHER_CAVE_CENTRAL_PILLAR_DIAMETER,
                    CaveStructureConfig.NETHER_CAVE_LIQUID_RADIUS
            )
    };
    private static final int CAVE_MIN_Y = Math.min(
            CaveStructureConfig.MUSHROOM_CAVE_END_Y,
            CaveStructureConfig.NETHER_CAVE_END_Y
    ) - 2;
    private static final int CAVE_MAX_Y = Math.max(
            CaveStructureConfig.tunnelOriginY(
                    CaveStructureConfig.MUSHROOM_CAVE_START_Y,
                    CaveStructureConfig.MUSHROOM_CAVE_VERTICAL_RADIUS
            ),
            CaveStructureConfig.tunnelOriginY(
                    CaveStructureConfig.NETHER_CAVE_START_Y,
                    CaveStructureConfig.NETHER_CAVE_VERTICAL_RADIUS
            )
    ) + 1;

    @SubscribeEvent
    public static void onSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        // The six independent dimensions no longer use the legacy spiral cave footprints.
        if (net.amurdza.examplemod.util.GameplayDimensions.isCustom(event.getLevel())) return;
        if (!isNaturalWorldSpawn(event.getSpawnType())) {
            return;
        }

        ServerLevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();

        if (event.getEntityType().getCategory() == MobCategory.CREATURE
                && isOnOuterSideOfSpiralRiver(level, pos)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (!isDeepDarkOrSoulSandValley(level, pos)) {
            return;
        }

        if (isExposedSurfacePosition(level, pos)) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean isNaturalWorldSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    private static boolean isDeepDarkOrSoulSandValley(ServerLevelAccessor level, BlockPos pos) {
        return level.getBiome(pos).is(ModTags.Biomes.deepDarkBiomes)
                || level.getBiome(pos).is(ModTags.Biomes.soulSandValleyBiomes);
    }

    private static boolean isOnOuterSideOfSpiralRiver(ServerLevelAccessor level, BlockPos pos) {
        if (pos.getY() < CAVE_MIN_Y
                || pos.getY() > CAVE_MAX_Y
                || level.canSeeSkyFromBelowWater(pos)) {
            return false;
        }

        for (CaveFootprint footprint : CAVE_FOOTPRINTS) {
            int xSpacingBlocks = footprint.xSpacing() * 16;
            long centerX = footprint.xStart()
                    + Math.round((pos.getX() - footprint.xStart()) / (double) xSpacingBlocks)
                    * xSpacingBlocks;
            int periodBlocks = footprint.biomeWidth() * 10 * 16;
            int cycle = Math.floorDiv(pos.getZ(), periodBlocks);

            for (int cycleOffset = -1; cycleOffset <= 1; cycleOffset++) {
                int cycleStartZ = (cycle + cycleOffset) * periodBlocks;
                if (isInsideFootprint(pos, centerX, cycleStartZ, footprint.targetContinents(), periodBlocks, footprint)
                        || isInsideFootprint(
                                pos,
                                centerX,
                                cycleStartZ,
                                1.0D - footprint.targetContinents(),
                                periodBlocks,
                                footprint
                        )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isInsideFootprint(
            BlockPos pos,
            long centerX,
            int cycleStartZ,
            double targetContinents,
            int periodBlocks,
            CaveFootprint footprint
    ) {
        double targetZ = cycleStartZ + targetContinents * periodBlocks;
        int centerZ = (int) Math.floor(targetZ / 16.0D) * 16;
        if (centerZ < 0) {
            return false;
        }

        double dx = pos.getX() + 0.5D - centerX;
        double dz = pos.getZ() + 0.5D - centerZ;
        double distanceSquared = dx * dx + dz * dz;
        double innerRiverBankRadius = footprint.innerRiverBankRadius();
        double caveOuterRadius = footprint.caveOuterRadius();
        return distanceSquared >= innerRiverBankRadius * innerRiverBankRadius
                && distanceSquared <= caveOuterRadius * caveOuterRadius;
    }

    private static boolean isExposedSurfacePosition(ServerLevelAccessor level, BlockPos pos) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() >= surfaceY - 1
                || level.canSeeSkyFromBelowWater(pos);
    }

    private record CaveFootprint(
            double targetContinents,
            int biomeWidth,
            int xStart,
            int xSpacing,
            double horizontalRadius,
            double centralPillarDiameter,
            double liquidRadius
    ) {
        private double innerRiverBankRadius() {
            return centralPillarDiameter * 0.5D + horizontalRadius;
        }

        private double caveOuterRadius() {
            return centralPillarDiameter * 0.5D
                    + 2.0D * (horizontalRadius + liquidRadius);
        }
    }
}
