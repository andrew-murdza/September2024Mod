import net.amurdza.examplemod.worldgen.TropicalLayout;

import java.util.Arrays;

/** Measure the radial widths and grade using the production sampler. */
public final class TropicalLayoutAudit {
    public static void main(String[] args) {
        int islands = args.length == 0 ? 256 : Integer.parseInt(args[0]);
        int[] expected = {60, 30, 120, 150};
        for (int i = 0; i < islands; i++) {
            TropicalLayout layout = new TropicalLayout(TropicalLayout.mix(i));
            var island = layout.island(i % 11 - 5, i % 17 - 8);
            for (int direction = 0; direction < 16; direction++) {
                int[] widths = new int[4];
                double angle = direction * Math.PI / 8;
                double previous = 0;
                for (int r = 0; r < 360; r++) {
                    var sample = layout.sample(island.x() + (r + .5) * Math.cos(angle),
                            island.z() + (r + .5) * Math.sin(angle));
                    int band = sample.biome() == 1 ? (sample.height() + .5 >= 63 ? 1 : 0) : sample.biome();
                    widths[band]++;
                    if (r > 0 && r < TropicalLayout.FOREST_RADIUS && Math.abs(previous - sample.height() - 1.0 / 12) > 1.e-7)
                        throw new AssertionError("Slope at radius " + r);
                    if (r >= 270 && r < 300 && Math.round(sample.height()) != 63)
                        throw new AssertionError("Mangrove land must be Y63");
                    if (r >= 315 && r < 358 && Math.round(sample.height()) != 58)
                        throw new AssertionError("Missing constant-depth mangrove shelf");
                    previous = sample.height();
                }
                if (!Arrays.equals(widths, expected)) throw new AssertionError(Arrays.toString(widths));
            }
        }
        System.out.println("Verified 600-block land diameter, four habitat bands, Y63 mangrove land, five-deep shelf and forest-only 1:12 slope on " + islands + " islands.");
    }
}
