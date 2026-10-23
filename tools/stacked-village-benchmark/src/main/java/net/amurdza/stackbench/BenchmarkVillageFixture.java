package net.amurdza.stackbench;

import com.villagerrealms.entity.VillagerRealmsEntityTypes;
import com.villagerrealms.entity.VillagerRealmsVillager;
import com.villagerrealms.housing.HousingAssignmentService;
import com.villagerrealms.logistics.VillagerAssignments;
import com.villagerrealms.logistics.storage.StructureLinksSavedData;
import com.villagerrealms.logistics.storage.VillageGoods;
import com.villagerrealms.profession.ProfessionAssignmentService;
import com.villagerrealms.profession.ProfessionIds;
import com.villagerrealms.profession.librarian.LibraryWorkSettings;
import com.villagerrealms.settlement.*;
import com.villagerrealms.structure.StructureTypeIds;
import com.villagerrealms.structure.StructureIntegrityService;
import com.villagerrealms.structure.data.*;
import com.villagerrealms.structure.detection.RoomDetector;
import com.villagerrealms.work.runtime.WorkerInventory;
import com.villagerrealms.work.runtime.StorageRoomIndex;
import com.villagerrealms.work.runtime.WorkplaceService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Isolated, physical village for the optional production benchmark mod. No entity is stepped or ticked manually. */
public final class BenchmarkVillageFixture {
    public static final int BOTTOM_Y = 18;
    public record Room(String name, ServerLevel level, RegisteredStructure structure, BlockPos origin,
                       BlockPos entrance, BlockPos chestPos, ChestBlockEntity chest, List<BlockPos> beds) { }

    private final MinecraftServer server;
    private final ServerLevel desert, nether;
    private final BlockPos center;
    private final Site site;
    private final int surfaceY;
    private int highestNaturalPlazaColumn;
    private record Site(BlockPos center, int minimumHeight, int maximumHeight, List<Integer> sampledHeights) { }
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final List<UUID> workerIds = new ArrayList<>();
    private final Map<UUID, ServerLevel> homeLevels = new LinkedHashMap<>();
    private final Map<ServerLevel, List<Room>> homes = new IdentityHashMap<>();
    private final Map<ServerLevel, List<Room>> backgroundJobs = new IdentityHashMap<>();
    private final List<BlockPos> desertStairs, netherStairs;
    private final UUID supplyIdentity = UUID.nameUUIDFromBytes("stackbench-initial-supply".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private Settlement village;
    private int setupAddedCane, setupExportedPaper;

    public static BenchmarkVillageFixture setup(MinecraftServer server) { return new BenchmarkVillageFixture(server); }

    private BenchmarkVillageFixture(MinecraftServer server) {
        this.server = server;
        desert = Objects.requireNonNull(server.getLevel(dimension("desert_dimension")), "AoE Desert missing");
        nether = Objects.requireNonNull(server.getLevel(dimension("nether_dimension")), "AoE Nether missing");
        site = findSite(desert);
        center = site.center();
        surfaceY = Math.max(65, site.maximumHeight());
        // A contiguous plaza keeps every room and the lower staircase within ordinary local-navigation range.
        plaza(desert, surfaceY);
        plaza(nether, BOTTOM_Y);
        Room town = room("town_center", desert, -11, surfaceY, -11, StructureTypeIds.TOWN_CENTER);
        put(desert, town.origin().offset(3, 0, 3), Blocks.LECTERN.defaultBlockState());
        linkWorkstation(town, town.origin().offset(3, 0, 3));
        TownCenterSettlementService.reconcile(desert);
        village = SettlementSavedData.get(desert).findByTownCenter(town.structure().id()).orElseThrow();
        require(village.area().centerX() == center.getX() && village.area().centerZ() == center.getZ(), "Town anchor changed benchmark center");
        own(town);
        require(com.villagerrealms.config.StackedVillageDimensions.between(desert, nether)
                .filter(link -> link.travel() && link.ticking() && link.crossings().stream().anyMatch(crossing ->
                        crossing.offsetX() == 0 && crossing.offsetZ() == 0 && crossing.upperY() == -30 && crossing.lowerY() == 125))
                .isPresent(), "Enable the real AoE stacked-villages datapack with its center crossing before setup");
        BlockPos upperLanding = center.atY(-30), lowerLanding = center.atY(125);
        desertStairs = stair(upperLanding, surfaceY + 30, false);
        netherStairs = stair(lowerLanding, 125 - BOTTOM_Y, true);
        buildStair(desert, desertStairs);
        buildStair(nether, netherStairs);
        // The plaza is west/north of the shaft; connect the high surface and low Nether ends at their actual heights.
        BlockPos surfaceEnd = desertStairs.get(desertStairs.size() - 1);
        connectSurfaceEnd(surfaceEnd);
        connect(nether, netherStairs.get(0), center.offset(-12, BOTTOM_Y, 0));
        landing(desert, upperLanding);
        landing(nether, lowerLanding);
        for (ServerLevel level : List.of(desert, nether)) {
            int y = level == desert ? surfaceY : BOTTOM_Y;
            String prefix = level == desert ? "surface" : "bottom";
            List<Room> houses = new ArrayList<>(), jobs = new ArrayList<>();
            homes.put(level, houses); backgroundJobs.put(level, jobs);
            for (int i = 0; i < 8; i++) {
                Room house = room(prefix + "_house_" + i, level, -108 + (i % 4) * 16, y, -108 + (i / 4) * 16, StructureTypeIds.HOUSE);
                List<BlockPos> beds = new ArrayList<>();
                for (int row = 0; row < 2; row++) for (int column = 0; column < 4; column++) {
                    BlockPos foot = house.origin().offset(1 + column * 2, 0, 2 + row * 3);
                    BlockState bed = Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH);
                    put(level, foot, bed.setValue(BedBlock.PART, BedPart.FOOT));
                    put(level, foot.south(), bed.setValue(BedBlock.PART, BedPart.HEAD));
                    beds.add(foot);
                }
                house = new Room(house.name(), level, house.structure(), house.origin(), house.entrance(), house.chestPos(), house.chest(), List.copyOf(beds));
                rooms.put(house.name(), house); houses.add(house);
                stock(house.chest(), Items.COOKED_CHICKEN, 128);
            }
            for (int i = 0; i < 4; i++) {
                Room job = room(prefix + "_background_library_" + i, level, -48 + (i % 2) * 16, y, -48 + (i / 2) * 16, StructureTypeIds.LIBRARY);
                Room source = room(prefix + "_background_granary_" + i, level, -80 + (i % 2) * 16, y, -48 + (i / 2) * 16, StructureTypeIds.GRANARY);
                jobs.add(job); stock(source.chest(), Items.SUGAR_CANE, 1024);
                for (int x : new int[] {1, 3, 5, 7}) {
                    BlockPos station = job.origin().offset(x, 0, 1);
                    put(level, station, Blocks.LECTERN.defaultBlockState());
                    linkWorkstation(job, station);
                }
                BlockPos furnacePos = job.origin().offset(7, 0, 6);
                put(level, furnacePos, (level == nether ? block("quark:blackstone_furnace") : Blocks.FURNACE).defaultBlockState());
                require(level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity,
                        "Background smelter must expose an ordinary furnace inventory: " + furnacePos);
                var furnace = (AbstractFurnaceBlockEntity) level.getBlockEntity(furnacePos);
                furnace.setItem(0, supply(new ItemStack(Items.RAW_GOLD, 64)));
                furnace.setItem(1, supply(new ItemStack(Items.CHARCOAL, 64)));
                furnace.setChanged();
            }
            Room crops = room(prefix + "_crop_plot", level, -16, y, -32, StructureTypeIds.FARMLAND);
            for (int dx : new int[] {2, 5}) for (int dz : new int[] {2, 5}) {
                BlockPos crop = crops.origin().offset(dx, 0, dz);
                if (level == desert) {
                    put(level, crop.below(), Blocks.SAND.defaultBlockState());
                    put(level, crop, Blocks.CACTUS.defaultBlockState());
                    put(level, crop.above(), Blocks.CACTUS.defaultBlockState());
                } else {
                    Block cane = block("mynethersdelight:powdery_cane");
                    put(level, crop.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
                    put(level, crop, cane.defaultBlockState());
                }
            }
        }
        VillageChunkTickingService.refresh(server);
        require(StackedVillageTravel.safe(desert, upperLanding) && StackedVillageTravel.safe(nether, lowerLanding), "Unsafe benchmark seam landing");
        validateStair(desert, desertStairs);
        validateStair(nether, netherStairs);
        for (Room room : rooms.values()) require(StructureIntegrityService.revalidate(room.level(), room.structure().id())
                == StructureIntegrityService.Result.VALID, "Fixture construction damaged " + room.name());
    }

    public Room room(String name, ServerLevel level, int relativeX, int feetY, int relativeZ, ResourceLocation type) {
        if (rooms.containsKey(name)) throw new IllegalArgumentException("Duplicate room " + name);
        BlockPos origin = center.offset(relativeX, feetY, relativeZ);
        BlockState material = material(level);
        for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) for (int y = -1; y <= 4; y++) {
            boolean shell = y == -1 || y == 4 || x == 0 || x == 9 || z == 0 || z == 9;
            put(level, origin.offset(x, y, z), shell ? material : Blocks.AIR.defaultBlockState());
        }
        BlockPos entrance = origin.offset(5, 0, 9), sign = origin.offset(4, 0, 10);
        BlockState door = block("aoemod:pale_oak_door").defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        put(level, entrance, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        put(level, entrance.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        put(level, sign, block("minecraft:pale_oak_wall_sign").defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH));
        put(level, origin.offset(5, 4, 5), Blocks.GLOWSTONE.defaultBlockState());
        BlockPos chestPos = origin.offset(2, 0, 7);
        put(level, chestPos, Blocks.CHEST.defaultBlockState());
        put(level, origin.offset(7, 0, 7), Blocks.CRAFTING_TABLE.defaultBlockState());
        var detection = new RoomDetector().detectRoom(level, entrance, sign, Direction.NORTH);
        require(detection.isSuccess(), "Room " + name + " failed detection: " + detection.status());
        var result = StructureSavedData.get(level).registerOrLink(type, RoomShape.of(detection.interiorPositions()), new StructureMarker(sign, entrance), false);
        require(result.succeeded(), "Room " + name + " registration failed: " + result.status());
        var structure = result.resultingStructure().orElseThrow();
        StructureLinksSavedData.get(level).link(structure.id(), chestPos, StructureLinksSavedData.Kind.CHEST);
        Room room = new Room(name, level, structure, origin, entrance, chestPos, (ChestBlockEntity) level.getBlockEntity(chestPos), List.of());
        rooms.put(name, room);
        linkWorkstation(room, origin.offset(7, 0, 7));
        if (village != null) own(room);
        // A three-wide aisle two blocks from the door would erase the sign mounted beside it.
        connect(level, entrance.south(3), center.offset(-12, feetY, relativeZ + 12));
        require(StructureIntegrityService.revalidate(level, structure.id()) == StructureIntegrityService.Result.VALID,
                "Room marker or doorway invalid after connecting " + name);
        return room;
    }

    public void linkWorkstation(Room room, BlockPos position) {
        require(StructureLinksSavedData.get(room.level()).link(room.structure().id(), position,
                StructureLinksSavedData.Kind.WORKSTATION), "Cannot link fixture workstation " + position);
    }

    public VillagerRealmsVillager spawnWorker(ServerLevel level, BlockPos start, ResourceLocation profession, UUID workplace, String role) {
        var worker = Objects.requireNonNull(VillagerRealmsEntityTypes.villager().create(level));
        worker.moveTo(start.getX() + .5, start.getY(), start.getZ() + .5);
        worker.finalizeSpawn(level, level.getCurrentDifficultyAt(start), MobSpawnType.SPAWN_EGG, null, null);
        worker.setAge(0); worker.setPersistenceRequired();
        worker.getSimulationState().setHunger(1); worker.getSimulationState().setWellness(1);
        require(ProfessionAssignmentService.assign(worker, profession).succeeded(), "Cannot assign " + profession);
        worker.setAssignments(new VillagerAssignments(workplace, Map.of(), Map.of(), Set.of()));
        require(SettlementSavedData.get(desert).assignVillager(village.id(), worker.getSimulationState()).succeeded(), "Cannot enroll benchmark worker");
        worker.getPersistentData().putString("StackBenchRole", role);
        if (profession.equals(ProfessionIds.FARMER)) worker.setItemSlot(EquipmentSlot.MAINHAND, supply(new ItemStack(Items.IRON_HOE)));
        require(level.addFreshEntity(worker), "Cannot spawn benchmark worker");
        require(WorkplaceService.resolve(level, worker).filter(room -> room.id().equals(workplace)).isPresent(),
                "New worker's assigned workplace is not eligible: " + role);
        int localCount = (int) homeLevels.values().stream().filter(value -> value == level).count();
        List<Room> houses = homes.get(level);
        if (houses != null && localCount < 64) require(HousingAssignmentService.assignHouse(level, worker,
                houses.get(localCount / 8).structure().id()).succeeded(), "Cannot claim benchmark bed");
        workerIds.add(worker.getUUID()); homeLevels.put(worker.getUUID(), level);
        return worker;
    }

    public void addPopulation(int target) {
        require(target >= workerIds.size() && target <= 128, "Benchmark population must increase up to128");
        while (workerIds.size() < target) {
            long upperCount = homeLevels.values().stream().filter(level -> level == desert).count();
            long lowerCount = homeLevels.size() - upperCount;
            ServerLevel level = upperCount <= lowerCount ? desert : nether;
            int local = (int) (level == desert ? upperCount : lowerCount);
            Room job = backgroundJobs.get(level).get(local % 4);
            spawnWorker(level, job.entrance().south(2).east(local % 3 - 1), ProfessionIds.LIBRARIAN,
                    job.structure().id(), "background_librarian_" + workerIds.size());
        }
    }

    /** Reset finite background demand between phases, never during a measured interval. */
    public void preparePopulationStock() {
        for (Room room : rooms.values()) {
            if (!room.name().contains("_background_")) continue;
            var inventory = com.villagerrealms.logistics.storage.LogicalStorage.container(room.level(), room.chestPos(), room.chest());
            var plan = WorkerInventory.copy(inventory);
            int exported = 0, added = 0;
            if (room.name().contains("_library_")) {
                exported = WorkerInventory.count(plan, Items.PAPER);
                WorkerInventory.consume(plan, Items.PAPER, exported);
            } else if (room.name().contains("_granary_")) {
                added = Math.max(0, 1024 - WorkerInventory.count(plan, Items.SUGAR_CANE));
                require(WorkerInventory.add(plan, supply(new ItemStack(Items.SUGAR_CANE, added))).isEmpty(),
                        "Insufficient setup stock capacity in " + room.name());
            }
            com.villagerrealms.logistics.storage.LogicalStorage.applyPlan(inventory, plan);
            setupExportedPaper += exported;
            setupAddedCane += added;
            StorageRoomIndex.invalidate(room.level(), room.structure().id());
        }
    }

    public boolean ready() {
        Set<Long> a = VillageChunkTickingService.activeChunks(desert), b = VillageChunkTickingService.activeChunks(nether);
        if (a.size() != 289 || !a.equals(b)) return false;
        for (long packed : a) {
            var chunk = new net.minecraft.world.level.ChunkPos(packed);
            if (!desert.isPositionEntityTicking(new BlockPos(chunk.getMinBlockX() + 8, surfaceY, chunk.getMinBlockZ() + 8))
                    || !nether.isPositionEntityTicking(new BlockPos(chunk.getMinBlockX() + 8, BOTTOM_Y, chunk.getMinBlockZ() + 8))) return false;
        }
        return true;
    }

    public Map<String, Object> readinessReport() {
        return Map.of("ready", ready(), "desertTickets", VillageChunkTickingService.activeChunks(desert).size(),
                "netherTickets", VillageChunkTickingService.activeChunks(nether).size(), "workers", workers().size(),
                "registeredWorkers", workerIds.size(), "rooms", rooms.size(), "center", center.toShortString(),
                "surfaceTerrain", Map.of("feetY", surfaceY, "sampledMinimum", site.minimumHeight(),
                        "sampledMaximum", site.maximumHeight(), "sampledHeights", site.sampledHeights(),
                        "highestNaturalPlazaColumn", highestNaturalPlazaColumn,
                        "grading", "Floor follows the selected lowland's sampled maximum; higher unsampled columns were opened to sky."));
    }

    /** Actual inventories only; the driver records before/after snapshots rather than counting attempted jobs. */
    public Map<String, Object> backgroundProductionReport() {
        Set<UUID> workshopIds = new HashSet<>();
        int cane = 0, paper = 0, rawGold = 0, goldIngots = 0, lecterns = 0, workshops = 0, validWorkshops = 0, linkedStations = 0;
        for (Room room : rooms.values()) {
            if (!room.name().contains("_background_")) continue;
            for (ItemStack stack : StorageRoomIndex.stored(room.level(), room.structure())) {
                if (stack.is(Items.SUGAR_CANE)) cane += stack.getCount();
                if (stack.is(Items.PAPER)) paper += stack.getCount();
            }
            if (!room.name().contains("_library_")) continue;
            workshops++; workshopIds.add(room.structure().id());
            RegisteredStructure current = StructureSavedData.get(room.level()).get(room.structure().id()).orElse(null);
            if (current != null && current.valid()) {
                validWorkshops++;
                linkedStations += WorkplaceService.workstations(room.level(), current,
                        state -> state.is(Blocks.LECTERN) || state.is(Blocks.CRAFTING_TABLE)).size();
            }
            for (int x : new int[] {1, 3, 5, 7})
                if (room.level().getBlockState(room.origin().offset(x, 0, 1)).is(Blocks.LECTERN)) lecterns++;
            if (room.level().getBlockEntity(room.origin().offset(7, 0, 6)) instanceof AbstractFurnaceBlockEntity furnace) {
                rawGold += WorkerInventory.count(furnace, Items.RAW_GOLD);
                goldIngots += WorkerInventory.count(furnace, Items.GOLD_INGOT);
            }
        }
        int backgroundWorkers = 0, assignedWorkers = 0, eligibleWorkers = 0, workingWorkers = 0;
        for (VillagerRealmsVillager worker : workers()) {
            if (!worker.getPersistentData().getString("StackBenchRole").startsWith("background_")) continue;
            backgroundWorkers++;
            if (worker.getAssignments().workplace().filter(workshopIds::contains).isPresent()) assignedWorkers++;
            var level = (ServerLevel) worker.level();
            if (worker.getAssignments().workplace().filter(workshopIds::contains).flatMap(StructureSavedData.get(level)::get)
                    .filter(room -> WorkplaceService.isEligible(level, worker, room)).isPresent()) eligibleWorkers++;
            if (worker.getNavigation().isInProgress()) workingWorkers++;
            cane += WorkerInventory.count(worker, Items.SUGAR_CANE);
            paper += WorkerInventory.count(worker, Items.PAPER);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("workshops", workshops); report.put("lecterns", lecterns);
        report.put("valid_workshops", validWorkshops); report.put("linked_paper_workstations", linkedStations);
        report.put("background_workers", backgroundWorkers); report.put("assigned_workshop_workers", assignedWorkers);
        report.put("eligible_workshop_workers", eligibleWorkers);
        report.put("navigating_workers", workingWorkers);
        report.put("sugar_cane_remaining", cane); report.put("paper_current", paper);
        report.put("initial_sugar_cane", 8192); report.put("cane_plus_paper", cane + paper);
        report.put("setup_added_sugar_cane", setupAddedCane);
        report.put("setup_exported_paper", setupExportedPaper);
        report.put("total_input_output_conserved", cane + paper + setupExportedPaper == 8192 + setupAddedCane);
        report.put("configured_paper_stock_target", LibraryWorkSettings.current().paperStock());
        report.put("raw_gold_in_furnaces", rawGold); report.put("gold_ingots_in_furnaces", goldIngots);
        report.put("note", "Background cane is topped up and completed paper exported only during phase setup; both totals are recorded. Ordinary librarians perform all measured production. Furnace inputs are finite initial stock.");
        return report;
    }

    public MinecraftServer server() { return server; }
    public ServerLevel desert() { return desert; }
    public ServerLevel nether() { return nether; }
    public Settlement village() { return village; }
    public BlockPos center() { return center; }
    public int surfaceY() { return surfaceY; }
    public Map<String, Room> rooms() { return Collections.unmodifiableMap(rooms); }
    public List<UUID> workerIds() { return List.copyOf(workerIds); }
    public List<VillagerRealmsVillager> workers() {
        List<VillagerRealmsVillager> result = new ArrayList<>();
        for (UUID id : workerIds) for (ServerLevel level : List.of(desert, nether))
            if (level.getEntity(id) instanceof VillagerRealmsVillager worker) { result.add(worker); break; }
        return result;
    }
    public List<BlockPos> desertStairs() { return desertStairs; }
    public List<BlockPos> netherStairs() { return netherStairs; }
    public ItemStack supply(ItemStack stack) { return VillageGoods.creativeSupplied(supplyIdentity, stack); }

    public void put(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).equals(state)) return;
        require(level.setBlock(pos, state, 18) && level.getBlockState(pos).equals(state), "Rejected fixture block " + state + " at " + level.dimension().location() + " " + pos);
    }

    private void stock(ChestBlockEntity chest, net.minecraft.world.item.Item item, int amount) {
        for (int slot = 0; slot < chest.getContainerSize() && amount > 0; slot++) {
            int count = Math.min(64, amount); chest.setItem(slot, supply(new ItemStack(item, count))); amount -= count;
        }
        chest.setChanged();
    }

    private void own(Room room) { require(StructureOwnershipSavedData.get(room.level()).assign(room.level(), room.structure().id(), village.id()), "Ownership failed: " + room.name()); }
    private static ResourceKey<Level> dimension(String name) { return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("aoemod", name)); }
    private static Block block(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        require(ForgeRegistries.BLOCKS.containsKey(key), "Missing fixture block " + id);
        return ForgeRegistries.BLOCKS.getValue(key);
    }
    private BlockState material(ServerLevel level) { return (level == desert ? Blocks.SANDSTONE : Blocks.BLACKSTONE).defaultBlockState(); }

    private static Site findSite(ServerLevel level) {
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        Site best = null;
        for (int dx = 0; dx < 12; dx++) for (int dz = 0; dz < 12; dz++) {
            int x = 200008 + dx * 256, z = 200008 + dz * 256;
            List<Integer> heights = new ArrayList<>();
            for (int ox : new int[] {-108, -48, 16}) for (int oz : new int[] {-108, -48, 16}) {
                int height = generator.getBaseHeight(x + ox, z + oz, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                heights.add(height);
            }
            int minimum = Collections.min(heights), maximum = Collections.max(heights);
            if (minimum >= 64 && (best == null || maximum < best.maximumHeight()
                    || maximum == best.maximumHeight() && minimum > best.minimumHeight()))
                best = new Site(new BlockPos(x, 0, z), minimum, maximum, List.copyOf(heights));
        }
        require(best != null && best.maximumHeight() < level.getMaxBuildHeight() - 8,
                "No dry isolated Desert site found near200000");
        return best;
    }

    private void plaza(ServerLevel level, int y) {
        for (int x = -112; x <= 24; x++) for (int z = -112; z <= 48; z++) {
            if (level == desert) {
                int naturalTop = level.getHeight(Heightmap.Types.WORLD_SURFACE, center.getX() + x, center.getZ() + z);
                highestNaturalPlazaColumn = Math.max(highestNaturalPlazaColumn, naturalTop);
                for (int clearY = y; clearY < naturalTop; clearY++)
                    put(level, center.offset(x, clearY, z), Blocks.AIR.defaultBlockState());
            }
            // The positive quadrant is the helix, so leave it to the staircase builder.
            if (x >= -1 && z >= -1) continue;
            put(level, center.offset(x, y - 1, z), material(level));
            for (int dy = 0; dy <= 5; dy++) put(level, center.offset(x, y + dy, z), Blocks.AIR.defaultBlockState());
            if (level == nether) put(level, center.offset(x, y + 6, z), material(level));
        }
    }

    private static List<BlockPos> stair(BlockPos landing, int height, boolean topLanding) {
        int steps = height * 2 + 4;
        BlockPos reference = ring(topLanding ? steps : 0);
        int bottom = landing.getY() - (topLanding ? height : 0);
        List<BlockPos> path = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            BlockPos point = ring(i);
            path.add(new BlockPos(landing.getX() + point.getX() - reference.getX(), bottom + Math.max(0, (i - 4) / 2),
                    landing.getZ() + point.getZ() - reference.getZ()));
        }
        return List.copyOf(path);
    }

    private static BlockPos ring(int step) {
        int p = Math.floorMod(step, 64);
        return p < 16 ? new BlockPos(p, 0, 0) : p < 32 ? new BlockPos(16, 0, p - 16)
                : p < 48 ? new BlockPos(48 - p, 0, 16) : new BlockPos(0, 0, 64 - p);
    }

    private void buildStair(ServerLevel level, List<BlockPos> path) {
        Set<BlockPos> floors = new HashSet<>(), air = new HashSet<>();
        for (BlockPos feet : path) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = -2; y <= 4; y++)
                if (!level.isOutsideBuildHeight(feet.offset(x, y, z))) put(level, feet.offset(x, y, z), material(level));
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                floors.add(feet.offset(x, -1, z));
                for (int y = 0; y < 4; y++) air.add(feet.offset(x, y, z));
            }
        }
        for (BlockPos pos : floors) put(level, pos, material(level));
        for (BlockPos pos : air) if (!floors.contains(pos) && !level.isOutsideBuildHeight(pos)) put(level, pos, Blocks.AIR.defaultBlockState());
    }

    private static void validateStair(ServerLevel level, List<BlockPos> path) {
        BlockPos previous = null;
        for (BlockPos nominal : path) {
            BlockPos standing = null;
            // The three-wide tread intentionally joins the higher neighboring tread at a corner.
            for (int dy = 0; dy <= 1; dy++) if (StackedVillageTravel.safe(level, nominal.above(dy))) {
                standing = nominal.above(dy); break;
            }
            require(standing != null, "Blocked physical benchmark staircase at " + level.dimension().location() + " " + nominal);
            require(previous == null || Math.abs(standing.getY() - previous.getY()) <= 1,
                    "Unwalkable benchmark stair rise at " + standing);
            previous = standing;
        }
    }

    private void connect(ServerLevel level, BlockPos from, BlockPos to) {
        require(from.getY() == to.getY(), "Connector must be level");
        BlockPos cursor = from;
        while (!cursor.equals(to)) {
            landing(level, cursor);
            cursor = cursor.getX() != to.getX() ? cursor.offset(Integer.signum(to.getX() - cursor.getX()), 0, 0)
                    : cursor.offset(0, 0, Integer.signum(to.getZ() - cursor.getZ()));
        }
        landing(level, to);
    }

    private void connectSurfaceEnd(BlockPos end) {
        int x = end.getX() - center.getX(), z = end.getZ() - center.getZ();
        BlockPos outside;
        if (z == 0) outside = center.offset(x, surfaceY, -4);
        else if (x == 16) outside = center.offset(20, surfaceY, z);
        else if (z == 16) outside = center.offset(x, surfaceY, 20);
        else outside = center.offset(-4, surfaceY, z);
        // Exit perpendicular to the last tread. Crossing backward over the rising helix would cap its headroom.
        connect(desert, end, outside);
        if (z == 0) {
            BlockPos northeast = center.offset(20, surfaceY, -4);
            connect(desert, outside, northeast); outside = northeast;
        }
        BlockPos southern = new BlockPos(outside.getX(), surfaceY, center.getZ() + 20);
        connect(desert, outside, southern);
        connect(desert, southern, center.offset(-12, surfaceY, 20));
    }

    private void landing(ServerLevel level, BlockPos feet) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            put(level, feet.offset(x, -1, z), material(level));
            for (int y = 0; y < 3; y++) put(level, feet.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }

    private static void require(boolean test, String message) { if (!test) throw new IllegalStateException(message); }
}
