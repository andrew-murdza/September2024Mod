import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.amurdza.examplemod.worldgen.TropicalMulberryTrees;

/** Check that the expanded fruit quota remains deterministic and fits every sampled island. */
public final class TropicalMulberryAudit {
    public static void main(String[] args) {
        int count = args.length == 0 ? 1024 : Integer.parseInt(args[0]);
        for (int index = 0; index < count; index++) {
            var layout = new TropicalLayout(TropicalLayout.mix(index * 73428767L));
            var island = layout.island(index % 11 - 5, index % 17 - 8);
            var sites = TropicalMulberryTrees.sites(layout, island);
            require(sites.size() == 30, "Expected thirty fruiting tree chunks");
            require(sites.equals(TropicalMulberryTrees.sites(layout, island)), "Reservations changed on repeated access");
            for (int first = 0; first < sites.size(); first++) {
                var site = sites.get(first);
                var sample = layout.sample(site.x(), site.z(), island);
                require(sample.biome() == 1 && TropicalLayout.mangroveTreeChunk(site.chunkX(), site.chunkZ()),
                        "Fruit reservation escaped its mangrove tree chunk");
                for (int second = first + 1; second < sites.size(); second++) {
                    long dx = site.x() - sites.get(second).x(), dz = site.z() - sites.get(second).z();
                    require(dx * dx + dz * dz >= 48 * 48, "Fruit reservations overlap");
                }
            }
        }
        System.out.printf("{\"islands\":%d,\"fruiting_chunks_per_island\":30,\"minimum_separation\":48,\"passed\":true}%n", count);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
