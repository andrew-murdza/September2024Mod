package net.amurdza.stackbench;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.villagerrealms.entity.VillagerRealmsVillager;
import com.villagerrealms.settlement.VillageChunkTickingService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicitly enabled QA orchestration; never included in the shipped gameplay pack. */
@Mod("stackbench")
public final class BenchmarkMod {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Object> report = new LinkedHashMap<>();
    private final List<Map<String, Object>> phases = new ArrayList<>();
    private final long warmupSeconds = Long.getLong("stackbench.warmupSeconds", 60L);
    private final long baselineSeconds = Long.getLong("stackbench.baselineSeconds", 60L);
    private final long measureSeconds = Long.getLong("stackbench.measureSeconds", 600L);
    private final long soakSeconds = Long.getLong("stackbench.soakSeconds", 600L);
    private final int[] populations = java.util.Arrays.stream(System.getProperty("stackbench.populations", "32,64,128").split(","))
            .map(String::trim).mapToInt(Integer::parseInt).toArray();
    private final Path output = Path.of(System.getProperty("stackbench.output", "stackbench-report.json")).toAbsolutePath();
    private MinecraftServer server;
    private BenchmarkVillageFixture fixture;
    private BenchmarkCargoScenario cargo;
    private String state = "disabled";
    private String phaseName;
    private int phaseIndex = -1;
    private int phasePopulation;
    private long readyWaitStarted, warmupStarted, lastProgress;
    private Map<String, Object> phaseStart;
    private boolean finished;

    public BenchmarkMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SavedVillageRecovery());
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("stackbench.enabled") || Boolean.getBoolean("stackbench.recovery")) return;
        server = event.getServer();
        state = "setup";
        report.put("startedAt", Instant.now().toString());
        report.put("status", state);
        report.put("world", server.getWorldData().getLevelName());
        report.put("machine", BenchmarkMetrics.machineInfo());
        report.put("javaArguments", java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
        report.put("configuration", Map.of("warmupSeconds", warmupSeconds, "baselineSeconds", baselineSeconds,
                "measureSeconds", measureSeconds, "soakSeconds", soakSeconds, "populations", populations,
                "playerCountRequired", 0, "referenceTickBudgetMs", 50));
        report.put("phases", phases);
        lastProgress = System.nanoTime();
        writeReport();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null || event.getServer() != server || finished) return;
        try {
            if (server.getPlayerList().getPlayerCount() != 0)
                throw new IllegalStateException("Playerless benchmark interrupted by a connected player");
            if (state.equals("setup")) {
                long start = System.nanoTime();
                fixture = BenchmarkVillageFixture.setup(server);
                report.put("fixtureSetupSeconds", (System.nanoTime() - start) / 1_000_000_000.0);
                report.put("fixture", fixture.readinessReport());
                readyWaitStarted = System.nanoTime();
                state = "waiting_for_ticking_chunks";
                writeReport();
                return;
            }
            if (state.equals("waiting_for_ticking_chunks")) {
                if (fixture.ready()) {
                    report.put("chunkReadinessSeconds", secondsSince(readyWaitStarted));
                    beginWarmup(-1);
                } else if (secondsSince(readyWaitStarted) > 600) {
                    throw new IllegalStateException("Village chunks did not become fully ticking: " + fixture.readinessReport());
                }
                progress();
                return;
            }
            if (cargo != null) cargo.tick();
            if (state.equals("warmup")) {
                if (secondsSince(warmupStarted) >= warmupSeconds) {
                    if (cargo != null) cargo.beginMeasurement();
                    phaseStart = snapshot();
                    BenchmarkMetrics.begin(phaseName);
                    state = "measuring";
                    LOGGER.info("STACKBENCH Measuring {} with {} villagers", phaseName, phasePopulation);
                    writeReport();
                }
            } else if (state.equals("measuring")) {
                long length = phaseIndex == -1 ? baselineSeconds : phaseIndex == populations.length ? soakSeconds : measureSeconds;
                if (BenchmarkMetrics.elapsedSeconds() >= length) {
                    Map<String, Object> measured = BenchmarkMetrics.finish();
                    measured.put("requestedPopulation", phasePopulation);
                    measured.put("before", phaseStart);
                    measured.put("after", snapshot());
                    phases.add(measured);
                    LOGGER.info("STACKBENCH Completed {}: {}", phaseName, JSON.toJson(measured.get("fullTickMs")));
                    if (phaseIndex == populations.length || (soakSeconds == 0 && phaseIndex == populations.length - 1)) {
                        finished = true;
                        state = "completed";
                        report.put("finishedAt", Instant.now().toString());
                        report.put("final", snapshot());
                        report.put("interpretation", "Completed means measurements finished, not a gameplay/performance pass. Inspect cargo conservation, complete routes and tick tails for each population.");
                        writeReport();
                        server.halt(false);
                        return;
                    }
                    beginWarmup(phaseIndex + 1);
                }
            }
            progress();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void beginWarmup(int index) {
        phaseIndex = index;
        phasePopulation = index < 0 ? 0 : populations[Math.min(index, populations.length - 1)];
        phaseName = index < 0 ? "paired_empty_baseline" : index == populations.length
                ? "populated_day_night_soak_" + phasePopulation : "population_" + phasePopulation;
        if (phasePopulation > 0) {
            // The three courier jobs count toward the target; they are not extra hidden residents.
            if (cargo == null) cargo = BenchmarkCargoScenario.setup(fixture);
            fixture.addPopulation(phasePopulation);
            fixture.preparePopulationStock();
            cargo.onPopulation(phasePopulation);
        }
        // Work scheduling uses the Overworld clock. Hold daytime for comparable load steps,
        // then restore normal time for the final mixed work/sleep soak.
        boolean advancingTime = index == populations.length;
        for (ServerLevel level : server.getAllLevels()) {
            level.setDayTime(advancingTime ? 10000 : 6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(advancingTime, server);
        }
        warmupStarted = System.nanoTime();
        state = "warmup";
        LOGGER.info("STACKBENCH Warmup {}: {}", phaseName, JSON.toJson(fixture.readinessReport()));
        writeReport();
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("at", Instant.now().toString());
        result.put("serverTick", server.getTickCount());
        result.put("players", server.getPlayerList().getPlayerCount());
        result.put("overworldDayTime", server.overworld().getDayTime());
        result.put("fixture", fixture.readinessReport());
        result.put("backgroundProduction", fixture.backgroundProductionReport());
        if (cargo != null) result.put("cargo", cargo.report());
        List<Map<String, Object>> worlds = new ArrayList<>();
        for (ServerLevel level : List.of(fixture.desert(), fixture.nether())) {
            int entities = 0, villagers = 0, aliveVillagers = 0, movingVillagers = 0;
            Map<String, Integer> thoughts = new LinkedHashMap<>();
            for (Entity entity : level.getAllEntities()) {
                entities++;
                if (entity instanceof VillagerRealmsVillager worker) {
                    villagers++;
                    if (worker.isAlive()) aliveVillagers++;
                    if (worker.getDeltaMovement().horizontalDistanceSqr() > 0.0001) movingVillagers++;
                    thoughts.merge(worker.thoughtText(), 1, Integer::sum);
                }
            }
            worlds.add(Map.of("dimension", level.dimension().location().toString(), "entities", entities,
                    "villagers", villagers, "aliveVillagers", aliveVillagers, "movingVillagers", movingVillagers,
                    "thoughts", thoughts, "villageTicketChunks", VillageChunkTickingService.activeChunks(level).size(),
                    "loadedChunks", level.getChunkSource().getLoadedChunksCount()));
        }
        result.put("worlds", worlds);
        return result;
    }

    private void progress() {
        if (secondsSince(lastProgress) < 30) return;
        long diagnosticStarted = System.nanoTime();
        lastProgress = System.nanoTime();
        report.put("progress", Map.of("at", Instant.now().toString(), "phase", phaseName == null ? state : phaseName,
                "state", state, "measuredSeconds", BenchmarkMetrics.elapsedSeconds(),
                "completedTicks", BenchmarkMetrics.completedTicks(), "snapshot", snapshot()));
        LOGGER.info("STACKBENCH {} {} measuredSeconds={}", state, phaseName, Math.round(BenchmarkMetrics.elapsedSeconds()));
        writeReport();
        BenchmarkMetrics.recordDiagnosticNanos("progress_snapshot_and_report", System.nanoTime() - diagnosticStarted);
    }

    private void fail(Throwable failure) {
        LOGGER.error("STACKBENCH failed", failure);
        if (BenchmarkMetrics.isActive()) phases.add(BenchmarkMetrics.finish());
        finished = true;
        state = "failed";
        StringWriter stack = new StringWriter();
        failure.printStackTrace(new PrintWriter(stack));
        report.put("failure", stack.toString());
        report.put("finishedAt", Instant.now().toString());
        writeReport();
        server.halt(false);
    }

    private void writeReport() {
        report.put("status", state);
        report.put("updatedAt", Instant.now().toString());
        try {
            Files.createDirectories(output.getParent());
            Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
            Files.writeString(temporary, JSON.toJson(report), StandardCharsets.UTF_8);
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception failure) {
            LOGGER.error("Cannot write benchmark report {}", output, failure);
        }
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) {
        if (event.getServer() == server && !finished && !state.equals("disabled"))
            fail(new IllegalStateException("Server stopped before benchmark completion"));
    }

    private static double secondsSince(long start) { return (System.nanoTime() - start) / 1_000_000_000.0; }
}
