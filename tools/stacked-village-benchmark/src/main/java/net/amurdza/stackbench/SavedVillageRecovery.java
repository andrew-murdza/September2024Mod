package net.amurdza.stackbench;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.villagerrealms.entity.VillagerRealmsVillager;
import com.villagerrealms.housing.HousingSavedData;
import com.villagerrealms.housing.HousingAssignmentService;
import com.villagerrealms.settlement.VillageChunkTickingService;
import com.villagerrealms.work.runtime.WorkerInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Observes a copied, populated save; never rebuilds rooms, moves workers or supplies items. */
public final class SavedVillageRecovery {
    private final Map<String, Object> report = new LinkedHashMap<>();
    private final List<Map<String, Object>> history = new ArrayList<>();
    private MinecraftServer server;
    private ServerLevel desert, nether;
    private Path output;
    private UUID courierId, houseId;
    private Vec3 expectedPosition, previousPosition;
    private String expectedWorkplace, villageId;
    private int population;
    private Set<UUID> residentIds;
    private boolean finished, observedCourier;
    private long started, lastProgress, allSleepingSince;
    private double travelled;

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("stackbench.enabled") || !Boolean.getBoolean("stackbench.recovery")) return;
        server = event.getServer();
        output = Path.of(System.getProperty("stackbench.output", "stackbench-recovery.json")).toAbsolutePath();
        started = lastProgress = System.nanoTime();
        try {
            Path source = Path.of(System.getProperty("stackbench.sourceReport"));
            JsonObject original = JsonParser.parseString(Files.readString(source)).getAsJsonObject();
            require(original.get("status").getAsString().equals("completed"), "Source benchmark is incomplete");
            JsonObject last = original.getAsJsonObject("final");
            population = last.getAsJsonObject("fixture").get("registeredWorkers").getAsInt();
            JsonObject gold = null;
            for (var entry : last.getAsJsonObject("cargo").getAsJsonArray("routes")) {
                JsonObject route = entry.getAsJsonObject();
                if (route.get("name").getAsString().equals("gold_up")) gold = route;
            }
            require(gold != null, "Source has no gold courier");
            courierId = UUID.fromString(gold.get("uuid").getAsString());
            var position = gold.getAsJsonArray("position");
            expectedPosition = new Vec3(position.get(0).getAsDouble(), position.get(1).getAsDouble(), position.get(2).getAsDouble());
            expectedWorkplace = gold.get("assigned_workplace").getAsString();
            desert = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(gold.get("destination_dimension").getAsString())));
            nether = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(gold.get("source_dimension").getAsString())));
            require(desert != null && nether != null, "Saved paired dimensions are missing");
            // Hold night solely to observe the stranded worker's normal route to its saved bed.
            server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            server.overworld().setDayTime(18_000);
            report.put("startedAt", Instant.now().toString());
            report.put("sourceReport", source.toAbsolutePath().toString());
            report.put("world", server.getWorldData().getLevelName());
            report.put("machine", BenchmarkMetrics.machineInfo());
            report.put("configuration", Map.of("expectedResidents", population, "courierUuid", courierId.toString(),
                    "heldOverworldTime", 18000, "maximumSeconds", Long.getLong("stackbench.recoverySeconds", 600L),
                    "stableSleepingSeconds", Long.getLong("stackbench.recoveryStableSeconds", 60L)));
            report.put("method", "Copied saved village; normal AI only. No new fixtures, path/controller calls, resident relocation, food seeding or inventory transfers. Night is held to test the saved stranded courier's home route. Tick timings include observation.");
            report.put("history", history);
            report.put("status", "waiting_for_saved_residents");
            write();
        } catch (Throwable failure) { fail(failure); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void tick(TickEvent.ServerTickEvent event) {
        if (server == null || event.getServer() != server || event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(server.getPlayerList().getPlayerCount() == 0, "A player joined the recovery check");
            Map<UUID, VillagerRealmsVillager> residents = residents();
            VillagerRealmsVillager courier = residents.get(courierId);
            if (courier != null && !observedCourier) {
                require(courier.level() == desert && courier.position().distanceToSqr(expectedPosition) < 16,
                        "Saved courier did not start at the recorded stranded location");
                require(courier.getAssignments().workplace().map(UUID::toString).orElse("none").equals(expectedWorkplace),
                        "Saved courier workplace changed");
                houseId = HousingSavedData.get(desert).houseFor(courierId).orElseThrow();
                villageId = courier.getSimulationState().villageId().map(UUID::toString).orElseThrow();
                previousPosition = courier.position();
                report.put("firstObservedCourier", courierSnapshot(courier));
                observedCourier = true;
            }
            if (courier != null && observedCourier) {
                double step = courier.position().distanceTo(previousPosition);
                require(courier.level() == desert && step <= 4, "Recovery courier relocated instead of walking its home route");
                travelled += step;
                previousPosition = courier.position();
            }
            if (residentIds == null) {
                if (residents.size() == population && courier != null && ready()) {
                    residentIds = new LinkedHashSet<>(residents.keySet());
                    report.put("loadSeconds", secondsSince(started));
                    report.put("before", snapshot(residents));
                    BenchmarkMetrics.begin("saved_village_home_recovery");
                    report.put("status", "measuring");
                    write();
                } else require(secondsSince(started) < 600, "Saved residents or paired ticking chunks did not load");
                progress(residents);
                return;
            }
            require(residents.keySet().equals(residentIds), "Resident identities changed during recovery");
            require(residents.values().stream().allMatch(Entity::isAlive), "A saved resident died");
            require(ready(), "Paired chunks stopped entity ticking");
            require(courier != null && courier.getSimulationState().villageId().map(UUID::toString).orElse("").equals(villageId),
                    "Courier village membership changed");
            require(HousingSavedData.get(desert).houseFor(courierId).filter(houseId::equals).isPresent(), "Courier lost its saved house claim");
            require(courier.getAssignments().workplace().map(UUID::toString).orElse("none").equals(expectedWorkplace), "Courier workplace changed");
            boolean allSleeping = residents.values().stream().allMatch(VillagerRealmsVillager::isSleeping);
            if (allSleeping) {
                require(sleepingInClaimedHouse(courier), "Courier slept outside its claimed house");
                if (allSleepingSince == 0) {
                    long diagnostic = System.nanoTime();
                    require(residents.values().stream().allMatch(this::sleepingInClaimedHouse),
                            "A resident slept outside its assigned house");
                    BenchmarkMetrics.recordDiagnosticNanos("validate_all_sleeping_houses", System.nanoTime() - diagnostic);
                    allSleepingSince = System.nanoTime();
                    report.put("allSleepingAfterMeasuredSeconds", BenchmarkMetrics.elapsedSeconds());
                    report.put("firstAllSleeping", snapshot(residents));
                }
                if (secondsSince(allSleepingSince) >= Long.getLong("stackbench.recoveryStableSeconds", 60L)) {
                    finish(residents);
                    return;
                }
            } else allSleepingSince = 0;
            require(BenchmarkMetrics.elapsedSeconds() < Long.getLong("stackbench.recoverySeconds", 600L),
                    "Residents did not all reach their beds within the recovery window");
            progress(residents);
        } catch (Throwable failure) { fail(failure); }
    }

    private boolean sleepingInClaimedHouse(VillagerRealmsVillager courier) {
        var house = HousingAssignmentService.assignedHouse((ServerLevel) courier.level(), courier).orElse(null);
        var bed = courier.getSleepingPos().orElse(null);
        if (house == null || bed == null || courier.getUUID().equals(courierId) && !house.id().equals(houseId)) return false;
        for (BlockPos position : BlockPos.betweenClosed(bed.offset(-1, -1, -1), bed.offset(1, 1, 1)))
            if (house.shape().contains(position)) return true;
        return false;
    }

    private Map<UUID, VillagerRealmsVillager> residents() {
        Map<UUID, VillagerRealmsVillager> found = new LinkedHashMap<>();
        for (ServerLevel level : List.of(desert, nether))
            for (Entity entity : level.getAllEntities()) if (entity instanceof VillagerRealmsVillager worker) {
                require(found.put(worker.getUUID(), worker) == null, "Duplicate resident UUID");
            }
        return found;
    }

    private boolean ready() {
        Set<Long> a = VillageChunkTickingService.activeChunks(desert), b = VillageChunkTickingService.activeChunks(nether);
        if (a.size() != 289 || !a.equals(b)) return false;
        for (long packed : a) {
            ChunkPos chunk = new ChunkPos(packed);
            if (!desert.isPositionEntityTicking(new BlockPos(chunk.getMinBlockX() + 8, 74, chunk.getMinBlockZ() + 8))
                    || !nether.isPositionEntityTicking(new BlockPos(chunk.getMinBlockX() + 8, 18, chunk.getMinBlockZ() + 8))) return false;
        }
        return true;
    }

    private Map<String, Object> courierSnapshot(VillagerRealmsVillager worker) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("uuid", worker.getUUID().toString());
        value.put("dimension", worker.level().dimension().location().toString());
        value.put("position", List.of(worker.getX(), worker.getY(), worker.getZ()));
        value.put("thought", worker.thoughtText());
        value.put("sleeping", worker.isSleeping());
        value.put("bed", worker.getSleepingPos().map(BlockPos::toShortString).orElse("none"));
        value.put("house", HousingSavedData.get((ServerLevel) worker.level()).houseFor(worker.getUUID()).map(UUID::toString).orElse("none"));
        value.put("workplace", worker.getAssignments().workplace().map(UUID::toString).orElse("none"));
        value.put("cargo", WorkerInventory.contents(worker.getInventory()).toString());
        var shopping = worker.getPersistentData().getCompound("VillagerRealmsRemoteSupplies");
        value.put("queuedShopping", Map.of("active", !shopping.isEmpty(), "requestedCount", shopping.getInt("count")));
        value.put("observedWalkingDistance", travelled);
        return value;
    }

    private Map<String, Object> snapshot(Map<UUID, VillagerRealmsVillager> residents) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("at", Instant.now().toString());
        value.put("residents", residents.size());
        value.put("alive", residents.values().stream().filter(Entity::isAlive).count());
        value.put("sleeping", residents.values().stream().filter(VillagerRealmsVillager::isSleeping).count());
        value.put("players", server.getPlayerList().getPlayerCount());
        value.put("overworldDayTime", server.overworld().getDayTime());
        value.put("desertTickets", VillageChunkTickingService.activeChunks(desert).size());
        value.put("netherTickets", VillageChunkTickingService.activeChunks(nether).size());
        value.put("allResidentIds", residents.keySet().stream().map(UUID::toString).sorted().toList());
        if (residents.containsKey(courierId)) value.put("courier", courierSnapshot(residents.get(courierId)));
        return value;
    }

    private void progress(Map<UUID, VillagerRealmsVillager> residents) {
        if (secondsSince(lastProgress) < 30) return;
        long diagnostic = System.nanoTime();
        lastProgress = diagnostic;
        Map<String, Object> sample = snapshot(residents);
        sample.put("measuredSeconds", BenchmarkMetrics.elapsedSeconds());
        history.add(sample);
        report.put("progress", sample);
        write();
        BenchmarkMetrics.recordDiagnosticNanos("recovery_snapshot_and_report", System.nanoTime() - diagnostic);
    }

    private void finish(Map<UUID, VillagerRealmsVillager> residents) {
        report.put("metrics", BenchmarkMetrics.finish());
        report.put("final", snapshot(residents));
        report.put("status", "completed");
        report.put("passed", true);
        report.put("finishedAt", Instant.now().toString());
        finished = true;
        write();
        server.halt(false);
    }

    private void fail(Throwable failure) {
        if (BenchmarkMetrics.isActive()) report.put("metrics", BenchmarkMetrics.finish());
        report.put("status", "failed");
        report.put("passed", false);
        report.put("failure", failure.toString());
        report.put("finishedAt", Instant.now().toString());
        finished = true;
        write();
        server.halt(false);
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) {
        if (event.getServer() == server && !finished) fail(new IllegalStateException("Recovery server stopped before completion"));
    }

    private void write() {
        try {
            Files.createDirectories(output.getParent());
            Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception error) { throw new IllegalStateException("Cannot write recovery report", error); }
    }

    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private static double secondsSince(long nanos) { return (System.nanoTime() - nanos) / 1_000_000_000.0; }
}
