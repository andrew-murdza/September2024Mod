package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.config.CaveStructureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class MushroomCaveRegion {
    private static final double ORIGIN_Y = CaveStructureConfig.tunnelOriginY(
            CaveStructureConfig.MUSHROOM_CAVE_START_Y,
            CaveStructureConfig.MUSHROOM_CAVE_VERTICAL_RADIUS
    );
    private static final double HORIZONTAL_RADIUS = CaveStructureConfig.MUSHROOM_CAVE_HORIZONTAL_RADIUS;
    private static final double VERTICAL_RADIUS = CaveStructureConfig.MUSHROOM_CAVE_VERTICAL_RADIUS;
    private static final double CENTRAL_PILLAR_DIAMETER =
            CaveStructureConfig.MUSHROOM_CAVE_CENTRAL_PILLAR_DIAMETER;
    private static final double MIN_FLOOR_THICKNESS =
            CaveStructureConfig.MUSHROOM_CAVE_MIN_FLOOR_THICKNESS;
    private static final double LIQUID_DEPTH = CaveStructureConfig.MUSHROOM_CAVE_LIQUID_DEPTH;
    private static final double LIQUID_RADIUS = CaveStructureConfig.MUSHROOM_CAVE_LIQUID_RADIUS;
    private static final double MUSHROOM_SECTION_START_TURNS =
            CaveStructureConfig.MUSHROOM_CAVE_SECTION_START_TURNS;
    private static final double MUSHROOM_SECTION_END_TURNS =
            CaveStructureConfig.MUSHROOM_CAVE_SECTION_END_TURNS;

    private static final int BIOME_WIDTH_BLOCKS = CaveStructureConfig.MUSHROOM_CAVE_BIOME_WIDTH * 16;
    private static final int PERIOD_BLOCKS = BIOME_WIDTH_BLOCKS * 10;
    private static final double TARGET_CONTINENTS = CaveStructureConfig.MUSHROOM_CAVE_TARGET_CONTINENTS;
    private static final double X_START_BLOCKS = CaveStructureConfig.MUSHROOM_CAVE_X_START;
    private static final double X_SPACING_BLOCKS = CaveStructureConfig.MUSHROOM_CAVE_X_SPACING * 16.0D;
    private static final double YAW_START = Math.PI * 1.5D;
    private static final double FULL_TURN = Math.PI * 2.0D;
    private static final double FOOTPRINT_PADDING = 5.0D;
    private static final double CEILING_PADDING = 2.0D;
    private static final int SPIRAL_TURNS = 2;

    private MushroomCaveRegion() {
    }

    public static boolean containsFloor(BlockPos floorPos) {
        CaveOrigin origin = nearestApproximateOrigin(floorPos.getX(), floorPos.getZ());
        if (origin == null) {
            return false;
        }

        return containsFloorAir(
                floorPos.getX(),
                floorPos.getY() + 1,
                floorPos.getZ(),
                origin,
                HORIZONTAL_RADIUS,
                VERTICAL_RADIUS,
                CENTRAL_PILLAR_DIAMETER,
                MIN_FLOOR_THICKNESS,
                LIQUID_DEPTH,
                LIQUID_RADIUS
        );
    }

    public static boolean containsFloorForExactCave(
            BlockPos floorPos,
            double originX,
            double originY,
            double originZ,
            double horizontalRadius,
            double verticalRadius,
            double centralPillarDiameter,
            double minFloorThickness,
            double liquidDepth,
            double liquidRadius
    ) {
        return containsFloorAir(
                floorPos.getX(),
                floorPos.getY() + 1,
                floorPos.getZ(),
                new CaveOrigin(originX, originY, originZ),
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius
        );
    }

    public static boolean containsUpperCeilingColumn(BlockPos surfacePos) {
        int floorAirY = surfacePos.getY() + 1;
        int highestUpperFloorAirY = Mth.ceil(ORIGIN_Y - VERTICAL_RADIUS);
        if (floorAirY > highestUpperFloorAirY) {
            return false;
        }

        CaveOrigin origin = nearestApproximateOrigin(surfacePos.getX(), surfacePos.getZ());
        Double pathTurns = ownedFloorPathTurns(
                surfacePos.getX(),
                floorAirY,
                surfacePos.getZ(),
                origin,
                HORIZONTAL_RADIUS,
                VERTICAL_RADIUS,
                CENTRAL_PILLAR_DIAMETER,
                MIN_FLOOR_THICKNESS,
                LIQUID_DEPTH,
                LIQUID_RADIUS
        );
        return pathTurns != null && pathTurns < MUSHROOM_SECTION_START_TURNS;
    }

    private static boolean containsFloorAir(
            int blockX,
            int floorAirY,
            int blockZ,
            CaveOrigin origin,
            double horizontalRadius,
            double verticalRadius,
            double centralPillarDiameter,
            double minFloorThickness,
            double liquidDepth,
            double liquidRadius
    ) {
        Double pathTurns = ownedFloorPathTurns(
                blockX,
                floorAirY,
                blockZ,
                origin,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius
        );
        return pathTurns != null
                && pathTurns >= MUSHROOM_SECTION_START_TURNS
                && pathTurns <= MUSHROOM_SECTION_END_TURNS;
    }

    private static Double ownedFloorPathTurns(
            int blockX,
            int floorAirY,
            int blockZ,
            CaveOrigin origin,
            double horizontalRadius,
            double verticalRadius,
            double centralPillarDiameter,
            double minFloorThickness,
            double liquidDepth,
            double liquidRadius
    ) {
        if (origin == null || !containsHorizontalFootprint(
                blockX,
                blockZ,
                origin,
                horizontalRadius,
                centralPillarDiameter,
                liquidRadius
        )) {
            return null;
        }

        double tunnelRadius = horizontalRadius + liquidRadius;
        double pathRadius = centralPillarDiameter * 0.5D + tunnelRadius;
        double dx = blockX + 0.5D - origin.x();
        double dz = blockZ + 0.5D - origin.z();
        double theta = positiveAngle(Math.atan2(dz, dx));
        double clockwiseFromStart = positiveAngle(YAW_START - theta);
        double dropPerTurn = verticalRadius * 2.0D
                + minFloorThickness
                + 1.0D
                + liquidDepth;
        double topCenterYAtTheta = origin.y() - clockwiseFromStart / FULL_TURN * dropPerTurn;
        double firstCeilingY = topCenterYAtTheta + verticalRadius + CEILING_PADDING;
        int turn = Math.max(0, Mth.floor((firstCeilingY - floorAirY) / dropPerTurn));
        double seamAngle = (tunnelRadius + FOOTPRINT_PADDING) / pathRadius;
        boolean ownsTerminalSeam = turn == SPIRAL_TURNS && clockwiseFromStart < seamAngle;

        if (turn >= SPIRAL_TURNS && !ownsTerminalSeam) {
            return null;
        }

        double centerY = topCenterYAtTheta - turn * dropPerTurn;
        double topY = centerY + verticalRadius + CEILING_PADDING;
        double nextFloorCeilingY = centerY - dropPerTurn + verticalRadius + CEILING_PADDING;
        if (floorAirY > topY || floorAirY < nextFloorCeilingY) {
            return null;
        }

        return turn + clockwiseFromStart / FULL_TURN;
    }

    private static boolean containsHorizontalFootprint(
            int blockX,
            int blockZ,
            CaveOrigin origin,
            double horizontalRadius,
            double centralPillarDiameter,
            double liquidRadius
    ) {
        double tunnelRadius = horizontalRadius + liquidRadius;
        double pathRadius = centralPillarDiameter * 0.5D + tunnelRadius;
        double dx = blockX + 0.5D - origin.x();
        double dz = blockZ + 0.5D - origin.z();
        double radialDistance = Math.sqrt(dx * dx + dz * dz);
        double pillarRadius = pathRadius - tunnelRadius;
        return radialDistance <= pillarRadius + FOOTPRINT_PADDING
                || Math.abs(radialDistance - pathRadius) <= tunnelRadius + FOOTPRINT_PADDING;
    }

    private static double positiveAngle(double angle) {
        double result = angle % FULL_TURN;
        return result < 0.0D ? result + FULL_TURN : result;
    }

    private static CaveOrigin nearestApproximateOrigin(int blockX, int blockZ) {
        double targetZ = nearestTargetZ(blockZ);
        if (targetZ < 0.0D) {
            return null;
        }

        double targetX = X_START_BLOCKS
                + Math.round((blockX - X_START_BLOCKS) / X_SPACING_BLOCKS) * X_SPACING_BLOCKS;
        return new CaveOrigin(targetX, ORIGIN_Y, targetZ);
    }

    private static double nearestTargetZ(int blockZ) {
        double cycle = Math.floor((double) blockZ / PERIOD_BLOCKS);
        double bestZ = -1.0D;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int cycleOffset = -1; cycleOffset <= 1; cycleOffset++) {
            double baseZ = (cycle + cycleOffset) * PERIOD_BLOCKS;
            double[] candidates = {
                    baseZ + TARGET_CONTINENTS * PERIOD_BLOCKS,
                    baseZ + (1.0D - TARGET_CONTINENTS) * PERIOD_BLOCKS
            };

            for (double candidate : candidates) {
                if (candidate < 0.0D) {
                    continue;
                }

                double distance = Math.abs(blockZ - candidate);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestZ = candidate;
                }
            }
        }

        return bestZ;
    }

    private record CaveOrigin(double x, double y, double z) {
    }
}
