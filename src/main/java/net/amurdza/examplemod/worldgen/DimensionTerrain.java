package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

/** Seeded terrain shared by the density router and biome selection. Heights are block coordinates. */
public final class DimensionTerrain implements DensityFunction {
    public static final double FALLING_CAVE_HALF_WIDTH = 7.5;
    // Keep the mining banks and horizontal connectors at their existing widths.
    public static final double STAIR_BANK_HALF_WIDTH = 51.5;
    public static final double STAIR_BAND_SPACING = 223;
    public static final int BASALT_LAVA_LEVEL = 11;
    public static final double STAIR_ROOM_CLEARANCE = STAIR_BANK_HALF_WIDTH + 10;
    public static final double STAIR_ENTRANCE_REACH = STAIR_BANK_HALF_WIDTH + 12;
    public static final double DESERT_OUTLET_GRADE = 0.5;
    public static final int DESERT_LAKE_CARVE_CEILING = (int) DesertHeight.BOTTOM - 3;
    public static final double DESERT_OUTLET_MAX_BEND = 192;
    private final String dimension, output;
    private final DensityFunction land, detail, river, lake, caveA, caveB, rampDetail, rampBasin;
    private volatile TropicalLayout tropicalLayout;
    private volatile SavannaLakes savannaLakes;
    private volatile SavannaLakeBasins savannaBasins;
    private volatile SavannaBands savannaBands;
    private volatile CaveSupports caveSupports;
    private volatile DesertCaveLayout desertCaveLayout;

    public DimensionTerrain(String dimension, String output, DensityFunction land, DensityFunction detail,
                            DensityFunction river, DensityFunction lake, DensityFunction caveA, DensityFunction caveB) {
        this(dimension, output, land, detail, river, lake, caveA, caveB, detail, lake);
    }

    public DimensionTerrain(String dimension, String output, DensityFunction land, DensityFunction detail,
                            DensityFunction river, DensityFunction lake, DensityFunction caveA, DensityFunction caveB,
                            DensityFunction rampDetail, DensityFunction rampBasin) {
        this.dimension = dimension; this.output = output; this.land = land; this.detail = detail;
        this.river = river; this.lake = lake; this.caveA = caveA; this.caveB = caveB;
        this.rampDetail = rampDetail; this.rampBasin = rampBasin;
    }

    public String dimension() { return dimension; }
    public String output() { return output; }
    public DensityFunction land() { return land; }
    public DensityFunction detail() { return detail; }
    public DensityFunction river() { return river; }
    public DensityFunction lake() { return lake; }
    public DensityFunction caveA() { return caveA; }
    public DensityFunction caveB() { return caveB; }
    public DensityFunction rampDetail() { return rampDetail; }
    public DensityFunction rampBasin() { return rampBasin; }

    /** The anchor is sampled after RandomState seeds child noises; no global world seed is retained. */
    public static TropicalLayout tropicalLayout(DensityFunction seededLand) {
        double first = seededLand.compute(new SinglePointContext(0, 0, 0));
        double second = seededLand.compute(new SinglePointContext(8192, 0, -8192));
        return new TropicalLayout(TropicalLayout.mix(Double.doubleToLongBits(first)
                ^ Long.rotateLeft(Double.doubleToLongBits(second), 29)));
    }

    private TropicalLayout tropicalLayout() {
        TropicalLayout layout = tropicalLayout;
        if (layout == null) { layout = tropicalLayout(land); tropicalLayout = layout; }
        return layout;
    }

    public static SavannaLakes savannaLakes(DensityFunction seededLand, DensityFunction seededDetail) {
        double first = seededLand.compute(new SinglePointContext(0, 0, 0));
        double second = seededLand.compute(new SinglePointContext(8192, 0, -8192));
        long seed = TropicalLayout.mix(Double.doubleToLongBits(first)
                ^ Long.rotateLeft(Double.doubleToLongBits(second), 29));
        return new SavannaLakes(seed, (x, z) -> {
            var point = new SinglePointContext((int) x, 0, (int) z);
            return SavannaLakes.dryHeight(seededLand.compute(point), seededDetail.compute(point));
        });
    }

    private SavannaLakes savannaLakes() {
        SavannaLakes layout = savannaLakes;
        if (layout == null) { layout = savannaLakes(land, detail); savannaLakes = layout; }
        return layout;
    }

    /** Seeded noise shapes the lake layout, never an independent dry valley floor. */
    public static SavannaLakeBasins savannaBasins(DensityFunction seededLand) {
        return new SavannaLakeBasins((x, z) -> seededLand.compute(new SinglePointContext((int) x, 0, (int) z)));
    }

    private SavannaLakeBasins savannaBasins() {
        SavannaLakeBasins layout = savannaBasins;
        if (layout == null) { layout = savannaBasins(land); savannaBasins = layout; }
        return layout;
    }

    public static SavannaBands savannaBands(DensityFunction seededLand) { return new SavannaBands(seededLand); }

    private SavannaBands savannaBands() {
        SavannaBands bands = savannaBands;
        if (bands == null) { bands = savannaBands(land); savannaBands = bands; }
        return bands;
    }

    private CaveSupports caveSupports() {
        CaveSupports supports = caveSupports;
        if (supports == null) {
            double first = rampDetail.compute(new SinglePointContext(0, 0, 0));
            double second = rampDetail.compute(new SinglePointContext(8192, 0, -8192));
            supports = new CaveSupports(TropicalLayout.mix(Double.doubleToLongBits(first)
                    ^ Long.rotateLeft(Double.doubleToLongBits(second), 29)), rampBasin);
            caveSupports = supports;
        }
        return supports;
    }

    private double caveSupport(FunctionContext context, double land, double detail) {
        return caveSupports().density(context.blockX(), context.blockZ(), land, detail) / 4;
    }

    public static final KeyDispatchDataCodec<DimensionTerrain> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("dimension").forGetter(DimensionTerrain::dimension),
                    Codec.STRING.optionalFieldOf("output", "density").forGetter(DimensionTerrain::output),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("land").forGetter(DimensionTerrain::land),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail").forGetter(DimensionTerrain::detail),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("river").forGetter(DimensionTerrain::river),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("lake").forGetter(DimensionTerrain::lake),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cave_a").forGetter(DimensionTerrain::caveA),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cave_b").forGetter(DimensionTerrain::caveB),
                    DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("ramp_detail").forGetter(terrain -> Optional.of(terrain.rampDetail)),
                    DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("ramp_basin").forGetter(terrain -> Optional.of(terrain.rampBasin))
            ).apply(instance, (dimension, output, land, detail, river, lake, caveA, caveB, rampDetail, rampBasin) ->
                    new DimensionTerrain(dimension, output, land, detail, river, lake, caveA, caveB,
                            rampDetail.orElse(detail), rampBasin.orElse(lake)))));

    @Override
    public double compute(FunctionContext context) {
        if (dimension.equals("tropical")) {
            TropicalLayout.Sample sample = tropicalLayout().sample(context.blockX(), context.blockZ());
            if (output.equals("height")) return sample.height();
            if (output.equals("biome")) return sample.biome();
            return (sample.height() + .5 - context.blockY()) / 8.0;
        }
        if (dimension.equals("savanna")) {
            double height = savannaBands().height(context.blockX(), context.blockZ());
            if (output.equals("biome")) return isSurfaceWater(height, SavannaHeight.SEA_LEVEL) ? 1 : 0;
            return output.equals("height") ? height : (height + .5 - context.blockY()) / 8;
        }
        FunctionContext column = new SinglePointContext(context.blockX(), 0, context.blockZ());
        double continent = land.compute(column);
        double roughness = detail.compute(column);
        double drainage = Math.abs(river.compute(column));
        double basin = lake.compute(column);
        double height = surfaceHeight(dimension, continent, roughness, drainage, basin);
        if (output.equals("height")) return height;
        if (output.equals("lake_guard")) {
            // Match the exact-height fluid boundary on both sides of the shore.
            // Interpolation can otherwise carve a dry bank column just below
            // sea level, where the exact fluid picker correctly supplies air.
            // A four-block roof below the deepest bed prevents water entering
            // through a cave ceiling, including after density interpolation.
            int y = context.blockY();
            boolean protect = y >= DESERT_LAKE_CARVE_CEILING && y < DesertHeight.SEA_LEVEL
                    && Mth.ceil(height + .5) <= DesertHeight.SEA_LEVEL
                    && (isSurfaceWater(height, DesertHeight.SEA_LEVEL)
                    || touchesDesertLake(land, context.blockX(), context.blockZ())
                    || desertSurfaceConnection(context) >= 1);
            return protect ? (height + .5 - context.blockY()) / 8.0 : -128;
        }
        if (output.equals("biome")) {
            if (dimension.equals("desert")) return isSurfaceWater(height, DesertHeight.SEA_LEVEL) ? 2 : 0;
            if (dimension.equals("plains")) return isSurfaceWater(height, 64) ? 1 : 0;
            // Preserve the existing ocean label for lake basins only. Ignoring
            // river carving here keeps ordinary channels in their land habitat.
            double basinHeight = surfaceHeight(dimension, continent, roughness, 1.0, basin);
            return dimension.equals("snowy") ? snowySurfaceBiome(height, basinHeight) : 0;
        }
        int y = context.blockY();
        if (dimension.equals("nether")) return nether(context, continent, roughness, basin);
        double density = (height + 0.5 - y) / 8.0;
        if (dimension.equals("snowy")) {
            // No carver may puncture the rock between the deepest lake (Y16) and this ceiling.
            if (y >= -32 && y <= -1) {
                double floor = -26 + Mth.clamp(roughness * 4, -2, 2);
                double ceiling = floor + 22 + Mth.clamp(basin * 3, -2, 2);
                ceiling = Math.min(-3, ceiling);
                density = Math.min(density, gallery(y, floor, ceiling, continent));
            } else if (y > -90 && y < -49) {
                density = Math.min(density, tunnel(context, -90, -49));
            }
        } else if (dimension.equals("desert")) {
            double rampDisplacement = rampDetail.compute(column);
            double rampPhase = rampBasin.compute(column);
            double corridor = netherRampDistance(context.blockZ(), rampDisplacement);
            double floor = -26 + galleryRelief(rampDisplacement);
            if (y >= -31 && y < 0) {
                double room = gallery(y, floor, -3 + galleryCeilingRelief(rampDisplacement, rampPhase), 0);
                double support = Math.min(caveSupport(context, continent, rampDisplacement), (corridor - STAIR_ROOM_CLEARANCE) / 4);
                // Large caverns follow T extrema; broad rock banks around T=0
                // enclose the independently curved descending connectors.
                double terraceBank = (DesertCaveLayout.CAVERN_THRESHOLD - Math.abs(continent)) * 20;
                density = Math.min(density, Math.max(Math.max(room, support), terraceBank));
            }
            boolean lakeColumn = isSurfaceWater(height, DesertHeight.SEA_LEVEL);
            if (y < 28) {
                // The persistent portal maps Nether Y128 to Desert Y-32 without
                // shifting X/Z. Both routers use identical seeded ramp fields.
                double rampFloor = netherRampFloor(context.blockX(), rampPhase) - 160;
                // Retain solid support around the actual legacy ramp, not its
                // former infinite bank stripe through both new T caverns.
                double legacyBank = Math.min((STAIR_BANK_HALF_WIDTH - corridor) / 4,
                        Math.min(y - rampFloor + 8, rampFloor + 14 - y) / 4);
                density = Math.max(density, legacyBank);
                density = carveRamp(density, y, corridor, rampFloor, floor);
                // Preserve the Nether seam and extend only its low gallery
                // landings through the relocated T banks into the caverns.
                double oldLanding = Math.max(Math.max(floor + .5 - y, y - floor - 9.5),
                        Math.max(corridor - 110, (Math.abs(rampFloor - floor) - 4) * 4)) / 4;
                density = Math.min(density, oldLanding);
            }
            if (y < 0) density = Math.min(density, desertSurfaceConnection(context));
            // Cancel only the individual high lake blocks; the same passage
            // remains continuous underneath the lake and in adjacent dry land.
            if (y >= 0 && (!lakeColumn || y < DESERT_LAKE_CARVE_CEILING)) {
                density = Math.min(density, desertSurfaceConnection(context));
            }
        } else if (y > -60 && y < 40) {
            density = Math.min(density, tunnel(context, -60, 40));
        }
        return density;
    }

    /** Continuous banks avoid vertical water walls at lake and river edges. */
    public static double surfaceHeight(String dimension, double land, double detail, double river, double lake) {
        double height;
        switch (dimension) {
            case "plains" -> {
                height = Math.max(65, 74 + land * 19 + detail * 4);
                height = Mth.lerp(1 - smooth(0.020, 0.060, river), height, 57);
                return Mth.lerp(smooth(0.35, 0.51, lake), height, 51 + Mth.clamp(detail * 5, -3, 3));
            }
            case "snowy" -> {
                height = Math.max(101, 111 + Math.max(0, land) * 82 + detail * 9);
                double basinFloor = 48 - 24 * smooth(0.34, 0.64, lake) - 8 * smooth(0.70, 0.85, lake);
                height = Mth.lerp(smooth(0.06, 0.34, lake), height, basinFloor);
                height = Math.min(height, Mth.lerp(1 - smooth(0.014, 0.050, river), height, 84));
                return Math.max(16, height);
            }
            case "desert" -> {
                return DesertHeight.height(land, detail);
            }
            default -> { return 128; }
        }
    }

    /** The first air block must be below sea level for a column to contain surface water. */
    public static boolean isSurfaceWater(double height, int seaLevel) {
        return Mth.ceil(height + .5) < seaLevel;
    }

    public static boolean touchesDesertLake(DensityFunction land, int x, int z) {
        return land.compute(new SinglePointContext(x - 1, 0, z)) <= DesertHeight.WATER_EDGE_NOISE
                || land.compute(new SinglePointContext(x + 1, 0, z)) <= DesertHeight.WATER_EDGE_NOISE
                || land.compute(new SinglePointContext(x, 0, z - 1)) <= DesertHeight.WATER_EDGE_NOISE
                || land.compute(new SinglePointContext(x, 0, z + 1)) <= DesertHeight.WATER_EDGE_NOISE;
    }

    /** Surface habitat indices used by the snowy biome source, independent of query altitude. */
    public static int snowySurfaceBiome(double height, double basinHeight) {
        // Ocean water ends at Y95. Its original basin-only predicate excludes
        // river channels; dry banks and foothills are dappled forest, with
        // snowy taiga on the upper slopes of the same seeded height field.
        if (Mth.ceil(basinHeight + 0.5) < 96) return 2;
        return height < 120 ? 3 : 0;
    }

    /** Sea level 64 contains water through Y63; bed Y57 therefore has exactly six water blocks. */
    public static int tropicalWaterDepth(double height) {
        return Math.max(0, 64 - Mth.ceil(height + 0.5));
    }

    private double tunnel(FunctionContext context, int bottom, int top) {
        double tubes = Math.max(Math.abs(caveA.compute(context)) - 0.12,
                Math.abs(caveB.compute(context)) - 0.14) * 32;
        return Math.max(tubes, Math.max(bottom + 2 - context.blockY(), context.blockY() - top + 2));
    }

    private static double gallery(int y, double floor, double ceiling, double pillars) {
        double room = Math.max(floor + 0.5 - y, y - ceiling + 0.5);
        // Broad rooms remain linked around seeded support pillars rather than a single hollow slab.
        return Math.max(room, (pillars - 0.56) * 26) / 4;
    }

    private double nether(FunctionContext context, double land, double detail, double basin) {
        int y = context.blockY();
        if (y <= 1) return 1;
        int band = Math.floorDiv(y, 32);
        double floor = band * 32 + 6 + galleryRelief(detail);
        double corridor = netherRampDistance(context.blockZ(), detail);
        if (band == 0) {
            floor = basaltFloor(context.blockX(), corridor, basin, detail);
        }
        // A second shared seeded contour varies the room height gently; both
        // surfaces stay within their stratum without hard, flat clipping.
        double ceiling = band * 32 + 29 + galleryCeilingRelief(detail, basin);
        double density = y >= 127 ? 1 : gallery(y, floor, ceiling, 0);
        // The bank and surrounding gallery retain their existing dimensions.
        double rock = Math.min(caveSupport(context, land, detail), (corridor - STAIR_ROOM_CLEARANCE) / 4);
        density = Math.max(density, rock);
        // A broad, full-height rock bank encloses the ramp's sides and roof as well as its floor.
        density = Math.max(density, (STAIR_BANK_HALF_WIDTH - corridor) / 4);
        double rampFloor = netherRampFloor(context.blockX(), basin);
        return carveRamp(density, y, corridor, rampFloor, floor);
    }

    public static double galleryRelief(double detail) {
        // Keep the same central grade and two-block bounds without turning
        // every noise peak past +/-0.5 into a perfectly level terrace.
        return 2 * Math.tanh(detail * 2);
    }

    public static double galleryCeilingRelief(double detail, double basin) {
        return galleryRelief(.65 * detail + .65 * basin);
    }

    /** A continuous river at the middle of every gallery, with dry banks on both sides. */
    public static double basaltFloor(int x, double corridor, double basin, double detail) {
        double fromRiver = STAIR_BAND_SPACING / 2 - corridor;
        // Y0 bedrock + Y1..2 rock leave exactly eight source blocks at maximum depth.
        double beach = Math.min(14 + galleryRelief(detail), 2 + Math.max(0, fromRiver - 2) * 0.4);
        double rampFloor = netherRampFloor(x, basin);
        // The transverse landing continues as a bridge across the river. Its
        // steeper sides retain the shallow grade of the ordinary side beaches.
        double bridge = 1 - smooth(3, 8, Math.abs(rampFloor - 18));
        return Mth.lerp(bridge, beach, Math.max(beach, BASALT_LAVA_LEVEL + 1));
    }

    public DesertCaveLayout desertCaves() {
        DesertCaveLayout layout = desertCaveLayout;
        if (layout == null) { layout = new DesertCaveLayout(this); desertCaveLayout = layout; }
        return layout;
    }

    private double desertSurfaceConnection(FunctionContext context) {
        return desertCaves().density(context.blockX(), context.blockY(), context.blockZ());
    }

    /** Newly exposed connector rock stops below the original desert surface. */
    public boolean isDesertEntranceRock(int x, int y, int z) {
        return dimension.equals("desert") && y >= 16
                && desertSurfaceConnection(new SinglePointContext(x, y, z)) < .75
                && y < desertUncarvedSurfaceY(x, z) && desertCaves().surfaceWasCut(x, z);
    }

    /** Match the router's four-block interpolation grid before the outlet is subtracted. */
    public int desertUncarvedSurfaceY(int x, int z) {
        int cellX = Math.floorDiv(x, 4) * 4, cellZ = Math.floorDiv(z, 4) * 4;
        double height = Mth.lerp2((x - cellX) / 4.0, (z - cellZ) / 4.0,
                desertSurfaceHeight(cellX, cellZ), desertSurfaceHeight(cellX + 4, cellZ),
                desertSurfaceHeight(cellX, cellZ + 4), desertSurfaceHeight(cellX + 4, cellZ + 4));
        // The density is positive only below height + .5. Using the exact
        // interpolated contour protects intact sand/terracotta at every rim,
        // including negative coordinates and steep terracotta terraces.
        return Mth.ceil(height + .5) - 1;
    }

    private double desertSurfaceHeight(int x, int z) {
        var column = new SinglePointContext(x, 0, z);
        return surfaceHeight("desert", land.compute(column), detail.compute(column), 0, 0);
    }

    /** Stable route indices retain one pair of patches per terrace-side descent. */
    public DesertOutlet desertOutlet(int cycle, int stripe) {
        return new DesertOutlet(desertCaves().route(cycle, stripe));
    }

    public static double desertOutletFloor(double distance) { return DesertCaveLayout.floor(distance); }

    public static final class DesertOutlet {
        private final DesertCaveLayout.Route route;
        private DesertOutlet(DesertCaveLayout.Route route) { this.route = route; }
        public double x() { return route.xAt(0); }
        public double z() { return route.zAt(0); }
        public int length() { return route.length(); }
        public int entranceStart() { return DesertCaveLayout.ENTRANCE_START; }
        public double floorAt(double distance) { return route.floorAt(distance); }
        public double bendA() { return 0; }
        public double bendB() { return 0; }
        public double xAt(double distance) { return route.xAt(distance); }
        public double zAt(double distance) { return route.zAt(distance); }
        public double headingAt(double distance) { return route.headingAt(distance); }
        public double density(int x, int y, int z) { return route.density(x, y, z); }
    }

    private static double carveRamp(double density, int y, double corridor, double rampFloor, double galleryFloor) {
        double ramp = Math.max(Math.max(rampFloor + 0.5 - y, y - rampFloor - 9.5), corridor - FALLING_CAVE_HALF_WIDTH) / 4;
        density = Math.max(density, rampEntranceSupport(y, corridor, rampFloor, galleryFloor));
        return Math.min(Math.min(density, ramp), rampEntrance(y, corridor, rampFloor, galleryFloor));
    }

    private static double rampEntrance(int y, double corridor, double rampFloor, double galleryFloor) {
        double floor = entranceFloor(corridor, rampFloor, galleryFloor);
        return Math.max(Math.max(floor + 0.5 - y, y - floor - 9.5),
                // Leave room for the lateral floor relief and the4-block density
                // interpolation cell, so a mouth stays connected across its bank.
                Math.max(corridor - STAIR_ENTRANCE_REACH, (Math.abs(rampFloor - galleryFloor) - 8) * 4)) / 4;
    }

    private static double entranceFloor(double corridor, double rampFloor, double galleryFloor) {
        return Mth.lerp(smooth(STAIR_BANK_HALF_WIDTH - 2, STAIR_ENTRANCE_REACH - 2, corridor), rampFloor, galleryFloor);
    }

    private static double rampEntranceSupport(int y, double corridor, double rampFloor, double galleryFloor) {
        double floor = entranceFloor(corridor, rampFloor, galleryFloor);
        // More than one four-block density cell keeps the support intact after
        // interpolation even where the landing rises between sample planes.
        return Math.min(Math.min(floor + .5 - y, y - floor + 6.5),
                Math.min(STAIR_ENTRANCE_REACH - corridor, (8 - Math.abs(rampFloor - galleryFloor)) * 4)) / 4;
    }

    /** Shared with the seeded fluid picker, including the same detail-field displacement. */
    public static double netherRampDistance(int z, double detail) {
        double displaced = z + detail * 18;
        return Math.abs(displaced - STAIR_BAND_SPACING * Math.floor(displaced / STAIR_BAND_SPACING) - STAIR_BAND_SPACING / 2);
    }

    public static double netherRampFloor(int x, double basin) {
        double phase = (x + basin * 32) / 616.0;
        phase -= Math.floor(phase);
        // A half-block grade continues through all four Nether floors, the
        // unscaled portal seam, Deep Dark and the Desert Y0 boundary.
        return 18 + 154 * (1 - Math.abs(2 * phase - 1));
    }

    private static double smooth(double low, double high, double value) {
        double t = Mth.clamp((value - low) / (high - low), 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override public void fillArray(double[] values, ContextProvider context) { context.fillAllDirectly(values, this); }
    @Override public double minValue() { return -128; }
    @Override public double maxValue() { return 384; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        // Visiting every child is essential: RandomState installs world-seeded NormalNoise instances here.
        return visitor.apply(new DimensionTerrain(dimension, output, land.mapAll(visitor), detail.mapAll(visitor),
                river.mapAll(visitor), lake.mapAll(visitor), caveA.mapAll(visitor), caveB.mapAll(visitor),
                rampDetail.mapAll(visitor), rampBasin.mapAll(visitor)));
    }
}
