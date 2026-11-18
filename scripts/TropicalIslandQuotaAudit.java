import net.amurdza.examplemod.worldgen.TropicalIslandPlan;
import net.amurdza.examplemod.worldgen.TropicalLayout;

import java.util.HashMap;
import java.util.Map;

/** Checks that varied seeded island geometry can actually supply every reserved patch. */
public final class TropicalIslandQuotaAudit {
    // Area-scaled quotas for the restored 138/270/300/360-block habitat radii.
    private static final int FOREST_MUSHROOM_AND_AZALEA_PATCHES = 21;
    private static final int FOREST_CROP_PATCHES = 16;
    private static final int GROVE_CROP_PATCHES = 11;
    private static final int BANK_PATCHES = 16;
    private static final int WET_CROP_PATCHES = 84;

    public static void main(String[] args) {
        int islands = args.length == 0 ? 64 : Integer.parseInt(args[0]);
        int minForest = Integer.MAX_VALUE, minCherry = Integer.MAX_VALUE;
        int minMounds = Integer.MAX_VALUE, minShallow = Integer.MAX_VALUE;
        long started = System.nanoTime();
        for (int index = 0; index < islands; index++) {
            var layout = new TropicalLayout(TropicalLayout.mix(index * 73428767L));
            var island = layout.island(index % 5 - 2, index % 7 - 3);
            var plan = TropicalIslandPlan.forIsland(layout, island);
            minForest = Math.min(minForest, plan.rainforest().size());
            minCherry = Math.min(minCherry, plan.cherry().size());
            minMounds = Math.min(minMounds, plan.mounds().size());
            minShallow = Math.min(minShallow, plan.shallow().size());
            Map<String, Integer> forest = new HashMap<>(), mounds = new HashMap<>(), crops = new HashMap<>(), wet = new HashMap<>();
            plan.rainforest().forEach(chunk -> count(forest, plan.rainforestSpecies(chunk.x(), chunk.z())));
            plan.mounds().forEach(chunk -> count(mounds, plan.moundSpecies(chunk.x(), chunk.z())));
            plan.cherry().forEach(chunk -> count(crops, plan.cherryCrop(chunk.x(), chunk.z())));
            plan.shallow().forEach(chunk -> count(wet, plan.wetCrop(chunk.x(), chunk.z())));
            for (String species : new String[]{"minecraft:red_mushroom", "minecraft:brown_mushroom", "minecraft:azalea", "minecraft:flowering_azalea"})
                require(forest.getOrDefault(species, 0) == FOREST_MUSHROOM_AND_AZALEA_PATCHES, "Forest quota " + species + ": " + forest);
            for (String species : new String[]{"minecraft:melon", "farmersdelight:wild_tomatoes", "pineapple_delight:pineapple_wild_crop"})
                require(forest.getOrDefault(species, 0) == FOREST_CROP_PATCHES, "Forest crop quota " + species + ": " + forest);
            require(!plan.cherry().isEmpty(), "Original cherry grove must generate");
            for (String species : new String[]{"environmental:blue_delphinium", "environmental:purple_delphinium", "minecraft:rose_bush", "minecraft:torchflower"})
                require(forest.getOrDefault(species, 0) > 0, "Missing restored rainforest flower " + species);
            for (String species : new String[]{"minecraft:red_mushroom", "minecraft:brown_mushroom", "farmersdelight:wild_cabbages", "hearthandharvest:wild_cotton"})
                require(mounds.getOrDefault(species, 0) == BANK_PATCHES, "Mound quota " + species + ": " + mounds);
            int remainingMounds = plan.mounds().size() - 4 * BANK_PATCHES;
            require(mounds.getOrDefault("minecraft:blue_orchid", 0) == remainingMounds * 11 / 20, "Blue orchid rounding");
            require(mounds.getOrDefault("minecraft:pitcher_plant", 0) == remainingMounds - remainingMounds * 11 / 20, "Pitcher rounding");
            for (String species : new String[]{"farmersdelight:wild_carrots", "legumedelight:wild_beans"})
                require(crops.getOrDefault(species, 0) == GROVE_CROP_PATCHES, "Original grove crop quota " + species + ": " + crops);
            require(wet.getOrDefault("farmersdelight:wild_rice", 0) == WET_CROP_PATCHES
                    && wet.getOrDefault("minecraft:sugar_cane", 0) == WET_CROP_PATCHES, "Shallow crop quota " + wet);
            for (var chunk : plan.shallow())
                require(TropicalIslandPlan.shallowCells(layout, chunk.x(), chunk.z()) >= 5, "Wet crops assigned to deep water");
        }
        System.out.printf(java.util.Locale.ROOT,
                "{\"islands\":%d,\"all_quotas_passed\":true,\"minimum_forest_chunks\":%d,\"minimum_cherry_chunks\":%d,"
                        + "\"minimum_mounds\":%d,\"minimum_shallow_chunks\":%d,\"seconds\":%.3f}%n",
                islands, minForest, minCherry, minMounds, minShallow, (System.nanoTime() - started) / 1e9);
    }

    private static void count(Map<String, Integer> counts, String species) {
        if (species != null) counts.merge(species, 1, Integer::sum);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
