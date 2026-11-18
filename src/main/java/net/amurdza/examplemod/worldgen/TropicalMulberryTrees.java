package net.amurdza.examplemod.worldgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Six separated natural mangroves, reserved before any neighboring chunk decorates. */
public final class TropicalMulberryTrees {
    public static final int TREES = 6;
    public static final int FRUIT_PER_TREE = 6;
    private static final int CLEARANCE = 22;
    public record Site(int chunkX, int chunkZ) {
        public int x() { return chunkX * 16 + 8; }
        public int z() { return chunkZ * 16 + 8; }
    }
    private record Key(long seed, long island) { }
    private static final Map<Key, List<Site>> CACHE = new LinkedHashMap<>(64, .75F, true);

    private TropicalMulberryTrees() { }

    public static synchronized List<Site> sites(TropicalLayout layout, TropicalLayout.Island island) {
        Key key = new Key(layout.seed(), island.hash());
        List<Site> cached = CACHE.get(key);
        if (cached != null) return cached;
        List<Site> candidates = new ArrayList<>();
        int reach = (int) Math.ceil(TropicalLayout.ISLAND_RADIUS);
        for (int x = Math.floorDiv((int) Math.floor(island.x()) - reach, 16);
             x <= Math.floorDiv((int) Math.ceil(island.x()) + reach, 16); x++) {
            for (int z = Math.floorDiv((int) Math.floor(island.z()) - reach, 16);
                 z <= Math.floorDiv((int) Math.ceil(island.z()) + reach, 16); z++) {
                var sample = layout.sample(x * 16 + 8, z * 16 + 8, island);
                if (TropicalLayout.mangroveTreeChunk(x, z)
                        && sample.distance() >= sample.forestEdge() + 20
                        && sample.distance() <= sample.mangroveEdge() - 16) candidates.add(new Site(x, z));
            }
        }
        candidates.sort(Comparator.<Site>comparingLong(site -> TropicalLayout.mix(island.hash()
                ^ site.chunkX * 341873128712L ^ site.chunkZ * 132897987541L ^ 0x428A2F98D728AE22L))
                .thenComparingInt(Site::chunkX).thenComparingInt(Site::chunkZ));
        List<Site> chosen = new ArrayList<>();
        for (Site candidate : candidates) {
            if (chosen.stream().anyMatch(site -> squaredDistance(site, candidate.x(), candidate.z()) < 48 * 48)) continue;
            chosen.add(candidate);
            if (chosen.size() == TREES) break;
        }
        if (chosen.size() != TREES) throw new IllegalStateException("Tropical island lacks six separated mulberry trees: " + island);
        List<Site> result = List.copyOf(chosen);
        CACHE.put(key, result);
        if (CACHE.size() > 256) CACHE.remove(CACHE.keySet().iterator().next());
        return result;
    }

    public static boolean selected(List<Site> sites, int chunkX, int chunkZ) {
        return sites.contains(new Site(chunkX, chunkZ));
    }

    /** Ordinary crowns and roots must not take a reserved tree's planting space first. */
    public static boolean reserved(List<Site> sites, int x, int z) {
        return sites.stream().anyMatch(site -> squaredDistance(site, x, z) < CLEARANCE * CLEARANCE);
    }

    private static long squaredDistance(Site site, int x, int z) {
        long dx = (long) site.x() - x, dz = (long) site.z() - z;
        return dx * dx + dz * dz;
    }
}
