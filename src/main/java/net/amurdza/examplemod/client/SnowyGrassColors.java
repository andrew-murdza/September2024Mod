package net.amurdza.examplemod.client;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.level.Level;

/** Custom dimensions with exact, unaveraged block colors. */
public final class SnowyGrassColors {
    private SnowyGrassColors() { }

    public static boolean unblended(Level level) {
        return GameplayDimensions.isCustom(level) && !level.dimension().equals(GameplayDimensions.TROPICAL);
    }
}
