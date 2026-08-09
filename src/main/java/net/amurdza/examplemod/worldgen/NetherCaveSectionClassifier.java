package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.config.CaveStructureConfig;
import net.minecraft.util.Mth;

public final class NetherCaveSectionClassifier {
    public enum Section {
        DESERT,
        DEEP_DARK,
        SOUL_SAND_VALLEY,
        WARPED_FOREST,
        CRIMSON_FOREST,
        BASALT_DELTAS
    }

    private static final double DEFAULT_ORIGIN_Y = CaveStructureConfig.tunnelOriginY(
            CaveStructureConfig.NETHER_CAVE_START_Y,
            CaveStructureConfig.NETHER_CAVE_VERTICAL_RADIUS
    );
    private static final double DEFAULT_HORIZONTAL_RADIUS =
            CaveStructureConfig.NETHER_CAVE_HORIZONTAL_RADIUS;
    private static final double DEFAULT_VERTICAL_RADIUS =
            CaveStructureConfig.NETHER_CAVE_VERTICAL_RADIUS;
    private static final double DEFAULT_CENTRAL_PILLAR_DIAMETER =
            CaveStructureConfig.NETHER_CAVE_CENTRAL_PILLAR_DIAMETER;
    private static final double DEFAULT_MIN_FLOOR_THICKNESS =
            CaveStructureConfig.NETHER_CAVE_MIN_FLOOR_THICKNESS;
    private static final double DEFAULT_LIQUID_DEPTH = CaveStructureConfig.NETHER_CAVE_LIQUID_DEPTH;
    private static final double DEFAULT_LIQUID_RADIUS = CaveStructureConfig.NETHER_CAVE_LIQUID_RADIUS;
    private static final int DEFAULT_END_X = CaveStructureConfig.NETHER_CAVE_END_X;
    private static final int DEFAULT_MAX_DEEP_DARK_Y =
            CaveStructureConfig.NETHER_CAVE_MAX_DEEP_DARK_Y;
    private static final int DEFAULT_MAX_SOUL_SAND_VALLEY_Y =
            CaveStructureConfig.NETHER_CAVE_MAX_SOUL_SAND_VALLEY_Y;
    private static final int DEFAULT_MAX_WARPED_FOREST_Y =
            CaveStructureConfig.NETHER_CAVE_MAX_WARPED_FOREST_Y;
    private static final int DEFAULT_MAX_CRIMSON_FOREST_Y =
            CaveStructureConfig.NETHER_CAVE_MAX_CRIMSON_FOREST_Y;
    private static final int DEFAULT_MAX_BASALT_DELTAS_Y =
            CaveStructureConfig.NETHER_CAVE_MAX_BASALT_DELTAS_Y;

    private static final int BIOME_WIDTH_BLOCKS = CaveStructureConfig.NETHER_CAVE_BIOME_WIDTH * 16;
    private static final int PERIOD_BLOCKS = BIOME_WIDTH_BLOCKS * 10;
    private static final double TARGET_CONTINENTS = CaveStructureConfig.NETHER_CAVE_TARGET_CONTINENTS;
    private static final double X_START_BLOCKS = CaveStructureConfig.NETHER_CAVE_X_START;
    private static final double X_SPACING_BLOCKS = CaveStructureConfig.NETHER_CAVE_X_SPACING * 16.0D;
    private static final double YAW_START = Math.PI * 1.5D;
    private static final double FULL_TURN = Math.PI * 2.0D;
    private static final int SPIRAL_TURNS = 5;
    private static final double UPWARD_SPIRAL_TURNS = 1.0D;
    private static final double SOUTHBOUND_UPWARD_SPIRAL_TURNS = 0.25D;
    private static final double UPPER_VERTICAL_RADIUS_INCREASE = 16.0D;
    private static final double HORIZONTAL_FOOTPRINT_PADDING = 5.0D;
    private static final double CEILING_OWNERSHIP_PADDING = 3.0D;
    private static final double DEEP_DARK_CEILING_EXTRA = 2.0D;
    private static final int STRAIGHT_TRANSITION_LENGTH = 56;
    private static final int STRAIGHT_TERRACE_SUBSURFACE_DEPTH = 4;
    private static final int QUART_SIZE = 4;
    private static final int CEILING_BIOME_OWNERSHIP_PADDING = QUART_SIZE / 2;

    private NetherCaveSectionClassifier() {
    }

    public static Section sectionForExactCave(
            int blockX,
            int blockY,
            int blockZ,
            double originX,
            double originY,
            double originZ,
            double horizontalRadius,
            double verticalRadius,
            double centralPillarDiameter,
            double minFloorThickness,
            double liquidDepth,
            double liquidRadius,
            int endX,
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY
    ) {
        return sectionForCave(
                blockX,
                blockY,
                blockZ,
                originX,
                originY,
                originZ,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius,
                endX,
                maxDeepDarkY,
                maxSoulSandValleyY,
                maxWarpedForestY,
                maxCrimsonForestY,
                maxBasaltDeltasY
        );
    }

    public static Section sectionForApproximateWorld(int blockX, int blockY, int blockZ) {
        CaveOrigin origin = nearestApproximateOrigin(blockX, blockZ);

        if (origin == null) {
            return Section.DESERT;
        }

        return sectionForCave(
                blockX,
                blockY,
                blockZ,
                origin.x(),
                DEFAULT_ORIGIN_Y,
                origin.z(),
                DEFAULT_HORIZONTAL_RADIUS,
                DEFAULT_VERTICAL_RADIUS,
                DEFAULT_CENTRAL_PILLAR_DIAMETER,
                DEFAULT_MIN_FLOOR_THICKNESS,
                DEFAULT_LIQUID_DEPTH,
                DEFAULT_LIQUID_RADIUS,
                DEFAULT_END_X,
                DEFAULT_MAX_DEEP_DARK_Y,
                DEFAULT_MAX_SOUL_SAND_VALLEY_Y,
                DEFAULT_MAX_WARPED_FOREST_Y,
                DEFAULT_MAX_CRIMSON_FOREST_Y,
                DEFAULT_MAX_BASALT_DELTAS_Y
        );
    }

    public static boolean isOnApproximateInnerSideOfRiver(int blockX, int blockZ) {
        CaveOrigin origin = nearestApproximateOrigin(blockX, blockZ);

        if (origin == null) {
            return false;
        }

        return isOnInnerSideOfRiver(
                blockX,
                blockZ,
                origin.x(),
                origin.z(),
                DEFAULT_HORIZONTAL_RADIUS,
                DEFAULT_CENTRAL_PILLAR_DIAMETER,
                DEFAULT_LIQUID_RADIUS,
                DEFAULT_END_X
        );
    }

    private static Section sectionForCave(
            int blockX,
            int blockY,
            int blockZ,
            double originX,
            double originY,
            double originZ,
            double horizontalRadius,
            double verticalRadius,
            double centralPillarDiameter,
            double minFloorThickness,
            double liquidDepth,
            double liquidRadius,
            int endX,
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY
    ) {
        CaveFloor floor = nearestSpiralFloor(
                blockX,
                blockY,
                blockZ,
                originX,
                originY,
                originZ,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius,
                endX,
                maxDeepDarkY,
                maxSoulSandValleyY
        );

        if (floor == null) {
            return Section.DESERT;
        }

        if (floor.fromStraightExit()) {
            return Section.DESERT;
        }

        return sectionForFloor(
                floor.floorAirY(),
                floor.theta(),
                maxDeepDarkY,
                maxSoulSandValleyY,
                maxWarpedForestY,
                maxCrimsonForestY,
                maxBasaltDeltasY
        );
    }

    private static CaveFloor nearestSpiralFloor(
            int blockX,
            int blockY,
            int blockZ,
            double originX,
            double originY,
            double originZ,
            double horizontalRadius,
            double verticalRadius,
            double centralPillarDiameter,
            double minFloorThickness,
            double liquidDepth,
            double liquidRadius,
            int endX,
            int maxDeepDarkY,
            int maxSoulSandValleyY
    ) {
        double tunnelHorizontalRadius = horizontalRadius + liquidRadius;
        double pathRadius = centralPillarDiameter * 0.5D + tunnelHorizontalRadius;

        double dx = blockX + 0.5D - originX;
        double dz = blockZ + 0.5D - originZ;

        boolean insideSpiralFootprint = isInsideSpiralFootprint(
                blockX,
                blockZ,
                originX,
                originZ,
                pathRadius,
                tunnelHorizontalRadius
        );
        boolean insideExitFootprint = isInsideStraightExitFootprint(
                blockX,
                blockZ,
                originX,
                originZ,
                pathRadius,
                tunnelHorizontalRadius,
                endX
        );

        double theta = positiveAngle(Math.atan2(dz, dx));
        double clockwiseFromStart = positiveAngle(YAW_START - theta);

        double turnPerStep = 1.0D / Math.max(pathRadius, 1.0D);
        double stepsPerTurn = FULL_TURN / turnPerStep;
        double requiredVerticalSeparation = verticalRadius * 2.0D + minFloorThickness + 1.0D + liquidDepth;
        double dropPerStep = Mth.clamp(requiredVerticalSeparation / stepsPerTurn, 0.01D, 0.85D);
        double dropPerTurn = dropPerStep * stepsPerTurn;

        CaveFloor upwardFloor = nearestUpwardSpiralFloor(
                blockY,
                theta,
                insideSpiralFootprint,
                originY,
                verticalRadius,
                liquidDepth,
                pathRadius,
                tunnelHorizontalRadius,
                dropPerTurn
        );
        if (upwardFloor != null) {
            return upwardFloor;
        }

        double topCenterYAtTheta = originY - (clockwiseFromStart / FULL_TURN) * dropPerTurn;
        double firstFloorAirY = topCenterYAtTheta - verticalRadius;
        boolean firstFloorIsDeepDark = firstFloorAirY <= maxDeepDarkY
                && firstFloorAirY > maxSoulSandValleyY;
        boolean coveredStraightTransition = isInsideCoveredStraightTransition(
                blockX,
                blockZ,
                originX,
                originZ,
                pathRadius,
                tunnelHorizontalRadius
        );

        if (insideSpiralFootprint
                && insideExitFootprint
                && firstFloorIsDeepDark
                && !coveredStraightTransition
                && deepDarkCeilingOwnsY(blockY, topCenterYAtTheta + verticalRadius)) {
            return new CaveFloor(firstFloorAirY, theta, false);
        }

        if (insideExitFootprint
                && isOnStraightExitSide(blockX, originX)
                && straightExitOwnsY(
                        blockY,
                        blockZ,
                        originY,
                        originZ,
                        verticalRadius,
                        pathRadius,
                        liquidDepth,
                        liquidRadius
                )) {
            return new CaveFloor(originY - verticalRadius, YAW_START, true);
        }

        if (!insideSpiralFootprint) {
            return null;
        }

        double firstCeilingY = topCenterYAtTheta + verticalRadius + CEILING_OWNERSHIP_PADDING;
        if (blockY > firstCeilingY) {
            boolean aboveDeepDarkCeiling = firstFloorIsDeepDark
                    && blockY <= firstCeilingY + DEEP_DARK_CEILING_EXTRA;

            if (aboveDeepDarkCeiling) {
                return new CaveFloor(firstFloorAirY, theta, false);
            }

            return null;
        }

        int turn = Math.max(0, Mth.floor(
                (firstCeilingY - blockY + CEILING_BIOME_OWNERSHIP_PADDING) / dropPerTurn
        ));
        double terminalSeamAngle = (tunnelHorizontalRadius + HORIZONTAL_FOOTPRINT_PADDING) / pathRadius;
        boolean ownsTerminalSeam = turn == SPIRAL_TURNS && clockwiseFromStart < terminalSeamAngle;
        if (turn >= SPIRAL_TURNS && !ownsTerminalSeam) {
            return null;
        }

        double centerY = topCenterYAtTheta - turn * dropPerTurn;

        if (floorOwnsY(blockY, centerY, verticalRadius, dropPerTurn)) {
            return new CaveFloor(centerY - verticalRadius, theta, false);
        }

        return null;
    }

    private static CaveFloor nearestUpwardSpiralFloor(
            int blockY,
            double theta,
            boolean insideSpiralFootprint,
            double originY,
            double verticalRadius,
            double liquidDepth,
            double pathRadius,
            double tunnelHorizontalRadius,
            double risePerTurn
    ) {
        if (!insideSpiralFootprint) {
            return null;
        }

        double counterclockwiseFromStart = positiveAngle(theta - YAW_START);
        double maximumUpwardAngle = FULL_TURN * UPWARD_SPIRAL_TURNS;
        double terminalSeamAngle =
                (tunnelHorizontalRadius + HORIZONTAL_FOOTPRINT_PADDING) / pathRadius;

        CaveFloor floor = upwardSpiralFloorAtProgress(
                blockY,
                theta,
                originY,
                verticalRadius,
                liquidDepth,
                risePerTurn,
                counterclockwiseFromStart,
                maximumUpwardAngle,
                terminalSeamAngle
        );
        if (floor != null) {
            return floor;
        }

        floor = upwardSpiralFloorAtProgress(
                blockY,
                theta,
                originY,
                verticalRadius,
                liquidDepth,
                risePerTurn,
                counterclockwiseFromStart - FULL_TURN,
                maximumUpwardAngle,
                terminalSeamAngle
        );
        if (floor != null) {
            return floor;
        }

        return upwardSpiralFloorAtProgress(
                blockY,
                theta,
                originY,
                verticalRadius,
                liquidDepth,
                risePerTurn,
                counterclockwiseFromStart + FULL_TURN,
                maximumUpwardAngle,
                terminalSeamAngle
        );
    }

    private static CaveFloor upwardSpiralFloorAtProgress(
            int blockY,
            double theta,
            double originY,
            double verticalRadius,
            double liquidDepth,
            double risePerTurn,
            double progressAngle,
            double maximumUpwardAngle,
            double terminalSeamAngle
    ) {
        if (progressAngle < -terminalSeamAngle
                || progressAngle > maximumUpwardAngle + terminalSeamAngle) {
            return null;
        }

        double clampedProgressAngle = Mth.clamp(
                progressAngle,
                0.0D,
                maximumUpwardAngle
        );
        double upwardTurns = clampedProgressAngle / FULL_TURN;
        double radiusIncrease = Mth.clamp(
                (upwardTurns - SOUTHBOUND_UPWARD_SPIRAL_TURNS) * risePerTurn,
                0.0D,
                UPPER_VERTICAL_RADIUS_INCREASE
        );
        double upperVerticalRadius = verticalRadius + radiusIncrease;
        double centerY = originY
                + clampedProgressAngle
                / FULL_TURN
                * risePerTurn;
        double lowestOwnedY = centerY - verticalRadius - liquidDepth - 2.0D;
        double highestOwnedY = centerY + upperVerticalRadius + CEILING_OWNERSHIP_PADDING;

        if (blockY < lowestOwnedY || blockY > highestOwnedY) {
            return null;
        }

        return new CaveFloor(centerY - verticalRadius, theta, false);
    }

    private static boolean isOnStraightExitSide(
            int blockX,
            double originX
    ) {
        return blockX + 0.5D >= originX;
    }

    private static boolean isOnInnerSideOfRiver(
            int blockX,
            int blockZ,
            double originX,
            double originZ,
            double horizontalRadius,
            double centralPillarDiameter,
            double liquidRadius,
            int endX
    ) {
        double tunnelHorizontalRadius = horizontalRadius + liquidRadius;
        double pathRadius = centralPillarDiameter * 0.5D + tunnelHorizontalRadius;

        if (isInsideStraightExitFootprint(
                blockX,
                blockZ,
                originX,
                originZ,
                pathRadius,
                tunnelHorizontalRadius,
                endX
        ) && isOnStraightExitSide(blockX, originX)) {
            double straightCenterZ = originZ - pathRadius;
            return blockZ + 0.5D > straightCenterZ + liquidRadius;
        }

        double dx = blockX + 0.5D - originX;
        double dz = blockZ + 0.5D - originZ;
        double radialDistance = Math.sqrt(dx * dx + dz * dz);
        double innerRiverBankRadius = pathRadius - liquidRadius;

        return radialDistance < innerRiverBankRadius;
    }

    private static boolean straightExitOwnsY(
            int blockY,
            int blockZ,
            double originY,
            double originZ,
            double verticalRadius,
            double pathRadius,
            double liquidDepth,
            double liquidRadius
    ) {
        int flatSurfaceY = Mth.floor(originY - verticalRadius - 2.5D);
        double riverCenterZ = originZ - pathRadius;
        double zDistance = Math.abs(blockZ + 0.5D - riverCenterZ);
        int lowestOwnedY = flatSurfaceY - STRAIGHT_TERRACE_SUBSURFACE_DEPTH;

        if (zDistance <= liquidRadius) {
            double normalizedZ = zDistance / liquidRadius;
            double depthScale = Math.sqrt(Math.max(0.0D, 1.0D - normalizedZ * normalizedZ));
            int riverDepth = Math.max(1, Mth.ceil(liquidDepth * depthScale));
            lowestOwnedY = flatSurfaceY - riverDepth;
        }

        double ceilingY = originY + verticalRadius + CEILING_OWNERSHIP_PADDING;
        return blockY >= lowestOwnedY && blockY <= ceilingY;
    }

    private static boolean isInsideCoveredStraightTransition(
            int blockX,
            int blockZ,
            double originX,
            double originZ,
            double pathRadius,
            double tunnelHorizontalRadius
    ) {
        double queryX = blockX + 0.5D;
        double straightCenterZ = originZ - pathRadius;
        return queryX >= originX
                && queryX < originX + STRAIGHT_TRANSITION_LENGTH
                && Math.abs(blockZ + 0.5D - straightCenterZ) <= tunnelHorizontalRadius;
    }

    private static boolean deepDarkCeilingOwnsY(int blockY, double continuousCeilingY) {
        int ceilingBlockY = Mth.ceil(continuousCeilingY - 0.5D);
        int blockAboveCeilingY = ceilingBlockY + 1;
        int lowestQuartCenterY = quartCenterForBlock(ceilingBlockY);
        int highestQuartCenterY = quartCenterForBlock(blockAboveCeilingY);
        int minimumOwnedY = Math.min(ceilingBlockY, lowestQuartCenterY);
        int maximumOwnedY = Math.max(blockAboveCeilingY, highestQuartCenterY);
        return blockY >= minimumOwnedY && blockY <= maximumOwnedY;
    }

    private static int quartCenterForBlock(int blockY) {
        return Math.floorDiv(blockY, QUART_SIZE) * QUART_SIZE + QUART_SIZE / 2;
    }

    private static boolean isInsideSpiralFootprint(
            int blockX,
            int blockZ,
            double originX,
            double originZ,
            double pathRadius,
            double tunnelHorizontalRadius
    ) {
        double dx = blockX + 0.5D - originX;
        double dz = blockZ + 0.5D - originZ;
        double radialDistance = Math.sqrt(dx * dx + dz * dz);

        double pillarRadius = pathRadius - tunnelHorizontalRadius;

        return radialDistance <= pillarRadius + HORIZONTAL_FOOTPRINT_PADDING
                || Math.abs(radialDistance - pathRadius)
                <= tunnelHorizontalRadius + HORIZONTAL_FOOTPRINT_PADDING;
    }

    private static boolean isInsideStraightExitFootprint(
            int blockX,
            int blockZ,
            double originX,
            double originZ,
            double pathRadius,
            double tunnelHorizontalRadius,
            int endX
    ) {
        if (endX <= 0) {
            return false;
        }

        double startX = originX + Math.cos(YAW_START) * pathRadius;
        double startZ = originZ + Math.sin(YAW_START) * pathRadius;

        double queryX = blockX + 0.5D;
        double queryZ = blockZ + 0.5D;
        double clampedX = Mth.clamp(queryX, startX, startX + endX);
        double dx = queryX - clampedX;
        double dz = queryZ - startZ;
        double radius = tunnelHorizontalRadius + HORIZONTAL_FOOTPRINT_PADDING;

        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean floorOwnsY(
            int blockY,
            double centerY,
            double verticalRadius,
            double dropPerTurn
    ) {
        double topY = centerY
                + verticalRadius
                + CEILING_OWNERSHIP_PADDING
                + CEILING_BIOME_OWNERSHIP_PADDING;
        double nextFloorCeilingY = centerY - dropPerTurn
                + verticalRadius
                + CEILING_OWNERSHIP_PADDING;

        return blockY <= topY && blockY >= nextFloorCeilingY;
    }

    private static Section sectionForFloor(
            double floorAirY,
            double theta,
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY
    ) {
        if (floorAirY > maxDeepDarkY) {
            return Section.DESERT;
        }

        if (floorAirY > maxSoulSandValleyY) {
            return Section.DEEP_DARK;
        }

        if (floorAirY > maxWarpedForestY) {
            return Section.SOUL_SAND_VALLEY;
        }

        if (floorAirY > maxCrimsonForestY) {
            return Section.WARPED_FOREST;
        }

        if (floorAirY > maxBasaltDeltasY) {
            return Section.CRIMSON_FOREST;
        }

        return Section.BASALT_DELTAS;
    }

    private static double positiveAngle(double angle) {
        double result = angle % FULL_TURN;

        if (result < 0.0D) {
            result += FULL_TURN;
        }

        return result;
    }

    private static CaveOrigin nearestApproximateOrigin(int blockX, int blockZ) {
        double targetZ = nearestTargetZ(blockZ);
        if (targetZ < 0.0D) {
            return null;
        }

        double targetX = X_START_BLOCKS + Math.round((blockX - X_START_BLOCKS) / X_SPACING_BLOCKS) * X_SPACING_BLOCKS;
        return new CaveOrigin(targetX, targetZ);
    }

    private static double nearestTargetZ(int blockZ) {
        double phaseA = TARGET_CONTINENTS;
        double phaseB = 1.0D - TARGET_CONTINENTS;
        double cycle = Math.floor((double) blockZ / PERIOD_BLOCKS);

        double bestZ = -1.0D;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int cycleOffset = -1; cycleOffset <= 1; cycleOffset++) {
            double baseZ = (cycle + cycleOffset) * PERIOD_BLOCKS;

            double candidateA = baseZ + phaseA * PERIOD_BLOCKS;
            if (candidateA >= 0.0D) {
                double distanceA = Math.abs(blockZ - candidateA);
                if (distanceA < bestDistance) {
                    bestDistance = distanceA;
                    bestZ = candidateA;
                }
            }

            double candidateB = baseZ + phaseB * PERIOD_BLOCKS;
            if (candidateB >= 0.0D) {
                double distanceB = Math.abs(blockZ - candidateB);
                if (distanceB < bestDistance) {
                    bestDistance = distanceB;
                    bestZ = candidateB;
                }
            }
        }

        return bestZ;
    }

    private record CaveFloor(double floorAirY, double theta, boolean fromStraightExit) {
    }

    private record CaveOrigin(double x, double z) {
    }
}
