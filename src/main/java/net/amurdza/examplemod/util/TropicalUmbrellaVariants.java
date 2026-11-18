package net.amurdza.examplemod.util;

import dev.hybridlabs.aquatic.entity.cephalopod.UmbrellaOctopusEntity;
import net.minecraft.world.entity.Entity;

/** Matches Hybrid Aquatic's UUID-derived six-texture umbrella-octopus palette. */
public final class TropicalUmbrellaVariants {
    private static final String[] COLORS = {"yellow", "brown", "orange", "pink", "purple"};
    private TropicalUmbrellaVariants() { }

    public static String color(Entity entity) {
        String color = COLORS[Math.floorMod(entity.getUUID().getLeastSignificantBits(), COLORS.length)];
        // Also cover existing individuals and spawn eggs without changing UUIDs.
        return color.equals("brown") && GameplayDimensions.is(entity.level(), GameplayDimensions.TROPICAL)
                ? "orange" : color;
    }

    public static boolean excludedNaturalVariant(Entity entity) {
        // Match the actual five-color renderer, rather than upstream's unrelated
        // six-color Kotlin random selection. Brown is the pictured taupe variant.
        return entity instanceof UmbrellaOctopusEntity
                && GameplayDimensions.is(entity.level(), GameplayDimensions.TROPICAL)
                && Math.floorMod(entity.getUUID().getLeastSignificantBits(), COLORS.length) == 1;
    }
}
