package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Samples actual interpolated generator columns, rather than only the un-interpolated density formula. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CaveSupportTests {
    private static final long[] SEEDS = {12345L, 98765L, -8152788754161584091L};

    private CaveSupportTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void stackedRampsMatchAcrossThePortalAndOpenIntoBothUpperHabitats(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        ServerLevel desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var lowerGenerator = (NoiseBasedChunkGenerator) nether.getChunkSource().getGenerator();
        var upperGenerator = (NoiseBasedChunkGenerator) desert.getChunkSource().getGenerator();
        for (long seed : SEEDS) {
            var lowerRandom = RandomState.create(lowerGenerator.generatorSettings().value(),
                    nether.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var upperRandom = RandomState.create(upperGenerator.generatorSettings().value(),
                    desert.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var lowerTerrain = NetherRampDecorationGuard.terrain(lowerRandom).orElseThrow();
            var upperTerrain = NetherRampDecorationGuard.terrain(upperRandom).orElseThrow();
            int seamColumns = 0, deepDarkMouths = 0, desertMouths = 0;
            for (int x = 0; x <= 616; x++) {
                int z = displacedCenter(lowerTerrain, x, DimensionTerrain.STAIR_BAND_SPACING / 2);
                var point = new DensityFunction.SinglePointContext(x, 0, z);
                helper.assertTrue(lowerTerrain.rampDetail().compute(point) == upperTerrain.rampDetail().compute(point)
                                && lowerTerrain.rampBasin().compute(point) == upperTerrain.rampBasin().compute(point),
                        "The two dimensions must share exactly seeded ramp fields at their portal seam");
                double floor = DimensionTerrain.netherRampFloor(x, lowerTerrain.rampBasin().compute(point));
                if (floor > 119 && floor < 126) {
                    var lower = lowerGenerator.getBaseColumn(x, z, nether, lowerRandom);
                    var upper = upperGenerator.getBaseColumn(x, z, desert, upperRandom);
                    helper.assertTrue(lower.getBlock(127).isAir() && upper.getBlock(-32).isAir(),
                            "A walkable ramp must cross both sides of the seamless portal at " + x + "," + z);
                    seamColumns++;
                }
                double localFloor = floor - 160;
                double deepFloor = -26 + DimensionTerrain.galleryRelief(upperTerrain.rampDetail().compute(point));
                boolean deepMouth = Math.abs(localFloor - deepFloor) < .35;
                boolean desertMouth = localFloor > 11.5;
                if (!deepMouth && !desertMouth) continue;
                for (int offset : deepMouth ? new int[]{-65, 65} : new int[]{20}) {
                    int mouthZ = displacedCenter(lowerTerrain, x, DimensionTerrain.STAIR_BAND_SPACING / 2 + offset);
                    int previousFoot = Integer.MIN_VALUE;
                    int direction = Integer.signum(mouthZ - z);
                    for (int pathZ = z; pathZ != mouthZ + direction; pathZ += direction) {
                        var mouth = upperGenerator.getBaseColumn(x, pathZ, desert, upperRandom);
                        int foot = Integer.MIN_VALUE;
                        // The wider mouth follows the seeded floor's gentle
                        // lateral relief. Test a walkable path with support and
                        // headroom, rather than requiring a perfectly level slit.
                        for (int y = (int) Math.floor(localFloor) - 3; y <= (int) Math.ceil(localFloor) + (desertMouth ? 16 : 6); y++) {
                            if (mouth.getBlock(y - 1).isSolid() && mouth.getBlock(y).isAir() && mouth.getBlock(y + 1).isAir()) {
                                foot = y;
                                break;
                            }
                        }
                        helper.assertTrue(foot != Integer.MIN_VALUE && (previousFoot == Integer.MIN_VALUE || Math.abs(foot - previousFoot) <= 1),
                                "The staircase needs a supported walkable side opening at " + x + "," + pathZ + " seed=" + seed
                                        + " previousFeet=" + previousFoot + " feet=" + foot + " rampFloor=" + localFloor
                                        + " deepMouth=" + deepMouth + " desertMouth=" + desertMouth);
                        previousFoot = foot;
                    }
                }
                if (deepMouth) deepDarkMouths++;
                if (desertMouth) desertMouths++;
            }
            helper.assertTrue(seamColumns >= 16 && deepDarkMouths > 0 && desertMouths > 0,
                    "Each ramp cycle must cross the seam and open into both Deep Dark and Desert");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void wideStairMarginsReplaceDeadZonesAndOpenBroadRooms(GameTestHelper helper) {
        for (var key : java.util.List.of(GameplayDimensions.NETHER, GameplayDimensions.DESERT)) {
            ServerLevel level = helper.getLevel().getServer().getLevel(key);
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            int galleryY = key.equals(GameplayDimensions.NETHER) ? 52 : -16;
            int openFormerDeadZone = 0, checked = 0, rockMargins = 0;
            for (long seed : SEEDS) {
                var random = RandomState.create(generator.generatorSettings().value(),
                        level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
                var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
                for (int x = -616; x <= 616; x += 16) for (int stripe = -2; stripe <= 2; stripe++) {
                    int formerZ = displacedCenter(terrain, x, stripe * DimensionTerrain.STAIR_BAND_SPACING);
                    if (generator.getBaseColumn(x, formerZ, level, random).getBlock(galleryY).isAir()) openFormerDeadZone++;
                    checked++;
                    for (int offset : new int[]{-28, 28}) {
                        int z = displacedCenter(terrain, x, stripe * DimensionTerrain.STAIR_BAND_SPACING + DimensionTerrain.STAIR_BAND_SPACING / 2 + offset);
                        if (!generator.getBaseColumn(x, z, level, random).getBlock(galleryY).isAir()) rockMargins++;
                    }
                }
            }
            helper.assertTrue(openFormerDeadZone > checked * .80,
                    "Former dead zones must be open galleries except for isolated pillars: " + openFormerDeadZone + "/" + checked);
            helper.assertTrue(rockMargins > checked * 1.5,
                    "New mining margins must extend well beyond the previous 18-block half-width");
        }
        helper.succeed();
    }

    private static int displacedCenter(DimensionTerrain terrain, int x, double center) {
        int z = (int) Math.round(center);
        for (int iteration = 0; iteration < 8; iteration++) {
            z = (int) Math.round(center - terrain.rampDetail().compute(new DensityFunction.SinglePointContext(x, 0, z)) * 18);
        }
        return z;
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void netherAndDeepDarkRetainBroadRockAndFloorToCeilingPillars(GameTestHelper helper) {
        for (var dimension : java.util.List.of(GameplayDimensions.NETHER, GameplayDimensions.DESERT)) {
            ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            boolean nether = dimension.equals(GameplayDimensions.NETHER);
            int bottom = nether ? 2 : -31, top = nether ? 126 : -1, middle = nether ? 52 : -16;
            int sampled = 0, open = 0, solid = 0, broad = 0, pillars = 0;
            for (long seed : SEEDS) {
                var random = RandomState.create(generator.generatorSettings().value(),
                        level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
                Map<Long, NoiseColumn> columns = new HashMap<>();
                for (int x = -288; x <= 288; x += 24) for (int z = -288; z <= 288; z += 24) {
                    NoiseColumn center = column(level, generator, random, columns, x, z);
                    sampled++;
                    if (center.getBlock(middle).isAir()) open++;
                    if (!solidThrough(center, bottom, top)) continue;
                    solid++;
                    int openNeighbors = 0, solidNeighbors = 0;
                    for (int[] offset : new int[][]{{8, 0}, {-8, 0}, {0, 8}, {0, -8}}) {
                        NoiseColumn neighbor = column(level, generator, random, columns, x + offset[0], z + offset[1]);
                        if (solidThrough(neighbor, bottom, top)) solidNeighbors++;
                    }
                    // A14-block probe clears the isolated7..11-block support pillars.
                    for (int[] offset : new int[][]{{14, 0}, {-14, 0}, {0, 14}, {0, -14}}) {
                        if (column(level, generator, random, columns, x + offset[0], z + offset[1])
                                .getBlock(middle).isAir()) openNeighbors++;
                    }
                    // A sixteen-block cross fits within either31-block mining margin.
                    if (solidNeighbors == 4) broad++;
                    // Supports with a room on every side must still join the lower and upper rock layers.
                    if (openNeighbors == 4) pillars++;
                }
            }
            helper.assertTrue(open > sampled * .45, dimension.location() + " must retain substantial open galleries");
            helper.assertTrue(solid > sampled / 12, dimension.location() + " needs substantial continuous support rock");
            helper.assertTrue(broad > sampled / 100, dimension.location() + " needs broad mineable solid areas");
            helper.assertTrue(pillars > 0, dimension.location() + " needs isolated floor-to-ceiling pillars among its rooms");
            AOEMod.LOGGER.info("Cave support audit {}: samples={}, open={}, continuous={}, broad={}, isolated pillars={}",
                    dimension.location(), sampled, open, solid, broad, pillars);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void netherRampsHaveRockWallsAndRoofsBetweenGalleryEntrances(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int checked = 0;
        for (long seed : SEEDS) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            DimensionTerrain terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            for (int x = -616; x <= 616; x += 8) {
                int z = (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2);
                for (int iteration = 0; iteration < 8; iteration++) {
                    z = (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2 - terrain.rampDetail().compute(new DensityFunction.SinglePointContext(x, 0, z)) * 18);
                }
                double ramp = DimensionTerrain.netherRampFloor(x,
                        terrain.rampBasin().compute(new DensityFunction.SinglePointContext(x, 0, z)));
                // These spans lie well above a gallery floor and below the next floor, away from entry mouths.
                if (!(ramp > 50 && ramp < 57 || ramp > 82 && ramp < 89)) continue;
                int bodyY = (int) Math.ceil(ramp) + 2;
                var center = generator.getBaseColumn(x, z, level, random);
                helper.assertTrue(center.getBlock(bodyY).isAir(), "Ramp passage was filled at " + x + "," + bodyY + "," + z);
                for (int offset : new int[]{-11, 11}) {
                    var wall = generator.getBaseColumn(x, z + offset, level, random);
                    for (int dy = 0; dy < 3; dy++) helper.assertTrue(wall.getBlock(bodyY + dy).isSolid(),
                            "Ramp side is exposed at " + x + "," + (bodyY + dy) + "," + (z + offset) + " seed " + seed);
                }
                helper.assertTrue(center.getBlock((int) Math.ceil(ramp) + 11).isSolid(),
                        "Ramp lacks a rock roof at " + x + "," + z + " seed " + seed);
                for (int y = 32; y < 127; y++) helper.assertFalse(center.getBlock(y).is(Blocks.LAVA),
                        "Lava must stay out of the three upper Nether habitats");
                checked++;
            }
        }
        helper.assertTrue(checked >= 30, "The enclosure test must inspect substantial uphill and downhill ramp lengths");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void exportGeneratedCaveCrossSections(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        ServerLevel desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var netherGenerator = (NoiseBasedChunkGenerator) nether.getChunkSource().getGenerator();
        var desertGenerator = (NoiseBasedChunkGenerator) desert.getChunkSource().getGenerator();
        var netherRandom = RandomState.create(netherGenerator.generatorSettings().value(),
                nether.registryAccess().lookupOrThrow(Registries.NOISE), SEEDS[0]);
        var desertRandom = RandomState.create(desertGenerator.generatorSettings().value(),
                desert.registryAccess().lookupOrThrow(Registries.NOISE), SEEDS[0]);
        var terrain = NetherRampDecorationGuard.terrain(netherRandom).orElseThrow();
        BufferedImage picture = new BufferedImage(1700, 1300, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = picture.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(new Color(0xf5f4f1));
            graphics.fillRect(0, 0, picture.getWidth(), picture.getHeight());
            graphics.setColor(new Color(0x272b36));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            graphics.drawString("Generated cave cross-sections", 90, 45);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            graphics.drawString("Actual interpolated terrain columns • Seed 12345 • Before surface materials and decoration", 90, 76);
            legend(graphics, 90, 97, new Color(0x777588), "Solid terrain");
            legend(graphics, 265, 97, new Color(0xe4ebf0), "Open cave");
            legend(graphics, 430, 97, new Color(0xeaa04b), "Lava");

            NoiseColumn[] along = new NoiseColumn[768];
            NoiseColumn[] across = new NoiseColumn[768];
            NoiseColumn[] deepDark = new NoiseColumn[768];
            int crossX = 72;
            for (int offset = 0; offset < along.length; offset++) {
                int x = offset - 192;
                int z = (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2);
                for (int iteration = 0; iteration < 8; iteration++) {
                    z = (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2 - terrain.rampDetail().compute(new DensityFunction.SinglePointContext(x, 0, z)) * 18);
                }
                var lower = netherGenerator.getBaseColumn(x, z, nether, netherRandom);
                var upper = desertGenerator.getBaseColumn(x, z, desert, desertRandom);
                var stacked = new net.minecraft.world.level.block.state.BlockState[192];
                for (int y = 0; y < stacked.length; y++) stacked[y] = y < 128 ? lower.getBlock(y) : upper.getBlock(y - 160);
                along[offset] = new NoiseColumn(0, stacked);
                across[offset] = netherGenerator.getBaseColumn(crossX, offset - 192, nether, netherRandom);
                deepDark[offset] = desertGenerator.getBaseColumn(x, (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2), desert, desertRandom);
            }
            slice(graphics, along, 90, 160, 0, 191, 2,
                    "Nether · following the ramp through the seamless stack", "X −192 to 575; Z follows the ramp", -192);
            slice(graphics, across, 90, 650, 0, 127, 2,
                    "Nether · across galleries, rock masses, pillars and ramp banks", "Z −192 to 575; X = 72", -192);
            slice(graphics, deepDark, 90, 1018, -32, 25, 3,
                    "Deep dark · galleries within supporting rock", "X −192 to 575; Z = 112", -192);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            graphics.setColor(new Color(0x565b67));
            graphics.drawString("Top: Nether Y0..127; Desert Y-32..31 is shown at stacked Y128..191. Solid terrain is mineable; colors show geometry.", 90, 1270);
            Path output = Path.of("evidence", "nether-cave-slices.png");
            Files.createDirectories(output.getParent());
            helper.assertTrue(ImageIO.write(picture, "png", output.toFile()), "PNG image encoder must be available");
            AOEMod.LOGGER.info("Saved actual generated cave slice evidence to {}", output.toAbsolutePath());
        } catch (IOException failure) {
            throw new IllegalStateException("Could not save cave cross-section evidence", failure);
        } finally {
            graphics.dispose();
        }
        helper.succeed();
    }

    private static void legend(Graphics2D graphics, int x, int y, Color color, String label) {
        graphics.setColor(color);
        graphics.fillRect(x, y, 20, 20);
        graphics.setColor(new Color(0x272b36));
        graphics.drawString(label, x + 28, y + 17);
    }

    private static void slice(Graphics2D graphics, NoiseColumn[] columns, int left, int top,
                              int minY, int maxY, int scaleY, String title, String coordinates, int start) {
        graphics.setColor(new Color(0x272b36));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.drawString(title, left, top - 14);
        for (int x = 0; x < columns.length; x++) for (int y = minY; y <= maxY; y++) {
            var state = columns[x].getBlock(y);
            graphics.setColor(new Color(state.is(Blocks.LAVA) ? 0xeaa04b : state.isAir() ? 0xe4ebf0 : 0x777588));
            graphics.fillRect(left + x * 2, top + (maxY - y) * scaleY, 2, scaleY);
        }
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        graphics.setColor(new Color(0x565b67));
        for (int y = Math.floorDiv(maxY, 32) * 32; y >= minY; y -= 32) {
            int row = top + (maxY - y) * scaleY;
            graphics.drawString("Y " + y, left - 64, row + 5);
            graphics.drawLine(left - 8, row, left - 2, row);
        }
        int bottom = top + (maxY - minY + 1) * scaleY;
        for (int i = 0; i < columns.length; i += 192) graphics.drawString(Integer.toString(start + i), left + i * 2, bottom + 20);
        graphics.drawString(coordinates, left + 940, bottom + 42);
    }

    private static NoiseColumn column(ServerLevel level, NoiseBasedChunkGenerator generator, RandomState random,
                                       Map<Long, NoiseColumn> columns, int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        return columns.computeIfAbsent(key, ignored -> generator.getBaseColumn(x, z, level, random));
    }

    private static boolean solidThrough(NoiseColumn column, int bottom, int top) {
        for (int y = bottom; y <= top; y++) if (!column.getBlock(y).isSolid()) return false;
        return true;
    }
}
