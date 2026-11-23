package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** The flat forest boundary, independently of however far its ramp must descend. */
public record MansionEstateShape(BoundingBox house, int layoutVersion) {
    public static final int SQUARE_VERSION = 11;
    public static final int WIDE_CIRCLE_VERSION = 12;
    public static final int WIDE_MARGIN = 128;
    public boolean square() { return layoutVersion == SQUARE_VERSION; }
    public boolean widened() { return layoutVersion >= SQUARE_VERSION; }
    public int margin() { return widened() ? WIDE_MARGIN : 64; }
    public double centerX() { return (house.minX() + house.maxX()) / 2.0; }
    public double centerZ() { return (house.minZ() + house.maxZ()) / 2.0; }
    public double halfWidth() { return Math.max(house.getXSpan() - 1, house.getZSpan() - 1) / 2.0 + margin(); }
    public double distanceBeyondFlat(int x, int z) {
        double dx = Math.abs(x - centerX()), dz = Math.abs(z - centerZ());
        return (square() ? Math.max(dx, dz) : Math.hypot(dx, dz)) - halfWidth();
    }
    public boolean contains(int x, int z) { return distanceBeyondFlat(x, z) <= 0; }
}
