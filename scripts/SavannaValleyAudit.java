import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.amurdza.examplemod.worldgen.SavannaHeight;
import net.amurdza.examplemod.worldgen.SavannaLakeBasins;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Seeded continuous-terrain audit; never opens or edits a Minecraft world. */
public final class SavannaValleyAudit {
    private static final int SIDE = 512, STEP = 8, MIN = -SIDE * STEP / 2;
    private static final double SCALE = 2.35, EPSILON = .01;
    private static final long[] SEEDS = {0, 12345, 98765, -8152788754161584091L};
    private static final Path RESOURCES = Path.of("src/main/resources/data/aoemod/worldgen");

    private static NormalNoise noise(long seed, String name) throws Exception {
        var spec = JsonParser.parseString(Files.readString(RESOURCES.resolve("noise/terrain/savanna/" + name + ".json"))).getAsJsonObject();
        double[] amplitudes = new double[spec.getAsJsonArray("amplitudes").size()];
        for (int i = 0; i < amplitudes.length; i++) amplitudes[i] = spec.getAsJsonArray("amplitudes").get(i).getAsDouble();
        return NormalNoise.create(new XoroshiroRandomSource(seed).forkPositional().fromHashOf("aoemod:terrain/savanna/" + name),
                spec.get("firstOctave").getAsInt(), amplitudes);
    }

    /** Frozen profile immediately before the flooded-valley revision. */
    private static double previous(double land, double detail) {
        double distance = land + .5;
        if (distance <= 0) return Math.max(35.5, 61.5 + distance * 100);
        double rise = Math.pow(Math.pow(3.0 / 145, 2.0 / 3) + distance, 1.5) * 145 - 3;
        double upper = Math.max(0, Math.min(1, (distance - .9) / .5));
        return 61.5 + 4 * ((rise + detail * 5 * Math.min(1, distance / .08)) / 34.75
                + 2.5 * upper * upper * (3 - 2 * upper));
    }

    private static boolean water(double height) { return height <= SavannaHeight.WATER_EDGE_HEIGHT; }

    private static Map<String, Object> summarize(double[] heights) {
        var result = new LinkedHashMap<String, Object>();
        int wet = 0, bottoms = 0, complete = 0, boundary = 0, shallow = 0;
        double lowest = Double.POSITIVE_INFINITY, highest = Double.NEGATIVE_INFINITY;
        for (double height : heights) {
            lowest = Math.min(lowest, height); highest = Math.max(highest, height);
            if (water(height)) wet++;
            if (height == SavannaHeight.BOTTOM) bottoms++;
        }
        boolean[] visited = new boolean[heights.length];
        int[] queue = new int[heights.length];
        var areas = new ArrayList<Integer>();
        for (int start = 0; start < heights.length; start++) {
            if (visited[start] || !water(heights[start])) continue;
            int head = 0, tail = 1;
            queue[0] = start; visited[start] = true;
            boolean edge = false, fullDepth = false;
            while (head < tail) {
                int index = queue[head++], x = index / SIDE, z = index % SIDE;
                edge |= x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1;
                fullDepth |= heights[index] == SavannaHeight.BOTTOM;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 || x + dx < 0 || x + dx >= SIDE || z + dz < 0 || z + dz >= SIDE) continue;
                    int next = (x + dx) * SIDE + z + dz;
                    if (!visited[next] && water(heights[next])) { visited[next] = true; queue[tail++] = next; }
                }
            }
            if (edge) boundary++;
            else { complete++; areas.add(tail * STEP * STEP); if (!fullDepth) shallow++; }
        }
        // Group equal-height plateaus, then require a lower neighbor somewhere
        // along each dry component's boundary. Exclude map-edge components.
        Arrays.fill(visited, false);
        int dryMinima = 0;
        for (int start = 0; start < heights.length; start++) {
            if (visited[start] || water(heights[start])) continue;
            int head = 0, tail = 1;
            queue[0] = start; visited[start] = true;
            boolean edge = false, descends = false;
            double height = heights[start];
            while (head < tail) {
                int index = queue[head++], x = index / SIDE, z = index % SIDE;
                edge |= x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 || x + dx < 0 || x + dx >= SIDE || z + dz < 0 || z + dz >= SIDE) continue;
                    int next = (x + dx) * SIDE + z + dz;
                    double difference = heights[next] - height;
                    if (difference < -1e-9) descends = true;
                    else if (Math.abs(difference) <= 1e-9 && !visited[next]) {
                        visited[next] = true; queue[tail++] = next;
                    }
                }
            }
            if (!edge && !descends) dryMinima++;
        }
        areas.sort(Integer::compareTo);
        result.put("sampled_columns", heights.length);
        result.put("water_percent", wet * 100.0 / heights.length);
        result.put("full_depth_columns", bottoms);
        result.put("lowest_height", lowest);
        result.put("highest_height", highest);
        result.put("highest_solid_block", (int) Math.ceil(highest + .5) - 1);
        result.put("complete_lakes", complete);
        result.put("boundary_lakes", boundary);
        result.put("complete_lakes_without_full_depth", shallow);
        result.put("dry_minima", dryMinima);
        result.put("median_complete_lake_area", areas.isEmpty() ? 0 : areas.get(areas.size() / 2));
        return result;
    }

    private static int color(double[] heights, int x, int z) {
        double h = heights[x * SIDE + z];
        Color low, high;
        double amount;
        if (water(h)) {
            low = new Color(20, 66, 111); high = new Color(88, 176, 200);
            amount = (h - SavannaHeight.BOTTOM) / (SavannaHeight.WATER_EDGE_HEIGHT - SavannaHeight.BOTTOM);
        } else {
            low = new Color(132, 155, 85); high = new Color(215, 188, 130);
            amount = Math.min(1, (h - SavannaHeight.WATER_EDGE_HEIGHT) / 12);
        }
        double dx = heights[Math.min(SIDE - 1, x + 1) * SIDE + z] - heights[Math.max(0, x - 1) * SIDE + z];
        double dz = heights[x * SIDE + Math.min(SIDE - 1, z + 1)] - heights[x * SIDE + Math.max(0, z - 1)];
        double shade = Math.max(.7, Math.min(1.13, 1 + (dx + dz) / 25));
        if (!water(h) && x > 0 && z > 0 && ((int) h != (int) heights[(x - 1) * SIDE + z]
                || (int) h != (int) heights[x * SIDE + z - 1])) shade *= .83;
        int red = (int) Math.min(255, (low.getRed() + amount * (high.getRed() - low.getRed())) * shade);
        int green = (int) Math.min(255, (low.getGreen() + amount * (high.getGreen() - low.getGreen())) * shade);
        int blue = (int) Math.min(255, (low.getBlue() + amount * (high.getBlue() - low.getBlue())) * shade);
        return new Color(red, green, blue).getRGB();
    }

    private static void preview(Path path, long seed, double[] old, double[] current,
                                Map<String, Object> before, Map<String, Object> after) throws Exception {
        var image = new BufferedImage(SIDE * 2 + 72, SIDE + 192, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(246, 245, 240)); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(35, 48, 41)); graphics.setFont(new Font("SansSerif", Font.BOLD, 27));
        graphics.drawString("Savanna valleys and lakes", 24, 36);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 15));
        graphics.drawString("Terrain-height preview · seed " + seed + " · same 4,096 × 4,096 block region", 24, 62);
        double[][] maps = {old, current};
        var stats = List.of(before, after);
        for (int panel = 0; panel < 2; panel++) {
            int left = 24 + panel * (SIDE + 24);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 19));
            graphics.drawString(panel == 0 ? "Previous terrain" : "Flooded valleys", left, 97);
            for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) image.setRGB(left + x, 112 + z, color(maps[panel], x, z));
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
            graphics.drawString(String.format(Locale.ROOT, "Water %.1f%% · dry minima %s · highest sampled block Y%s",
                    stats.get(panel).get("water_percent"), stats.get(panel).get("dry_minima"), stats.get(panel).get("highest_solid_block")), left, SIDE + 139);
        }
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 13));
        graphics.drawString("Blue: water depth (light shores, dark lake floors). Land contours: 1 block. Analytical preview, not an in-game screenshot.", 24, SIDE + 173);
        graphics.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); } catch (LinkageError expectedOutsideForge) { }
        Path output = Path.of(args.length == 0 ? "build/savanna-valleys" : args[0]);
        Files.createDirectories(output);
        var rows = new ArrayList<Map<String, Object>>();
        for (long seed : SEEDS) {
            var land = noise(seed, "land");
            var detail = noise(seed, "detail");
            var terrain = new SavannaLakeBasins((x, z) -> land.getValue(x * SCALE, 0, z * SCALE));
            double[] old = new double[SIDE * SIDE], current = new double[SIDE * SIDE];
            double maxSlope = 0, slopeSum = 0;
            int slopeSamples = 0;
            for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) {
                double wx = MIN + x * STEP, wz = MIN + z * STEP;
                int index = x * SIDE + z;
                old[index] = previous(land.getValue(wx * SCALE, 0, wz * SCALE), detail.getValue(wx * SCALE, 0, wz * SCALE));
                current[index] = terrain.height(wx, wz);
                if (!water(current[index]) && x % 4 == 0 && z % 4 == 0) {
                    double east = terrain.height(wx + EPSILON, wz), west = terrain.height(wx - EPSILON, wz);
                    double south = terrain.height(wx, wz + EPSILON), north = terrain.height(wx, wz - EPSILON);
                    if (water(east) || water(west) || water(south) || water(north)) continue;
                    double dx = (east - west) / (2 * EPSILON);
                    double dz = (south - north) / (2 * EPSILON);
                    double slope = Math.hypot(dx, dz);
                    maxSlope = Math.max(maxSlope, slope); slopeSum += slope; slopeSamples++;
                }
            }
            var before = summarize(old);
            var after = summarize(current);
            after.put("mean_dry_slope", slopeSum / slopeSamples);
            after.put("max_dry_slope", maxSlope);
            if (((Number) after.get("dry_minima")).intValue() != 0) throw new AssertionError("Dry valley remains: " + after);
            if (((Number) after.get("complete_lakes_without_full_depth")).intValue() != 0) throw new AssertionError("Shallow lake remains: " + after);
            if (((Number) after.get("complete_lakes")).intValue() == 0 || ((Number) after.get("highest_height")).doubleValue() >= 72) {
                throw new AssertionError("Invalid terrain envelope: " + after);
            }
            if (maxSlope > SavannaHeight.MAX_LAND_SLOPE + 1e-6) throw new AssertionError("Steep dry bank: " + maxSlope);
            rows.add(Map.of("seed", seed, "previous", before, "current", after));
            System.out.println("seed=" + seed + " previous=" + before + " current=" + after);
            if (seed == SEEDS[0]) preview(output.resolve("terrain-preview.png"), seed, old, current, before, after);
        }
        var report = new LinkedHashMap<String, Object>();
        report.put("audit_source", "scripts/SavannaValleyAudit.java");
        report.put("region_per_seed", "[-2048,2048) squared, sampled every eight blocks");
        report.put("seeds", rows);
        report.put("limitations", "Finite continuous-height samples using production noise parameters, before block rounding. Eight-neighbor water connectivity and area are raster estimates; map-edge lakes and minima are excluded. Plateau minima use an equality tolerance of 1e-9. Dry interior slopes use central differences with epsilon .01 on a 32-block grid; stencils crossing the shoreline are excluded. This audit does not generate decorated chunks; runtime GameTests check those separately. Existing chunks are unchanged.");
        Files.writeString(output.resolve("calibration.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report) + System.lineSeparator());
        System.out.println("PASS: every sampled valley is wet and every complete lake reaches full depth.");
    }
}
