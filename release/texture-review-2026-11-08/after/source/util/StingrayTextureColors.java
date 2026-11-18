package net.amurdza.examplemod.util;

/** Exact palette repair for Ambient Additions' native 64x64 stingray atlas. */
public final class StingrayTextureColors {
    private StingrayTextureColors() { }

    public static boolean isSpot(int argb) {
        int rgb = argb & 0xFFFFFF;
        return (argb >>> 24) != 0 && (rgb == 0x726C8A || rgb == 0x9E9466);
    }

    public static int withoutSpot(int x, int y, int argb) {
        if (!isSpot(argb)) return argb;
        // Continue the native amber gradient across each wing's three top
        // columns. The other spot texels lie on the amber rim and tail.
        int background = 0xBA8524;
        if (x >= 38 && x <= 40 && y < 10) {
            background = switch (x) {
                case 39 -> 0xCEA628;
                case 40 -> 0xD9BC4E;
                default -> 0xBA8524;
            };
        } else if (x >= 9 && x <= 14 && y >= 9 && y <= 10) {
            background = 0xD9BC4E;
        }
        return (argb & 0xFF000000) | background;
    }
}
