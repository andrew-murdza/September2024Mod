package net.amurdza.examplemod.config;

public final class CaveStructureConfig {
    public static final double NETHER_CAVE_TARGET_CONTINENTS = 0.45D;
    public static final int NETHER_CAVE_BIOME_WIDTH = 64;
    public static final int NETHER_CAVE_X_START = 560;
    public static final int NETHER_CAVE_X_SPACING = 64;
    public static final float NETHER_CAVE_HORIZONTAL_RADIUS = 50.0F;
    public static final float NETHER_CAVE_VERTICAL_RADIUS = 9.0F;
    public static final int NETHER_CAVE_START_Y = 165;
    public static final int NETHER_CAVE_END_Y = -62;
    public static final int NETHER_CAVE_END_X = 0;
    public static final float NETHER_CAVE_CENTRAL_PILLAR_DIAMETER = 100.0F;
    public static final float NETHER_CAVE_MIN_FLOOR_THICKNESS = 12.0F;
    public static final float NETHER_CAVE_LIQUID_RADIUS = 16.0F;
    public static final float NETHER_CAVE_LIQUID_DEPTH = 12.0F;
    public static final int NETHER_CAVE_MAX_DEEP_DARK_Y = 167;
    public static final int NETHER_CAVE_MAX_SOUL_SAND_VALLEY_Y = 124;
    public static final int NETHER_CAVE_MAX_WARPED_FOREST_Y = 81;
    public static final int NETHER_CAVE_MAX_CRIMSON_FOREST_Y = 38;
    public static final int NETHER_CAVE_MAX_BASALT_DELTAS_Y = -5;

    public static final double MUSHROOM_CAVE_TARGET_CONTINENTS = 0.35D;
    public static final int MUSHROOM_CAVE_BIOME_WIDTH = 64;
    public static final int MUSHROOM_CAVE_X_START = 560;
    public static final int MUSHROOM_CAVE_X_SPACING = 64;
    public static final float MUSHROOM_CAVE_HORIZONTAL_RADIUS = 50.0F;
    public static final float MUSHROOM_CAVE_VERTICAL_RADIUS = 18.0F;
    public static final int MUSHROOM_CAVE_START_Y = 62;
    public static final int MUSHROOM_CAVE_END_Y = -62;
    public static final int MUSHROOM_CAVE_END_X = 0;
    public static final float MUSHROOM_CAVE_CENTRAL_PILLAR_DIAMETER = 100.0F;
    public static final float MUSHROOM_CAVE_MIN_FLOOR_THICKNESS = 8.0F;
    public static final float MUSHROOM_CAVE_LIQUID_RADIUS = 16.0F;
    public static final float MUSHROOM_CAVE_LIQUID_DEPTH = 12.0F;
    // East/northbound to west/southbound: one straight-edged half-turn across the north side.
    public static final double MUSHROOM_CAVE_SECTION_START_TURNS = 0.75D;
    public static final double MUSHROOM_CAVE_SECTION_END_TURNS = 1.25D;

    private CaveStructureConfig() {
    }

    public static int tunnelOriginY(int startY, float verticalRadius) {
        return startY + (int) Math.ceil(verticalRadius + 1.5F);
    }
}
