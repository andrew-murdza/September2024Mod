package net.amurdza.examplemod.worldgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

/** Independently scattered compact crop patches in 120-by-120 world tiles. */
public final class SpacedCropPatches {
    public static final int TILE_SIZE = 120;

    private SpacedCropPatches() { }

    public static List<CropTiles.Patch> plan(long seed, int tileX, int tileZ, List<String> palette,
                                            int plants, BiPredicate<Integer, Integer> suitable) {
        List<CropTiles.Patch> result = new ArrayList<>();
        for (int species = 0; species < palette.size(); species++) {
            String name = palette.get(species);
            long salt = seed ^ name.hashCode() ^ species * 0x632be59bd9b4e019L;
            List<CropTiles.Patch> candidates = new ArrayList<>();
            // Eight-block cells put all six-block footprints entirely inside
            // one decorating chunk, even where a 120-block tile cuts a chunk.
            for (int dx = 0; dx < TILE_SIZE; dx += 8) for (int dz = 0; dz < TILE_SIZE; dz += 8) {
                candidates.add(new CropTiles.Patch(tileX * TILE_SIZE + dx + 3,
                        tileZ * TILE_SIZE + dz + 3, name));
            }
            candidates.sort(Comparator.comparingLong(p -> TropicalLayout.mix(salt
                    ^ p.x() * 341873128712L ^ p.z() * 132897987541L)));
            for (var candidate : candidates) {
                if (result.stream().anyMatch(p -> Math.abs(p.x() - candidate.x()) < 24
                        && Math.abs(p.z() - candidate.z()) < 24)) continue;
                if (!suitable.test(candidate.x(), candidate.z())) continue;
                int count = 0;
                for (int dx = -2; dx <= 3 && count < plants; dx++) {
                    for (int dz = -2; dz <= 3 && count < plants; dz++) {
                        if (suitable.test(candidate.x() + dx, candidate.z() + dz)) count++;
                    }
                }
                if (count == plants) {
                    result.add(candidate);
                    break;
                }
            }
        }
        return List.copyOf(result);
    }
}
