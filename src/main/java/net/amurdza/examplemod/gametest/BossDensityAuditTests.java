package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Measure accepted sites and Euclidean neighbor distances, not just candidate grid spacing. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossDensityAuditTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 12000)
    public static void acceptedBossSitesHaveMeasuredEuclideanSpacing(GameTestHelper helper) throws Exception {
        List<Map<String, Object>> reports = new ArrayList<>();
        String[][] kinds = {{"illager_mansion", "illager_mansions", "plains"}, {"orc_camp", "orc_camps", "savanna"},
                {"yeti_cave", "yeti_caves", "snowy"}, {"wither_cave", "wither_caves", "nether"}};
        for (String[] kind : kinds) {
            var key = switch (kind[2]) { case "plains" -> GameplayDimensions.PLAINS; case "savanna" -> GameplayDimensions.SAVANNA;
                case "snowy" -> GameplayDimensions.SNOWY; default -> GameplayDimensions.NETHER; };
            var level = helper.getLevel().getServer().getLevel(key);
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod", kind[0]));
            var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(new ResourceLocation("aoemod", kind[1]));
            var placement = set.placement() instanceof RandomSpreadStructurePlacement spread ? spread : null;
            boolean yeti = kind[0].equals("yeti_cave");
            boolean camp = kind[0].equals("orc_camp");
            Set<Long> seeds = new LinkedHashSet<>();
            seeds.add(level.getSeed());
            if (yeti) seeds.addAll(List.of(8265669121673610644L, 3330086936475256017L));
            List<Double> pooledDistances = new ArrayList<>();
            int acceptedTotal = 0;
            // Yeti's rare accepted plateaus need more interior observations.
            // Three surrounding regions retain distant neighbors; the other
            // boss audits keep their existing complete 12x12 candidate grids.
            int outerRadius = yeti ? 9 : 6, interiorRadius = yeti ? 6 : 4;
            int candidateCount = 4 * outerRadius * outerRadius;
            for (long seed : seeds) {
                var random = seed == level.getSeed() ? level.getChunkSource().randomState()
                        : RandomState.create(generator.generatorSettings().value(),
                                level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
                List<BlockPos> sites = new ArrayList<>(), interior = new ArrayList<>();
                long start = System.nanoTime();
                for (int rx = -outerRadius; rx < outerRadius; rx++) for (int rz = -outerRadius; rz < outerRadius; rz++) {
                    var chunk = yeti ? net.amurdza.examplemod.worldgen.structure.PlateauLairPlacement.candidate(random, rz, rx)
                            : camp ? new ChunkPos(net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement.findCenter(net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement.terrainLand(random), rx, rz))
                            : placement.getPotentialStructureChunk(seed, rx * placement.spacing(), rz * placement.spacing());
                    var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                            level.getStructureManager(), seed, chunk, level, structure.biomes()::contains);
                    var candidate = structure.findValidGenerationPoint(context);
                    if (candidate.isEmpty()) continue;
                    BlockPos pos = candidate.orElseThrow().position(); sites.add(pos);
                    if (rx >= -interiorRadius && rx < interiorRadius && rz >= -interiorRadius && rz < interiorRadius) interior.add(pos);
                }
                List<Double> distances = new ArrayList<>();
                for (BlockPos pos : interior) distances.add(sites.stream().filter(other -> !other.equals(pos) && (!yeti || Math.abs(other.getZ() - pos.getZ()) < 300))
                        .mapToDouble(other -> Math.hypot(pos.getX() - other.getX(), pos.getZ() - other.getZ())).min().orElse(Double.NaN));
                distances.sort(Double::compare);
                Map<String, Object> report = summarize(kind[0], distances);
                report.put("aggregation", "per_seed"); report.put("seed", seed); report.put("candidateCount", candidateCount);
                report.put("accepted", sites.size()); if (placement != null) report.put("spacingChunks", placement.spacing());
                if (placement != null) report.put("separationChunks", placement.separation());
                if (camp) {
                    report.put("spacingXBlocks", net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement.X_SPACING);
                    report.put("spacingZBlocks", net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement.Z_SPACING);
                }
                report.put("seconds", (System.nanoTime()-start)/1.0e9);
                report.put("sites", sites.stream().map(pos -> List.of(pos.getX(),pos.getY(),pos.getZ())).toList());
                reports.add(report);
                pooledDistances.addAll(distances); acceptedTotal += sites.size();
                System.out.println("AOEMOD_BOSS_DENSITY " + kind[0] + " seed=" + seed + " accepted=" + sites.size()
                        + " interior=" + interior.size() + " mean=" + report.get("meanNearest")
                        + " median=" + report.get("medianNearest") + " seconds=" + report.get("seconds"));
                helper.assertTrue(!interior.isEmpty(), "No accepted interior boss sites for " + kind[0] + " seed=" + seed);
                double mean = (double) report.get("meanNearest");
                helper.assertTrue(mean >= 650 && mean <= 1600,
                        "Accepted-site spacing departed substantially from the roughly 1000-block target: "
                                + kind[0] + " seed=" + seed + " mean=" + mean);
            }
            if (yeti) {
                pooledDistances.sort(Double::compare);
                Map<String, Object> pooled = summarize(kind[0], pooledDistances);
                pooled.put("aggregation", "pooled"); pooled.put("seeds", List.copyOf(seeds));
                pooled.put("candidateCount", candidateCount * seeds.size()); pooled.put("accepted", acceptedTotal);
                pooled.put("spacingAlongPlateau", 1000);
                reports.add(pooled);
                System.out.println("AOEMOD_BOSS_DENSITY_POOLED yeti_cave seeds=" + seeds.size()
                        + " interior=" + pooledDistances.size() + " mean=" + pooled.get("meanNearest")
                        + " median=" + pooled.get("medianNearest"));
                double mean = (double) pooled.get("meanNearest");
                helper.assertTrue(mean >= 650 && mean <= 1600,
                        "Pooled accepted Yeti spacing departed substantially from the roughly 1000-block target: " + mean);
            }
        }
        Path out = Path.of("build/reports/boss-density.json"); Files.createDirectories(out.getParent());
        Files.writeString(out, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reports));
        helper.succeed();
    }

    private static Map<String, Object> summarize(String kind, List<Double> sortedDistances) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("kind", kind); report.put("interiorSamples", sortedDistances.size());
        report.put("meanNearest", sortedDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        report.put("medianNearest", sortedDistances.isEmpty() ? 0
                : (sortedDistances.get((sortedDistances.size() - 1) / 2) + sortedDistances.get(sortedDistances.size() / 2)) / 2);
        report.put("maxNearest", sortedDistances.isEmpty() ? 0 : sortedDistances.get(sortedDistances.size() - 1));
        return report;
    }
}
