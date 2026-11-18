package net.amurdza.examplemod.portal;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Route permissions are shared with the picker, but always enforced again on the server. */
public enum PortalDestination {
    OVERWORLD("overworld", Level.OVERWORLD, 0x86A9DC),
    TROPICAL("tropical", GameplayDimensions.TROPICAL, 0x48D05A),
    SAVANNA("savanna", GameplayDimensions.SAVANNA, 0xE99D36),
    PLAINS("plains", GameplayDimensions.PLAINS, 0x929292),
    SNOWY("snowy", GameplayDimensions.SNOWY, 0xFFFFFF),
    DESERT("desert", GameplayDimensions.DESERT, 0xF8DD39),
    NETHER("nether", GameplayDimensions.NETHER, 0xEF4444);

    private final String id;
    private final ResourceKey<Level> dimension;
    private final int color;

    PortalDestination(String id, ResourceKey<Level> dimension, int color) {
        this.id = id;
        this.dimension = dimension;
        this.color = color;
    }

    public String id() { return id; }
    public ResourceKey<Level> dimension() { return dimension; }
    public int color() { return color; }
    public String translationKey() { return "portal.aoemod.destination." + id; }

    public static Optional<PortalDestination> fromDimension(ResourceKey<Level> dimension) {
        return Arrays.stream(values()).filter(destination -> destination.dimension.equals(dimension)).findFirst();
    }

    public static List<PortalDestination> allowedFrom(ResourceKey<Level> source) {
        return Arrays.stream(values()).filter(destination -> destination.allowedFromSource(source)).toList();
    }

    public boolean allowedFromSource(ResourceKey<Level> source) {
        if (dimension.equals(source) || fromDimension(source).isEmpty()) {
            return false;
        }
        if (GameplayDimensions.NETHER.equals(source)) {
            return this == DESERT;
        }
        if (this == NETHER) {
            return GameplayDimensions.DESERT.equals(source);
        }
        return true;
    }
}
