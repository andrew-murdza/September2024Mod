package net.amurdza.examplemod.util;

import net.minecraft.world.level.Level;

/** Underground life follows the ordinary day clock even without a visible sky. */
public final class SoulCaveClock {
    private SoulCaveClock() { }

    public static boolean isNight(Level level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return time >= 12600L && time <= 23400L;
    }
}
