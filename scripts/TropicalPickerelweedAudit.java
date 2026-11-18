import net.amurdza.examplemod.worldgen.TropicalIslandPlan;
import net.amurdza.examplemod.worldgen.TropicalLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Compare the second shallow reduction with the frozen first reduction and original deep allocation. */
public final class TropicalPickerelweedAudit {
    public static void main(String[] args) {
        int islands = args.length == 0 ? 128 : Integer.parseInt(args[0]);
        long originalShallow = 0, oldShallow = 0, newShallow = 0, oldDeep = 0, newDeep = 0;
        long smallChunks = 0, largeChunks = 0;
        int roundedUp = 0, roundedDown = 0, checkedDeepChunks = 0;
        for (int index = 0; index < islands; index++) {
            var layout = new TropicalLayout(TropicalLayout.mix(index * 73428767L));
            var island = layout.island(index % 5 - 2, index % 7 - 3);
            var plan = TropicalIslandPlan.forIsland(layout, island);
            var baseline = baseline(layout, island, plan);
            int shallowCount = plan.shallow().size(), deepCount = baseline.deep.size();
            require(shallowCount > 0 && deepCount > 0, "Audit requires both habitats");
            for (int i = 0; i < shallowCount; i++) {
                var chunk = plan.shallow().get(i);
                int original = baseline.emergent / shallowCount + (i < baseline.emergent % shallowCount ? 1 : 0);
                long firstRounding = TropicalLayout.mix(island.hash() ^ 0x9B05688C2B3E6C1FL
                        ^ chunk.x() * 341873128712L ^ chunk.z() * 132897987541L);
                int before = original / 2 + (original % 2 != 0 && (firstRounding & 1) != 0 ? 1 : 0);
                int after = plan.pickerelweedAttempts(chunk.x(), chunk.z());
                require(after == before / 2 || before % 2 == 1 && after == before / 2 + 1,
                        "Shallow allocation did not halve: " + chunk + ", before=" + before + ", after=" + after);
                if (before % 2 == 1) {
                    if (after > before / 2) roundedUp++; else roundedDown++;
                }
                originalShallow += original;
                oldShallow += before;
                newShallow += after;
                String dripleaf = plan.dripleafSpecies(chunk.x(), chunk.z());
                require(dripleaf.equals(TropicalIslandPlan.forChunk(layout, chunk.x(), chunk.z())
                        .dripleafSpecies(chunk.x(), chunk.z())), "Dripleaf choice depends on plan lookup order");
                if (dripleaf.equals("minecraft:small_dripleaf")) smallChunks++;
                else {
                    require(dripleaf.equals("minecraft:big_dripleaf"), "Unknown dripleaf allocation");
                    largeChunks++;
                }
            }
            for (int i = 0; i < deepCount; i++) {
                var chunk = baseline.deep.get(i);
                int before = baseline.submerged / deepCount + (i < baseline.submerged % deepCount ? 1 : 0);
                int after = plan.pickerelweedAttempts(chunk.x(), chunk.z());
                require(before == after, "Deep allocation moved or changed: " + chunk + ", before=" + before + ", after=" + after);
                oldDeep += before;
                newDeep += after;
                checkedDeepChunks++;
            }
            long expected = plan.shallow().stream().mapToLong(chunk -> plan.pickerelweedAttempts(chunk.x(), chunk.z())).sum()
                    + baseline.submerged;
            require(plan.pickerelweedTotal() == expected, "Removed shallow attempts leaked into another chunk");
            require(plan.shallow().stream().filter(chunk -> "farmersdelight:wild_rice".equals(plan.wetCrop(chunk.x(), chunk.z()))).count() == 84
                            && plan.shallow().stream().filter(chunk -> "minecraft:sugar_cane".equals(plan.wetCrop(chunk.x(), chunk.z()))).count() == 84,
                    "Pickerelweed reduction changed reserved wet crop quotas");
        }
        require(oldDeep == newDeep, "The deep-water total changed");
        require(Math.abs((double) newShallow / oldShallow - .5) < .015,
                "Seeded odd-count rounding is biased away from half");
        require(roundedUp > 0 && roundedDown > 0, "Odd allocations must round both ways");
        long shallowChunks = smallChunks + largeChunks;
        require(Math.abs((double) smallChunks / shallowChunks - .75) < .015,
                "Exclusive dripleaf selection must preserve the three-small to one-large expectation");
        System.out.printf(java.util.Locale.ROOT,
                "{\"islands\":%d,\"original_shallow_attempts\":%d,\"old_shallow_attempts\":%d,\"new_shallow_attempts\":%d,\"shallow_retained_percent\":%.4f,"
                        + "\"old_deep_attempts\":%d,\"new_deep_attempts\":%d,\"unchanged_deep_chunks\":%d,"
                        + "\"odd_round_up\":%d,\"odd_round_down\":%d,\"wet_crop_quotas_unchanged\":true,"
                        + "\"small_only_chunks\":%d,\"large_only_chunks\":%d,\"small_patch_retained_percent\":%.4f,\"large_patch_retained_percent\":%.4f}%n",
                islands, originalShallow, oldShallow, newShallow, 100.0 * newShallow / oldShallow, oldDeep, newDeep,
                checkedDeepChunks, roundedUp, roundedDown, smallChunks, largeChunks,
                100.0 * 2 * smallChunks / (3 * shallowChunks), 100.0 * 2 * largeChunks / shallowChunks);
    }

    private record Baseline(List<TropicalIslandPlan.Chunk> deep, int emergent, int submerged) { }

    /** Frozen pre-change oracle: two coarse wet draws, then the original 75/25 relocation. */
    private static Baseline baseline(TropicalLayout layout, TropicalLayout.Island island, TropicalIslandPlan plan) {
        List<TropicalIslandPlan.Chunk> deep = new ArrayList<>();
        int thirds = 0, reach = (int) Math.ceil(TropicalLayout.ISLAND_RADIUS) + 16;
        for (int x = Math.floorDiv((int) Math.floor(island.x()) - reach, 16);
             x <= Math.floorDiv((int) Math.ceil(island.x()) + reach, 16); x++) {
            for (int z = Math.floorDiv((int) Math.floor(island.z()) - reach, 16);
                 z <= Math.floorDiv((int) Math.ceil(island.z()) + reach, 16); z++) {
                var center = layout.sample(x * 16 + 8, z * 16 + 8);
                if (center.island().hash() != island.hash() || center.distance() < center.forestEdge() - 12
                        || center.distance() > center.mangroveEdge() + 12) continue;
                boolean wet = false, shallow = false;
                for (int dx = 2; dx <= 12; dx += 2) for (int dz = 2; dz <= 12; dz += 2) {
                    int atX = x * 16 + dx, atZ = z * 16 + dz;
                    if (layout.sample(atX, atZ).biome() != 1) continue;
                    int bed = (int) Math.floor(layout.surfaceHeight(atX, atZ) + .5);
                    wet |= bed <= 62;
                    shallow |= bed == 62;
                }
                if (!wet) continue;
                thirds += shallow ? 2 : 6;
                var chunk = new TropicalIslandPlan.Chunk(x, z);
                if (!plan.shallow().contains(chunk)) deep.add(chunk);
            }
        }
        long salt = island.hash() ^ 0x510E527FADE682D1L;
        deep.sort(Comparator.<TropicalIslandPlan.Chunk>comparingLong(chunk -> TropicalLayout.mix(salt
                        ^ chunk.x() * 341873128712L ^ chunk.z() * 132897987541L))
                .thenComparingInt(TropicalIslandPlan.Chunk::x).thenComparingInt(TropicalIslandPlan.Chunk::z));
        int total = thirds / 3;
        if (Math.floorMod(TropicalLayout.mix(island.hash() ^ 0x1F83D9ABFB41BD6BL), 3) < thirds % 3) total++;
        int emergent = (total * 3 + 2) / 4;
        return new Baseline(deep, emergent, total - emergent);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
