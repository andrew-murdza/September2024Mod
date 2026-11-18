package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Gentle snowy terrain over the original cheese field; round supports are applied to complete columns. */
public final class SnowyTerrainProfile implements DensityFunction {
    public static final KeyDispatchDataCodec<SnowyTerrainProfile> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(SnowyTerrainProfile::ridges),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("caverns").forGetter(SnowyTerrainProfile::caverns),
                    DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("pillars", DensityFunctions.constant(-128))
                            .forGetter(SnowyTerrainProfile::pillars),
                    Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(SnowyTerrainProfile::surfaceOnly)
            ).apply(instance, SnowyTerrainProfile::new)));
    public static final double SHORE_HEIGHT = 62.5;
    public static final double FOREST_SHORE_SLOPE = 10;
    public static final double FOREST_BANK_SLOPE = 40;
    public static final double FOREST_SLOPE = FOREST_BANK_SLOPE / 2;
    private static final double FOREST_COMPRESSION = .3 / (.4 - MountainLakeOffset.LAKE_WEIRDNESS);
    public static final double FOREST_GENTLE_START = MountainLakeOffset.LAKE_WEIRDNESS
            + (65 - SHORE_HEIGHT + 1.2) / FOREST_BANK_SLOPE / FOREST_COMPRESSION;
    public static final double FOREST_TOP = 65 + (.4 - FOREST_GENTLE_START) * FOREST_SLOPE * FOREST_COMPRESSION;
    // Twice the original underwater grade (four times the interim gentler setting).
    public static final double LAKE_SLOPE = 500;
    public static final double PLATEAU_HEIGHT = FOREST_TOP + 145 * .75 * Math.pow(0.45, 1.5);
    private final DensityFunction ridges, caverns, pillars;
    private final boolean surfaceOnly;
    private final Map<Long, Column> columns = new ConcurrentHashMap<>();

    public SnowyTerrainProfile(DensityFunction ridges, DensityFunction caverns) {
        this(ridges, caverns, false);
    }

    public SnowyTerrainProfile(DensityFunction ridges, DensityFunction caverns, boolean surfaceOnly) {
        this(ridges, caverns, DensityFunctions.constant(-128), surfaceOnly);
    }

    public SnowyTerrainProfile(DensityFunction ridges, DensityFunction caverns, DensityFunction pillars, boolean surfaceOnly) {
        this.ridges = ridges;
        this.caverns = caverns;
        this.pillars = pillars;
        this.surfaceOnly = surfaceOnly;
    }

    public DensityFunction ridges() { return ridges; }
    public DensityFunction caverns() { return caverns; }
    public DensityFunction pillars() { return pillars; }
    public boolean surfaceOnly() { return surfaceOnly; }

    /** Height depends only on the habitat contour, never its varying local derivative. */
    public static double height(double weirdness) {
        double w = Math.abs(weirdness);
        if (w < MountainLakeOffset.LAKE_WEIRDNESS) {
            double depth = Math.min(MountainLakeOffset.MAX_LAKE_DEPTH,
                    (MountainLakeOffset.LAKE_WEIRDNESS - w) * LAKE_SLOPE);
            // Ease only the last four blocks
            // into a deeper floor that remains above the underground biomes.
            double transition = MountainLakeOffset.MAX_LAKE_DEPTH - 4;
            if (depth > transition) {
                double t = (depth - transition) / 4;
                depth = transition + 4 * (t + t * t - t * t * t);
            }
            return SHORE_HEIGHT - depth;
        }
        if (w <= 0.4) {
            // Fit the original forest contour into its remaining outer half,
            // preserving both shore height and the unchanged mountain edge.
            double distance = (w - MountainLakeOffset.LAKE_WEIRDNESS) * FOREST_COMPRESSION;
            if (distance < .08) return SHORE_HEIGHT + FOREST_SHORE_SLOPE * distance
                    + (FOREST_BANK_SLOPE - FOREST_SHORE_SLOPE) * distance * distance / .16;
            if (w <= FOREST_GENTLE_START) return SHORE_HEIGHT + FOREST_BANK_SLOPE * distance - 1.2;
            return 65 + FOREST_SLOPE * (w - FOREST_GENTLE_START) * FOREST_COMPRESSION;
        }
        if (w >= 0.85) return PLATEAU_HEIGHT;
        // One broad Hermite shoulder halves the mountain rise, joins the
        // forest's grade continuously and gradually eases into the summit.
        double t = (w - 0.4) / 0.45;
        double tangent = FOREST_SLOPE * FOREST_COMPRESSION * 0.45;
        return (2 * t * t * t - 3 * t * t + 1) * FOREST_TOP
                + (t * t * t - 2 * t * t + t) * tangent
                + (-2 * t * t * t + 3 * t * t) * PLATEAU_HEIGHT;
    }

    private Column column(int x, int z) {
        if (columns.size() > 8192) columns.clear();
        return columns.computeIfAbsent(key(x, z), ignored -> {
            double w = sample(x, z);
            return new Column(height(w), Math.abs(w));
        });
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX(), y = context.blockY(), z = context.blockZ();
        Column column = column(x, z);
        double terrain = (column.height + .5 - y) / 8;
        if (surfaceOnly || terrain < 0) return terrain;
        // Keep the four-block mud/clay bed above ocean caves. The post-noise
        // column pass also enforces this cover after interpolation at shorelines.
        if (column.height < SHORE_HEIGHT && y > column.height - SnowyOceanFloor.COVER_BLOCKS) return terrain;
        // Allow existing cheese chambers to daylight along a few horizontal strata on the
        // snowy hillside. Keep the original near-surface resistance on flat banks and summits.
        double hillside = Math.max(0, Math.min(1, Math.min((column.height - 71) / 4,
                (PLATEAU_HEIGHT - 2 - column.height) / 4)));
        double shelf = Math.max(0, 1 - Math.abs(Math.floorMod(y - 80 + 8, 16) - 8) / 5.0);
        double resistance = .5 - .40 * hillside * shelf;
        double cave = caverns.compute(context) + Math.max(0, Math.min(resistance, 1.5 - .64 * terrain));
        double density = Math.min(terrain, cave);
        // Native noise pillars are deliberately absent: their thin intersecting
        // sheets are replaced by circular, unbroken supports after ceiling lifts.
        // Vanilla's lower slide, translated to this dimension's -96 floor.
        double bottom = Math.max(0, Math.min(1, (y + 96) / 24.0));
        return .1171875 * (1 - bottom) + density * bottom;
    }

    private double sample(int x, int z) { return ridges.compute(new SinglePointContext(x, 0, z)); }
    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private record Column(double height, double weirdness) { }

    @Override public void fillArray(double[] values, ContextProvider context) { context.fillAllDirectly(values, this); }
    @Override public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new SnowyTerrainProfile(ridges.mapAll(visitor), caverns.mapAll(visitor), pillars.mapAll(visitor), surfaceOnly));
    }
    @Override public double minValue() { return -128; }
    @Override public double maxValue() { return 128; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
