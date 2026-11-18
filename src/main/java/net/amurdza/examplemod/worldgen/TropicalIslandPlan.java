package net.amurdza.examplemod.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Seeded island-wide quotas, independent of the order in which chunks decorate. */
public final class TropicalIslandPlan {
    public record Chunk(int x, int z) { }
    private record Column(int x, int z) { }
    private record Key(long seed, long island) { }
    private static final Map<Key, TropicalIslandPlan> CACHE = new LinkedHashMap<>(64, .75F, true);
    private static final List<String> FOREST_RESERVED = List.of("minecraft:red_mushroom", "minecraft:brown_mushroom",
            "minecraft:melon", "farmersdelight:wild_tomatoes", "pineapple_delight:pineapple_wild_crop",
            "minecraft:azalea", "minecraft:flowering_azalea");
    // Taking the remainder in this order implements the requested tie breaker.
    private static final List<String> FOREST_FLOWERS = List.of("environmental:blue_delphinium", "minecraft:rose_bush",
            "minecraft:torchflower", "environmental:purple_delphinium");
    private static final List<String> MOUND_RESERVED = List.of("minecraft:red_mushroom", "minecraft:brown_mushroom",
            "farmersdelight:wild_cabbages", "hearthandharvest:wild_cotton");
    // Scale the old fixed quotas by the area of each concentric habitat.
    // Per-chunk flower draws already scale with the number of chunks.
    // The previous height-based biome switch extended rainforest past its nominal radius
    // of 114 until the dry-bank profile crossed Y63.5 (smoothstep(t) = 0.3).
    private static final double OLD_FOREST_EDGE = 114 + 26 * (.5 - Math.sin(Math.asin(.4) / 3));
    private static final double FOREST_AREA_RATIO = areaRatio(TropicalLayout.CHERRY_RADIUS, TropicalLayout.FOREST_RADIUS, 72, OLD_FOREST_EDGE);
    private static final double GROVE_AREA_RATIO = areaRatio(0, TropicalLayout.CHERRY_RADIUS, 0, 72);
    private static final double BANK_AREA_RATIO = areaRatio(TropicalLayout.FOREST_RADIUS, TropicalLayout.MANGROVE_LAND_RADIUS, OLD_FOREST_EDGE, 140);
    private static final double WATER_AREA_RATIO = areaRatio(TropicalLayout.MANGROVE_LAND_RADIUS, TropicalLayout.ISLAND_RADIUS, 140, 156);
    private static final int MUSHROOM_PATCHES = scaled(4, FOREST_AREA_RATIO);
    private static final int FOREST_CROP_PATCHES = scaled(3, FOREST_AREA_RATIO);
    private static final int AZALEA_PATCHES = scaled(4, FOREST_AREA_RATIO);
    private static final int GROVE_CROP_PATCHES = scaled(3, GROVE_AREA_RATIO);
    private static final int FOREST_RESERVED_COUNT = 2 * MUSHROOM_PATCHES + 3 * FOREST_CROP_PATCHES
            + 2 * AZALEA_PATCHES;
    private static final int BANK_PATCHES = scaled(4, BANK_AREA_RATIO);
    private static final int WET_CROP_PATCHES = scaled(10, WATER_AREA_RATIO);

    private static double areaRatio(double inner, double outer, double oldInner, double oldOuter) {
        return (outer * outer - inner * inner) / (oldOuter * oldOuter - oldInner * oldInner);
    }

    private static int scaled(int quota, double ratio) {
        return (int) Math.round(quota * ratio);
    }

    private final List<Chunk> rainforest, cherry, mounds, shallow;
    private final List<Chunk> wetCropChunks;
    private final Set<Column> wetCropRoots = new HashSet<>(), wetCropClearance = new HashSet<>();
    private final Map<Chunk, Integer> pickerelweed;
    private final long dripleafSalt;

    private TropicalIslandPlan(TropicalLayout layout, TropicalLayout.Island island) {
        dripleafSalt = island.hash() ^ 0xCBBB9D5DC1059ED8L;
        List<Chunk> forest = new ArrayList<>(), grove = new ArrayList<>(), mound = new ArrayList<>(), wet = new ArrayList<>();
        List<Chunk> legacyWet = new ArrayList<>();
        int pickerelThirds = 0;
        int reach = (int) Math.ceil(TropicalLayout.ISLAND_RADIUS) + 16;
        for (int x = Math.floorDiv((int) Math.floor(island.x()) - reach, 16);
             x <= Math.floorDiv((int) Math.ceil(island.x()) + reach, 16); x++) {
            for (int z = Math.floorDiv((int) Math.floor(island.z()) - reach, 16);
                 z <= Math.floorDiv((int) Math.ceil(island.z()) + reach, 16); z++) {
                var sample = layout.sample(x * 16 + 8, z * 16 + 8, island);
                Chunk chunk = new Chunk(x, z);
                if (sample.biome() == 2) forest.add(chunk);
                if (sample.biome() == 3 && sample.distance() <= sample.cherryEdge() - 8) grove.add(chunk);
                boolean isShore = sample.distance() >= TropicalLayout.FOREST_RADIUS + 6
                        && sample.distance() <= TropicalLayout.MANGROVE_LAND_RADIUS - 5;
                if (isShore) mound.add(chunk);
                // Include boundary chunks with actual one-deep shore water,
                // even when their centers are dry or farther offshore.
                if (Math.abs(sample.distance() - (TropicalLayout.MANGROVE_LAND_RADIUS + 6)) < 18
                        && shallowCells(layout, island, x, z) >= 5) wet.add(chunk);
                if (sample.distance() >= sample.forestEdge() - 12 && sample.distance() <= sample.mangroveEdge() + 12) {
                    int oldHabitat = legacyWetHabitat(layout, island, x, z);
                    if (oldHabitat > 0) {
                        legacyWet.add(chunk);
                        pickerelThirds += oldHabitat == 2 ? 2 : 6;
                    }
                }
            }
        }
        rainforest = ordered(forest, island.hash() ^ 0x6A09E667F3BCC909L);
        cherry = ordered(grove, island.hash() ^ 0xBB67AE8584CAA73BL);
        mounds = ordered(mound, island.hash() ^ 0x3C6EF372FE94F82BL);
        List<Chunk> shallowOrder = new ArrayList<>(ordered(wet, island.hash() ^ 0xA54FF53A5F1D36F1L));
        // Prefer dry-shore chunks that also contain shallow planting cells.
        // The expanded strip separates this shoreline from the rainforest seam.
        shallowOrder.sort(Comparator.comparing(chunk -> !mounds.contains(chunk)));
        shallow = List.copyOf(shallowOrder);
        List<Chunk> crops = new ArrayList<>(WET_CROP_PATCHES * 2);
        for (Chunk chunk : shallow) {
            var roots = wetCropFootprint(layout, island, chunk);
            if (roots.isEmpty()) continue;
            crops.add(chunk);
            wetCropRoots.addAll(roots);
            for (Column root : roots) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
                wetCropClearance.add(new Column(root.x + dx, root.z + dz));
            if (crops.size() == WET_CROP_PATCHES * 2) break;
        }
        wetCropChunks = List.copyOf(crops);
        List<Chunk> deep = ordered(legacyWet.stream().filter(chunk -> !shallow.contains(chunk)).collect(java.util.stream.Collectors.toCollection(ArrayList::new)),
                island.hash() ^ 0x510E527FADE682D1L);
        // First retain the established allocation: the former two draws yielded
        // 2/3 patches in shallow chunks and two in deep-only chunks, with three
        // quarters of that island total relocated to emergent shorelines.
        int total = pickerelThirds / 3;
        if (Math.floorMod(TropicalLayout.mix(island.hash() ^ 0x1F83D9ABFB41BD6BL), 3) < pickerelThirds % 3) total++;
        int emergent = (total * 3 + 2) / 4;
        Map<Chunk, Integer> attempts = new java.util.HashMap<>();
        distribute(attempts, shallow.isEmpty() ? deep : shallow, emergent);
        distribute(attempts, deep.isEmpty() ? shallow : deep, total - emergent);
        // Keep the first shallow reduction, then halve its result again. Each
        // stage rounds odd counts with an independent seeded coin, retaining
        // one quarter of the original shoreline expectation overall.
        // Keeping each deep entry untouched preserves both its amount and place;
        // removed shoreline attempts are never transferred into deeper water.
        attempts.replaceAll((chunk, count) -> {
            if (!shallow.contains(chunk)) return count;
            long rounding = TropicalLayout.mix(island.hash() ^ 0x9B05688C2B3E6C1FL
                    ^ chunk.x * 341873128712L ^ chunk.z * 132897987541L);
            int previous = count / 2 + (count % 2 != 0 && (rounding & 1) != 0 ? 1 : 0);
            long secondRounding = TropicalLayout.mix(island.hash() ^ 0x5BE0CD19137E2179L
                    ^ chunk.x * 341873128712L ^ chunk.z * 132897987541L);
            return previous / 2 + (previous % 2 != 0 && (secondRounding & 1) != 0 ? 1 : 0);
        });
        pickerelweed = Map.copyOf(attempts);
    }

    public static synchronized TropicalIslandPlan forIsland(TropicalLayout layout, TropicalLayout.Island island) {
        Key key = new Key(layout.seed(), island.hash());
        TropicalIslandPlan plan = CACHE.get(key);
        if (plan == null) {
            plan = new TropicalIslandPlan(layout, island);
            CACHE.put(key, plan);
            if (CACHE.size() > 256) CACHE.remove(CACHE.keySet().iterator().next());
        }
        return plan;
    }

    public static TropicalIslandPlan forChunk(TropicalLayout layout, int x, int z) {
        return forIsland(layout, layout.nearest(x * 16 + 8, z * 16 + 8));
    }

    private static List<Chunk> ordered(List<Chunk> chunks, long salt) {
        chunks.sort(Comparator.<Chunk>comparingLong(chunk -> TropicalLayout.mix(salt
                ^ chunk.x * 341873128712L ^ chunk.z * 132897987541L)).thenComparingInt(Chunk::x).thenComparingInt(Chunk::z));
        return List.copyOf(chunks);
    }

    public static int shallowCells(TropicalLayout layout, int chunkX, int chunkZ) {
        return shallowCells(layout, layout.nearest(chunkX * 16 + 8, chunkZ * 16 + 8), chunkX, chunkZ);
    }

    private static int shallowCells(TropicalLayout layout, TropicalLayout.Island island, int chunkX, int chunkZ) {
        int count = 0;
        for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
            if (oneDeepMangrove(layout.sample(x, z, island))) count++;
        }
        return count;
    }

    private static boolean oneDeepMangrove(TropicalLayout.Sample column) {
        return column.biome() == 1 && (int) Math.floor(column.height() + .5) == 62;
    }

    /** Five edge-connected roots in the same six-by-six window used by actual wet-crop placement. */
    private static Set<Column> wetCropFootprint(TropicalLayout layout, TropicalLayout.Island island, Chunk chunk) {
        List<Column> sites = new ArrayList<>();
        for (int x = chunk.x * 16; x < chunk.x * 16 + 16; x++) for (int z = chunk.z * 16; z < chunk.z * 16 + 16; z++) {
            if (oneDeepMangrove(layout.sample(x, z, island))) sites.add(new Column(x, z));
        }
        Set<Column> shallowSites = Set.copyOf(sites);
        // Spread from a seeded starting point within the owning chunk.
        sites.sort(Comparator.comparingLong(c -> TropicalLayout.mix(island.hash() ^ c.x * 341873128712L ^ c.z * 132897987541L)));
        for (Column start : sites) {
            Set<Column> footprint = new LinkedHashSet<>();
            var frontier = new ArrayDeque<Column>();
            frontier.add(start);
            footprint.add(start);
            while (!frontier.isEmpty()) {
                Column next = frontier.remove();
                for (var step : List.of(new Column(next.x + 1, next.z), new Column(next.x, next.z + 1),
                        new Column(next.x - 1, next.z), new Column(next.x, next.z - 1))) {
                    if (step.x < start.x - 2 || step.x > start.x + 3 || step.z < start.z - 2 || step.z > start.z + 3
                            || !shallowSites.contains(step) || !footprint.add(step)) continue;
                    if (footprint.size() == 5) return footprint;
                    frontier.add(step);
                }
            }
        }
        return Set.of();
    }

    public boolean wetCropRoot(int x, int z) { return wetCropRoots.contains(new Column(x, z)); }
    public boolean nearWetCropRoot(int x, int z) { return wetCropClearance.contains(new Column(x, z)); }

    /** Reproduce the old six-by-six grid of potential wet-patch centres. */
    private static int legacyWetHabitat(TropicalLayout layout, TropicalLayout.Island island, int chunkX, int chunkZ) {
        int result = 0;
        for (int x = chunkX * 16 + 2; x <= chunkX * 16 + 12; x += 2) {
            for (int z = chunkZ * 16 + 2; z <= chunkZ * 16 + 12; z += 2) {
                var sample = layout.sample(x, z, island);
                if (sample.biome() != 1) continue;
                int ground = (int) Math.floor(sample.height() + .5);
                if (ground == 62) return 2;
                if (ground < 62) result = 1;
            }
        }
        return result;
    }

    private static void distribute(Map<Chunk, Integer> counts, List<Chunk> chunks, int total) {
        if (chunks.isEmpty()) return;
        for (int i = 0; i < total; i++) counts.merge(chunks.get(i % chunks.size()), 1, Integer::sum);
    }

    public List<Chunk> rainforest() { return rainforest; }
    public List<Chunk> cherry() { return cherry; }
    public List<Chunk> mounds() { return mounds; }
    public List<Chunk> shallow() { return shallow; }
    public int pickerelweedAttempts(int x, int z) { return pickerelweed.getOrDefault(new Chunk(x, z), 0); }
    public int pickerelweedTotal() { return pickerelweed.values().stream().mapToInt(Integer::intValue).sum(); }

    /** Two attempts of one type: 75% small-only chunks and 25% large-only chunks. */
    public String dripleafSpecies(int x, int z) {
        long choice = TropicalLayout.mix(dripleafSalt ^ x * 341873128712L ^ z * 132897987541L);
        return (choice & 3) == 0 ? "minecraft:big_dripleaf" : "minecraft:small_dripleaf";
    }

    public String rainforestSpecies(int x, int z) {
        int index = rainforest.indexOf(new Chunk(x, z));
        if (index < 0) return null;
        if (index < 2 * MUSHROOM_PATCHES) return FOREST_RESERVED.get(index / MUSHROOM_PATCHES);
        index -= 2 * MUSHROOM_PATCHES;
        if (index < 3 * FOREST_CROP_PATCHES) return FOREST_RESERVED.get(2 + index / FOREST_CROP_PATCHES);
        index -= 3 * FOREST_CROP_PATCHES;
        if (index < 2 * AZALEA_PATCHES) return FOREST_RESERVED.get(5 + index / AZALEA_PATCHES);
        index -= 2 * AZALEA_PATCHES;
        int torchflowerBonus = scaled(3, FOREST_AREA_RATIO);
        return index < torchflowerBonus ? "minecraft:torchflower"
                : FOREST_FLOWERS.get((index - torchflowerBonus) % FOREST_FLOWERS.size());
    }

    public String cherryCrop(int x, int z) {
        int index = cherry.indexOf(new Chunk(x, z));
        return index < 0 || index >= 2 * GROVE_CROP_PATCHES ? null : index < GROVE_CROP_PATCHES ? "farmersdelight:wild_carrots"
                : "legumedelight:wild_beans";
    }

    public String cherryFlower(int x, int z) {
        int index = cherry.indexOf(new Chunk(x, z));
        if (index < 2 * GROVE_CROP_PATCHES) return null;
        int slot = (index - 2 * GROVE_CROP_PATCHES + 1) % 21;
        return slot < 5 ? "minecraft:allium" : slot < 10 ? "minecraft:pink_petals"
                : slot < 15 ? "biomesoplenty:pink_daffodil" : slot < 17 ? "biomeswevegone:japanese_orchid"
                : slot < 19 ? "minecraft:peony" : "minecraft:lilac";
    }

    public String moundSpecies(int x, int z) {
        int index = mounds.indexOf(new Chunk(x, z));
        if (index < 0) return null;
        if (index < 4 * BANK_PATCHES) return MOUND_RESERVED.get(index / BANK_PATCHES);
        return index - 4 * BANK_PATCHES < (mounds.size() - 4 * BANK_PATCHES) * 11 / 20 ? "minecraft:blue_orchid" : "minecraft:pitcher_plant";
    }

    public String wetCrop(int x, int z) {
        int index = wetCropChunks.indexOf(new Chunk(x, z));
        return index < 0 ? null : index < WET_CROP_PATCHES ? "farmersdelight:wild_rice" : "minecraft:sugar_cane";
    }
}
