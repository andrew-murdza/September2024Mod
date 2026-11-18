import net.amurdza.examplemod.worldgen.SavannaRivers;

import java.util.ArrayDeque;

/** Runs the production geometry without Minecraft; checks topology and all eight neighbouring steps. */
public final class SavannaRiverAudit {
    public static void main(String[] args) {
        int enclosed = 0, samples = 0, water = 0;
        double maxStep = 0, minWidth = 100, maxWidth = 0;
        for (int seed = 0; seed < 16; seed++) {
            var rivers = new SavannaRivers(seed * 8163124321L, (x, z) -> 72);
            for (int row = -2; row <= 2; row++) for (int col = -2; col <= 2; col++) {
                var river = rivers.river(col, row);
                if (!river.active()) continue;
                for (int segment = 1; segment < river.points().length; segment++) {
                    var a = river.points()[segment - 1];
                    var b = river.points()[segment];
                    double dx = b.x() - a.x(), dz = b.z() - a.z(), length = Math.hypot(dx, dz);
                    for (double t = .125; t < 1; t += .125) {
                        double cx = a.x() + dx * t, cz = a.z() + dz * t;
                        double first = Double.NaN, last = Double.NaN;
                        for (double offset = -48; offset <= 48; offset += .5) {
                            double x = cx - dz / length * offset, z = cz + dx / length * offset;
                            if (rivers.height(x, z, 72) < 62.5) {
                                if (Double.isNaN(first)) first = offset;
                                last = offset;
                            }
                            int bx = (int) Math.round(x), bz = (int) Math.round(z);
                            double height = rivers.height(bx, bz, 72);
                            for (int sx = -1; sx <= 1; sx++) for (int sz = -1; sz <= 1; sz++) {
                                double other = rivers.height(bx + sx, bz + sz, 72);
                                maxStep = Math.max(maxStep, Math.abs(height - other));
                                if (Math.abs(Math.floor(height + .5) - Math.floor(other + .5)) > 1)
                                    throw new AssertionError("Unclimbable diagonal bank");
                            }
                        }
                        double width = last - first + .5;
                        minWidth = Math.min(minWidth, width);
                        maxWidth = Math.max(maxWidth, width);
                        if (width < 24 || width > 48) throw new AssertionError("Water width " + width);
                    }
                }
            }
            // Flood-fill dry land from the map boundary; any unreached dry cell is an island.
            int side = 513;
            boolean[] wet = new boolean[side * side], reached = new boolean[side * side];
            var queue = new ArrayDeque<Integer>();
            for (int z = 0; z < side; z++) for (int x = 0; x < side; x++) {
                int index = z * side + x;
                wet[index] = rivers.height((x - side / 2) * 8, (z - side / 2) * 8, 72) < 62.5;
                water += wet[index] ? 1 : 0;
                samples++;
                if (!wet[index] && (x == 0 || z == 0 || x == side - 1 || z == side - 1)) {
                    reached[index] = true;
                    queue.add(index);
                }
            }
            while (!queue.isEmpty()) {
                int index = queue.remove(), x = index % side, z = index / side;
                for (int adjacent : new int[]{x > 0 ? index - 1 : -1, x + 1 < side ? index + 1 : -1,
                        z > 0 ? index - side : -1, z + 1 < side ? index + side : -1}) {
                    if (adjacent >= 0 && !wet[adjacent] && !reached[adjacent]) {
                        reached[adjacent] = true;
                        queue.add(adjacent);
                    }
                }
            }
            for (int i = 0; i < wet.length; i++) if (!wet[i] && !reached[i]) enclosed++;
        }
        if (enclosed != 0) throw new AssertionError("Enclosed land samples " + enclosed);
        System.out.printf(java.util.Locale.ROOT,
                "seeds=16, water=%.3f%%, enclosed_land_samples=%d, water_width=%.2f..%.2f, max_diagonal_step=%.6f%n",
                100.0 * water / samples, enclosed, minWidth, maxWidth, maxStep);
    }
}
