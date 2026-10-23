package net.amurdza.stackbench;

import com.villagerrealms.entity.VillagerRealmsVillager;
import com.villagerrealms.logistics.VillagerAssignments;
import com.villagerrealms.policy.PermissionState;
import com.villagerrealms.policy.PolicyPriority;
import com.villagerrealms.policy.PolicyRule;
import com.villagerrealms.profession.ProfessionIds;
import com.villagerrealms.profession.cook.CookRecipe;
import com.villagerrealms.settlement.RemoteWorkSupplies;
import com.villagerrealms.structure.StructureTypeIds;
import com.villagerrealms.work.runtime.StorageRoomIndex;
import com.villagerrealms.work.runtime.WorkerInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.*;

/** QA observations and finite fixture provisioning only: workers perform all travel and transactions. */
public final class BenchmarkCargoScenario {
    private final BenchmarkVillageFixture fixture;
    private final BenchmarkVillageFixture.Room surfaceFood, surfaceCook, bottomCook, bottomGold, bottomLibrary;
    private final List<Flow> flows = new ArrayList<>();
    private final Set<UUID> couriers = new HashSet<>();
    private final List<String> violations = new ArrayList<>();
    private final Map<String, Integer> seeded = new LinkedHashMap<>();
    private int population;
    private long phaseTick;

    private static final class Flow {
        final String name;
        final UUID worker;
        final BenchmarkVillageFixture.Room source, destination;
        final Item input, output;
        final int inputPerOutput;
        int previousSource, previousOutput, pickedUp, deposited, phaseDeposited, phasePickups, tripDeliveries;
        boolean pickedUpThisTrip;
        long lastPickupTick, lastDepositTick;
        final long createdNanos = System.nanoTime();
        long firstPickupNanos, firstDepositNanos;
        Flow(String name, VillagerRealmsVillager worker, BenchmarkVillageFixture.Room source,
             BenchmarkVillageFixture.Room destination, Item input, Item output, int inputPerOutput) {
            this.name = name; this.worker = worker.getUUID(); this.source = source; this.destination = destination;
            this.input = input; this.output = output; this.inputPerOutput = inputPerOutput;
        }
    }

    public static BenchmarkCargoScenario setup(BenchmarkVillageFixture fixture) {
        return new BenchmarkCargoScenario(fixture);
    }

    private BenchmarkCargoScenario(BenchmarkVillageFixture fixture) {
        this.fixture = fixture;
        surfaceFood = fixture.room("cargo_surface_granary", fixture.desert(), -48, fixture.surfaceY(), 16, StructureTypeIds.GRANARY);
        surfaceCook = fixture.room("cargo_surface_restaurant", fixture.desert(), -32, fixture.surfaceY(), 32, StructureTypeIds.RESTAURANT);
        bottomCook = fixture.room("cargo_bottom_restaurant", fixture.nether(), -48, 18, 16, StructureTypeIds.RESTAURANT);
        bottomGold = fixture.room("cargo_bottom_gold", fixture.nether(), -32, 18, 32, StructureTypeIds.MINE_ENTRANCE);
        bottomLibrary = fixture.room("cargo_bottom_library", fixture.nether(), -16, 18, 16, StructureTypeIds.LIBRARY);
        fixture.put(bottomCook.level(), bottomCook.origin().offset(3, 0, 3), Blocks.SMOKER.defaultBlockState());
        fixture.put(bottomLibrary.level(), bottomLibrary.origin().offset(3, 0, 3), Blocks.LECTERN.defaultBlockState());
        com.villagerrealms.logistics.storage.StructureLinksSavedData.get(bottomCook.level()).link(bottomCook.structure().id(),
                bottomCook.origin().offset(3, 0, 3), com.villagerrealms.logistics.storage.StructureLinksSavedData.Kind.WORKSTATION);
        com.villagerrealms.logistics.storage.StructureLinksSavedData.get(bottomLibrary.level()).link(bottomLibrary.structure().id(),
                bottomLibrary.origin().offset(3, 0, 3), com.villagerrealms.logistics.storage.StructureLinksSavedData.Kind.WORKSTATION);
        var down = fixture.spawnWorker(fixture.nether(), bottomCook.entrance().south(2), ProfessionIds.COOK,
                bottomCook.structure().id(), "cargo_rabbit_down");
        var up = fixture.spawnWorker(fixture.desert(), surfaceCook.entrance().south(2), ProfessionIds.COOK,
                surfaceCook.structure().id(), "cargo_gold_up");
        var paper = fixture.spawnWorker(fixture.nether(), bottomLibrary.entrance().south(2), ProfessionIds.LIBRARIAN,
                bottomLibrary.structure().id(), "cargo_paper_down");
        allowOnly(down, CookRecipe.RABBIT);
        allowOnly(up, CookRecipe.GOLDEN_CARROT);
        // Each cook uses its own restaurant, keeping both measured recipes independent.
        forbid(down, Set.of(surfaceCook.structure().id()));
        forbid(up, Set.of(bottomCook.structure().id()));
        forbid(paper, fixture.rooms().values().stream().filter(room -> room.name().contains("background"))
                .map(room -> room.structure().id()).collect(java.util.stream.Collectors.toSet()));
        flows.add(new Flow("rabbit_down", down, surfaceFood, bottomCook, Items.RABBIT, Items.COOKED_RABBIT, 1));
        flows.add(new Flow("gold_up", up, bottomGold, surfaceCook, Items.GOLD_NUGGET, Items.GOLDEN_CARROT, 8));
        flows.add(new Flow("paper_down", paper, surfaceFood, bottomLibrary, Items.SUGAR_CANE, Items.PAPER, 1));
        for (Flow flow : flows) couriers.add(flow.worker);
    }

    /** Called before a population phase warms up; finite initial inventory is reported explicitly. */
    public void onPopulation(int target) {
        population = target;
        phaseTick = fixture.desert().getGameTime();
        var protectedRooms = Set.of(surfaceFood.structure().id(), surfaceCook.structure().id(),
                bottomCook.structure().id(), bottomGold.structure().id(), bottomLibrary.structure().id());
        for (var worker : fixture.workers()) {
            if (!couriers.contains(worker.getUUID())) forbid(worker, protectedRooms);
            // Normal per-villager diet policy protects measured goods; ordinary house food remains available.
            for (String item : List.of("rabbit", "cooked_rabbit", "golden_carrot"))
                worker.setPolicyOverrides(worker.getPolicyOverrides().withRule(net.minecraft.resources.ResourceLocation.parse(
                        "villager_realms:common/eat/minecraft/" + item), new PolicyRule(PermissionState.FORBID, PolicyPriority.NORMAL)));
        }
        topUp(surfaceFood, Items.RABBIT, 8, 0, "surface_raw_rabbit");
        topUp(surfaceFood, Items.SUGAR_CANE, 12, 1, "surface_sugar_cane");
        topUp(bottomGold, Items.GOLD_NUGGET, 32, 0, "bottom_gold_nugget");
        topUp(surfaceCook, Items.CARROT, 16, 0, "surface_carrot");
        topUp(bottomCook, Items.COAL, 4, 0, "bottom_coal");
        for (Flow flow : flows) {
            flow.previousSource = count(flow.source.chest(), flow.input);
            flow.previousOutput = count(flow.destination.chest(), flow.output);
            flow.phaseDeposited = flow.deposited; flow.phasePickups = flow.pickedUp;
        }
    }

    private void topUp(BenchmarkVillageFixture.Room room, Item item, int target, int preferredSlot, String key) {
        int missing = Math.max(0, target - count(room.chest(), item));
        if (missing == 0) return;
        // This is setup provisioning, never an in-flight transfer or periodic replenishment.
        for (int offset = 0; offset < room.chest().getContainerSize() && missing > 0; offset++) {
            int slot = (preferredSlot + offset) % room.chest().getContainerSize();
            ItemStack old = room.chest().getItem(slot);
            if (!old.isEmpty() && !old.is(item)) continue;
            int added = Math.min(missing, item.getMaxStackSize() - old.getCount());
            if (added <= 0) continue;
            room.chest().setItem(slot, com.villagerrealms.logistics.storage.VillageGoods.creativeSupplied(
                    UUID.nameUUIDFromBytes("stackbench-cargo-setup".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    new ItemStack(item, old.getCount() + added)));
            seeded.merge(key, added, Integer::sum); missing -= added;
        }
        room.chest().setChanged();
        StorageRoomIndex.invalidate(room.level(), room.structure().id());
    }

    /** Measurement boundary only: preserve live trips and stock, and exclude warmup deliveries. */
    public void beginMeasurement() {
        phaseTick = fixture.desert().getGameTime();
        for (Flow flow : flows) {
            flow.phaseDeposited = flow.deposited;
            flow.phasePickups = flow.pickedUp;
        }
    }

    /** Observe the ordinary entity tick; this method never moves a worker or advances a job. */
    public void tick() {
        for (Flow flow : flows) {
            var worker = resolve(flow.worker);
            int source = count(flow.source.chest(), flow.input);
            int output = count(flow.destination.chest(), flow.output);
            if (source < flow.previousSource) {
                int removed = flow.previousSource - source;
                if (worker == null || worker.level() != flow.source.level()
                        || flow.source.chestPos().distToCenterSqr(worker.position()) > 36)
                    violation(flow.name + ": source inventory decreased without its courier physically at the chest");
                else {
                    flow.pickedUp += removed; flow.pickedUpThisTrip = true;
                    flow.lastPickupTick = fixture.desert().getGameTime();
                    if (flow.firstPickupNanos == 0) flow.firstPickupNanos = System.nanoTime();
                }
            }
            if (output > flow.previousOutput) {
                int added = output - flow.previousOutput;
                if (worker == null || worker.level() != flow.destination.level()
                        || flow.destination.chestPos().distToCenterSqr(worker.position()) > 36)
                    violation(flow.name + ": output appeared without its courier physically at the destination chest");
                else {
                    flow.deposited += added; flow.lastDepositTick = fixture.desert().getGameTime();
                    if (flow.firstDepositNanos == 0) flow.firstDepositNanos = System.nanoTime();
                    if (flow.pickedUpThisTrip) { flow.tripDeliveries++; flow.pickedUpThisTrip = false; }
                }
            }
            if (worker != null && worker.getSimulationState().villageId().filter(fixture.village().id()::equals).isEmpty())
                violation(flow.name + ": courier lost village membership");
            flow.previousSource = source; flow.previousOutput = output;
        }
    }

    public Map<String, Object> report() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("population", population); result.put("couriers", couriers.size());
        result.put("phase_started_tick", phaseTick); result.put("setup_seeded_total", Map.copyOf(seeded));
        result.put("violations", List.copyOf(violations));
        List<Map<String, Object>> routes = new ArrayList<>();
        for (Flow flow : flows) {
            var worker = resolve(flow.worker);
            Map<String, Object> route = new LinkedHashMap<>();
            route.put("name", flow.name); route.put("uuid", flow.worker.toString());
            route.put("source_dimension", flow.source.level().dimension().location().toString());
            route.put("source_y", flow.source.chestPos().getY());
            route.put("destination_dimension", flow.destination.level().dimension().location().toString());
            route.put("destination_y", flow.destination.chestPos().getY());
            route.put("picked_up", flow.pickedUp); route.put("deposited", flow.deposited);
            route.put("round_trip_deliveries", flow.tripDeliveries);
            route.put("phase_picked_up", flow.pickedUp - flow.phasePickups);
            route.put("phase_deposited", flow.deposited - flow.phaseDeposited);
            route.put("last_pickup_tick", flow.lastPickupTick); route.put("last_deposit_tick", flow.lastDepositTick);
            if (flow.firstPickupNanos != 0) route.put("first_pickup_seconds_since_spawn", (flow.firstPickupNanos - flow.createdNanos) / 1_000_000_000.0);
            if (flow.firstDepositNanos != 0) route.put("first_deposit_seconds_since_spawn", (flow.firstDepositNanos - flow.createdNanos) / 1_000_000_000.0);
            route.put("input_current", total(flow, flow.input)); route.put("output_current", total(flow, flow.output));
            route.put("input_per_output", flow.inputPerOutput);
            route.put("output_note", "Measured food is protected by ordinary per-villager diet policy; other house meals remain available.");
            if (worker != null) {
                route.put("dimension", worker.level().dimension().location().toString());
                route.put("position", List.of(worker.getX(), worker.getY(), worker.getZ()));
                route.put("thought", worker.thoughtText()); route.put("cargo", WorkerInventory.contents(worker.getInventory()).toString());
                var path = worker.getNavigation().getPath();
                route.put("navigation", path == null ? Map.of("active", false) : Map.of("active", true,
                        "next_node", path.getNextNodeIndex(), "node_count", path.getNodeCount(),
                        "can_reach", path.canReach(), "target", path.getTarget().toShortString()));
                var shopping = worker.getPersistentData().getCompound("VillagerRealmsRemoteSupplies");
                route.put("remote_shopping", Map.of("active", !shopping.isEmpty(),
                        "returning", shopping.getBoolean("returning"), "requested_count", shopping.getInt("count")));
                route.put("remote_sources", RemoteWorkSupplies.sources((net.minecraft.server.level.ServerLevel) worker.level(), worker).size());
                route.put("task_failures", List.copyOf(worker.taskFailureReasons()));
                route.put("assigned_workplace", worker.getAssignments().workplace().map(UUID::toString).orElse("none"));
                var workplace = com.villagerrealms.structure.data.StructureSavedData.get(flow.destination.level())
                        .get(flow.destination.structure().id()).orElse(null);
                route.put("workplace_valid", workplace != null && workplace.valid());
                route.put("workplace_eligible", workplace != null && com.villagerrealms.work.runtime.WorkplaceService
                        .isEligible(flow.destination.level(), worker, workplace));
                route.put("source_valid", com.villagerrealms.structure.data.StructureSavedData.get(flow.source.level())
                        .get(flow.source.structure().id()).map(com.villagerrealms.structure.data.RegisteredStructure::valid).orElse(false));
            } else route.put("missing_worker", true);
            routes.add(route);
        }
        result.put("routes", routes);
        result.put("both_directions_completed", flows.get(0).deposited > 0 && flows.get(1).deposited > 0 && violations.isEmpty());
        result.put("phase_both_directions_completed", flows.get(0).deposited > flows.get(0).phaseDeposited
                && flows.get(1).deposited > flows.get(1).phaseDeposited && violations.isEmpty());
        result.put("rabbit_input_output_conserved", total(flows.get(0), Items.RABBIT) + total(flows.get(0), Items.COOKED_RABBIT)
                == seeded.getOrDefault("surface_raw_rabbit", 0));
        result.put("gold_input_output_conserved", total(flows.get(1), Items.GOLD_NUGGET) + 8 * total(flows.get(1), Items.GOLDEN_CARROT)
                == seeded.getOrDefault("bottom_gold_nugget", 0));
        result.put("paper_input_output_conserved", total(flows.get(2), Items.SUGAR_CANE) + total(flows.get(2), Items.PAPER)
                == seeded.getOrDefault("surface_sugar_cane", 0));
        return result;
    }

    private int total(Flow flow, Item item) {
        int count = 0;
        for (var room : List.of(flow.source, flow.destination)) {
            count += count(room.chest(), item);
            for (BlockPos pos : room.structure().shape().positions())
                if (room.level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) count += count(furnace, item);
        }
        var worker = resolve(flow.worker);
        if (worker != null) {
            count += WorkerInventory.count(worker, item);
            ItemStack meal = WorkerInventory.reservedMeal(worker);
            if (meal.is(item)) count += meal.getCount();
        }
        return count;
    }

    private VillagerRealmsVillager resolve(UUID id) {
        for (var level : List.of(fixture.desert(), fixture.nether()))
            if (level.getEntity(id) instanceof VillagerRealmsVillager worker) return worker;
        return null;
    }
    private void violation(String value) { if (!violations.contains(value)) violations.add(value); }
    private static int count(Container container, Item item) { return WorkerInventory.count(container, item); }
    private static void allowOnly(VillagerRealmsVillager worker, CookRecipe allowed) {
        for (CookRecipe recipe : CookRecipe.values()) if (recipe != allowed)
            worker.setPolicyOverrides(worker.getPolicyOverrides().withRule(recipe.actionId(),
                    new PolicyRule(PermissionState.FORBID, PolicyPriority.NORMAL)));
    }
    private static void forbid(VillagerRealmsVillager worker, Set<UUID> rooms) {
        var current = worker.getAssignments(); var forbidden = new HashSet<>(current.forbiddenStructures());
        forbidden.addAll(rooms);
        worker.setAssignments(new VillagerAssignments(current.workplace().orElse(null), current.preferredPickupStructures(),
                current.preferredDropOffStructures(), forbidden));
    }
}
