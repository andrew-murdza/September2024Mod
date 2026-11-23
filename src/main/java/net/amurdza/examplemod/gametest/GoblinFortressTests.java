package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressLayout;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressBlocks;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPlacement;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressSpawns;
import net.amurdza.examplemod.worldgen.structure.MansionResidentsPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoblinFortressTests {
    private GoblinFortressTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void authoredWriteScopeNeverLeaksToLaterOrdinaryPlacement(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var soil = new BlockPos(3000, 180, 149);
        var crop = soil.above();
        var soilBefore = level.getBlockState(soil); var cropBefore = level.getBlockState(crop);
        var mycelium = Blocks.MYCELIUM.defaultBlockState();
        var beets = Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3);
        helper.assertFalse(level.getBiome(crop).is(new ResourceLocation("aoemod:mushroom_caves")),
                "The regression must exercise ordinary Snowy terrain outside natural mushroom generation");
        try {
            level.setBlock(soil, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
            helper.assertTrue(level.setBlock(soil, mycelium, 2),
                    "Ordinary mycelium placement must use Snowy's shared block list");
            level.setBlock(soil, Blocks.AIR.defaultBlockState(), 2);
            helper.assertTrue(GoblinFortressBlocks.set(level, soil, mycelium) && level.getBlockState(soil).is(Blocks.MYCELIUM),
                    "An explicitly authored mushroom room must retain its actual mycelium floor");
            helper.assertFalse(GoblinFortressBlocks.isAuthored(level, soil, mycelium), "Successful writing must close its placement exception");
            GoblinFortressBlocks.set(level, soil, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
            helper.assertTrue(GoblinFortressBlocks.set(level, crop, beets) && level.getBlockState(crop).equals(beets),
                    "An explicitly authored dappled farm must retain mature beetroot");
            helper.assertFalse(GoblinFortressBlocks.isAuthored(level, crop, beets), "A crop write must not leave an ambient biome exception");
            level.setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
            helper.assertTrue(level.setBlock(crop, beets, 2), "Later ordinary beet planting uses Snowy's shared block list");
            helper.assertTrue(level.setBlock(soil, mycelium, 2), "Later ordinary mycelium placement is independently available throughout Snowy");
            helper.assertFalse(GoblinFortressBlocks.isAuthored(level, soil, mycelium),
                    "Shared availability must not leave an authored-write exception active");
        } finally {
            GoblinFortressBlocks.set(level, crop, cropBefore);
            GoblinFortressBlocks.set(level, soil, soilBefore);
        }

        var probePos = new BlockPos(1, 2, 3);
        var probeState = Blocks.STONE_BRICKS.defaultBlockState();
        var failure = new IllegalStateException("Authored write failure fixture");
        boolean[] throwOnWrite = {false};
        var proxy = (WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(GoblinFortressTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (world, method, args) -> {
                    if (!method.getName().equals("setBlock")) throw new UnsupportedOperationException(method.getName());
                    helper.assertTrue(GoblinFortressBlocks.isAuthored((WorldGenLevel) world, probePos, probeState),
                            "Only the exact active write may bypass the material rule");
                    helper.assertFalse(GoblinFortressBlocks.isAuthored((WorldGenLevel) world, probePos.east(), probeState)
                                    || GoblinFortressBlocks.isAuthored((WorldGenLevel) world, probePos, Blocks.DIRT.defaultBlockState())
                                    || GoblinFortressBlocks.isAuthored(level, probePos, probeState),
                            "Neighbor positions, other states, and other worlds must never inherit the scope");
                    if (throwOnWrite[0]) throw failure;
                    return false;
                });
        helper.assertFalse(GoblinFortressBlocks.set(proxy, probePos, probeState), "A rejected underlying write remains a failed write");
        helper.assertFalse(GoblinFortressBlocks.isAuthored(proxy, probePos, probeState), "A false return must close the scope");
        throwOnWrite[0] = true;
        boolean threw = false;
        try { GoblinFortressBlocks.set(proxy, probePos, probeState); }
        catch (IllegalStateException actual) { threw = actual == failure; }
        helper.assertTrue(threw && !GoblinFortressBlocks.isAuthored(proxy, probePos, probeState),
                "Exceptions must propagate while closing the authored-write scope");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void allRoomsAndPensConnectToOneGateAndTheFinalBoss(GameTestHelper helper) throws Exception {
        Function<BlockPos, BlockState> states = pos -> GoblinFortressLayout.stateAt(pos.getX(), pos.getY(), pos.getZ());
        var walking = walk(states);
        var unreachable = unreachable(walking);
        var report = Map.of("floorArea", GoblinFortressLayout.floorArea(), "walkingCells", walking.size(),
                "rooms", GoblinFortressLayout.rooms().size(), "pens", GoblinFortressLayout.pens().size(),
                "unreachable", unreachable, "bossReachable", walking.contains(GoblinFortressLayout.boss()));
        write("goblin-fortress-layout-report.json", report);
        helper.assertTrue(GoblinFortressLayout.floorArea() > 35000, "The fortress must provide a large inhabited floor area");
        helper.assertTrue(GoblinFortressLayout.rooms().size() >= 30 && GoblinFortressLayout.FLOORS.length == 5,
                "The mining society needs five distinct inhabited levels");
        helper.assertTrue(unreachable.isEmpty(), "Unreachable fortress rooms or pens: " + unreachable);
        helper.assertTrue(walking.contains(GoblinFortressLayout.boss()), "The single entrance must lead to the final Alpha Yeti chamber");
        for (int floor : GoblinFortressLayout.FLOORS) {
            for (int z = -14; z <= 65; z++) for (int x : new int[]{-74, 74})
                helper.assertFalse(open(GoblinFortressLayout.stateAt(x, floor + 1, z)), "Unexpected side entrance at " + x + "," + floor + "," + z);
            for (int x = -74; x <= 74; x++) {
                boolean tunnel = floor == 0 && Math.abs(x) <= 5;
                helper.assertTrue(open(GoblinFortressLayout.stateAt(x, floor + 1, 65)) == tunnel,
                        "The rear wall must contain exactly one attached yeti passage");
            }
        }
        for (int x = -57; x <= 57; x++)
            helper.assertTrue(open(GoblinFortressLayout.stateAt(x, 1, -53)) == (Math.abs(x) <= 5),
                    "The exterior curtain wall must retain one central gate");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void miningTracksAndRoomThemesKeepTheirAuthoredPurpose(GameTestHelper helper) {
        var mines = Set.of("miners_hall", "iron_mine", "copper_mine", "ore_sorting", "ore_vault",
                "crystal_gallery", "deep_mine");
        for (var room : GoblinFortressLayout.rooms()) {
            if (!mines.contains(room.name())) continue;
            var lanes = new java.util.TreeMap<Integer, Set<Integer>>();
            for (int x = room.minX(); x <= room.maxX(); x++) for (int z = room.minZ(); z <= room.maxZ(); z++) {
                if (GoblinFortressLayout.stateAt(x, room.floor() + 1, z).is(Blocks.RAIL))
                    lanes.computeIfAbsent(x, unused -> new HashSet<>()).add(z);
            }
            helper.assertTrue(lanes.size() == 2, room.name() + " must contain two distinct minecart tracks");
            for (var lane : lanes.entrySet()) for (int z = room.minZ() + 4; z <= room.maxZ() - 4; z++) {
                helper.assertTrue(lane.getValue().contains(z), room.name() + " track is interrupted at " + lane.getKey() + "," + z);
                helper.assertTrue(GoblinFortressLayout.stateAt(lane.getKey(), room.floor() + 2, z).isAir()
                                && GoblinFortressLayout.stateAt(lane.getKey(), room.floor() + 3, z).isAir(),
                        room.name() + " track needs clear minecart/passenger headroom");
            }
        }
        var ice = GoblinFortressLayout.rooms().stream().filter(room -> room.name().equals("ice_store")).findFirst().orElseThrow();
        for (int x : new int[]{ice.minX() + 1, ice.maxX() - 1}) for (int z = ice.minZ() + 4; z <= ice.maxZ() - 4; z++)
            helper.assertTrue(GoblinFortressLayout.stateAt(x, ice.floor() + 1, z).is(Blocks.BLUE_ICE),
                    "The ice store needs ice shelves, not ore caused by the 'ore' substring in 'store'");
        var cold = GoblinFortressLayout.rooms().stream().filter(room -> room.name().equals("cold_store")).findFirst().orElseThrow();
        helper.assertTrue(GoblinFortressLayout.stateAt(cold.minX() + 1, cold.floor() + 1, cold.minZ() + 4).is(Blocks.BARREL),
                "The cold store must contain storage barrels instead of a mining wall");
        var dining = GoblinFortressLayout.rooms().stream().filter(room -> room.name().equals("miners_refectory")).findFirst().orElseThrow();
        helper.assertTrue(GoblinFortressLayout.stateAt((dining.minX() + dining.maxX()) / 2,
                        dining.floor() + 1, (dining.minZ() + dining.maxZ()) / 2).is(Blocks.SMOOTH_STONE_SLAB),
                "The miners' refectory must contain its dining table instead of being classified as a mine");
        for (var room : List.of(ice, cold, dining)) for (int x = room.minX(); x <= room.maxX(); x++)
            for (int z = room.minZ(); z <= room.maxZ(); z++)
                helper.assertFalse(GoblinFortressLayout.stateAt(x, room.floor() + 1, z).is(Blocks.RAIL),
                        room.name() + " must not inherit minecart tracks from a partial name match");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rotationsAndSerializationKeepTheFullNativeRoster(GameTestHelper helper) {
        var context = StructurePieceSerializationContext.fromLevel(helper.getLevel());
        helper.assertTrue(GoblinFortressPiece.DEFENDER_COUNT > MansionResidentsPiece.DEFENDERS,
                "The fortress population must exceed the expanded mansion");
        for (var direction : Direction.Plane.HORIZONTAL) {
            var piece = new GoblinFortressPiece(new BlockPos(-321, 78, 513), direction, 417);
            var restored = new GoblinFortressPiece(context, piece.createTag(context));
            helper.assertTrue(restored.encounterId().equals(piece.encounterId()) && restored.origin().equals(piece.origin())
                            && restored.inward() == direction && restored.getBoundingBox().equals(piece.getBoundingBox()),
                    "Saved fortress identity and rotated bounds must survive reload");
            var residents = restored.residents();
            var defenders = residents.stream().filter(plan -> !plan.livestock()).toList();
            var counts = defenders.stream().collect(Collectors.groupingBy(GoblinFortressSpawns.Plan::type, Collectors.counting()));
            helper.assertTrue(defenders.size() == GoblinFortressPiece.DEFENDER_COUNT && counts.getOrDefault("twilightforest:alpha_yeti", 0L) == 1,
                    "The roster needs exactly 192 regular defenders and one native Alpha Yeti");
            for (String type : GoblinFortressPiece.HOSTILES) helper.assertTrue(counts.getOrDefault(type, 0L) == 32,
                    "All six Snowy hostile families need 32 authored representatives: " + type);
            helper.assertTrue(residents.stream().map(GoblinFortressSpawns::entityId).distinct().count() == residents.size(),
                    "Every defender and animal needs its own stable entity identity");
            helper.assertTrue(defenders.stream().map(GoblinFortressSpawns.Plan::preferred).distinct().count() == defenders.size(),
                    "Defenders must not share authored spawn positions");
            helper.assertTrue(residents.stream().allMatch(plan -> ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(plan.type()))
                            && piece.getBoundingBox().isInside(plan.preferred()) && plan.room().isInside(plan.preferred())),
                    "Each saved native species and spawn cell must exist and remain within its protected room");
            var animals = residents.stream().filter(GoblinFortressSpawns.Plan::livestock).map(GoblinFortressSpawns.Plan::type).collect(Collectors.toSet());
            helper.assertTrue(animals.containsAll(Set.of("aoemod:yak", "minecraft:fox", "minecraft:rabbit", "minecraft:goat",
                            "biodiverse:reindeer", "minecraft:polar_bear", "minecraft:mooshroom", "minecraft:salmon")),
                    "The separate dappled, snowy, mushroom, and fishing habitats must all retain their animals");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void naturalFortressPlacesConnectedHabitatsAndEveryResident(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        helper.assertTrue(level != null, "The Snowy dimension must be present");
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, level.getServer());
        var random = level.getChunkSource().randomState();
        ChunkPos owner = null;
        int firstStation = Integer.getInteger("aoemod.goblinFortressTestStation", 3);
        for (int station = firstStation; station < firstStation + 8; station++) {
            var candidate = GoblinFortressPlacement.candidate(random, 0, station);
            if (GoblinFortressPlacement.site(random, candidate) != null) { owner = candidate; break; }
        }
        helper.assertTrue(owner != null, "A nearby station must fit both the dappled farms and snowy pens");
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:yeti_cave"));
        var start = level.getChunk(owner.x, owner.z).getAllStarts().get(structure);
        helper.assertTrue(start != null && start.isValid(), "The configured structure set must naturally generate at its candidate");
        var piece = start.getPieces().stream().filter(GoblinFortressPiece.class::isInstance)
                .map(GoblinFortressPiece.class::cast).findFirst().orElseThrow();
        var bounds = piece.getBoundingBox();
        var forced = new ArrayList<ChunkPos>();
        try {
            for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++)
                for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                    var chunk = new ChunkPos(x, z);
                    if (!level.getForcedChunks().contains(chunk.toLong())) {
                        level.setChunkForced(x, z, true);
                        forced.add(chunk);
                    }
                    level.getChunk(x, z);
                }
        } catch (RuntimeException | Error failure) {
            release(level, forced);
            throw failure;
        }
        helper.runAfterDelay(5, () -> awaitNaturalChunks(helper, level, piece, forced, 5));
    }

    private static List<Map<String, Object>> unreadyChunks(ServerLevel level, GoblinFortressPiece piece) {
        var result = new ArrayList<Map<String, Object>>();
        var bounds = piece.getBoundingBox();
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++)
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                var chunk = new ChunkPos(x, z);
                boolean loaded = level.areEntitiesLoaded(chunk.toLong());
                boolean ticking = level.isPositionEntityTicking(chunk.getMiddleBlockPosition(piece.origin().getY()));
                if (!loaded || !ticking) result.add(Map.of("chunk", chunk.toString(), "entitiesLoaded", loaded, "entityTicking", ticking));
            }
        return result;
    }

    private static void awaitNaturalChunks(GameTestHelper helper, ServerLevel level, GoblinFortressPiece piece,
            List<ChunkPos> forced, int waited) {
        for (var plan : piece.residents())
            if (level.getEntity(GoblinFortressSpawns.entityId(plan)) instanceof Mob mob) mob.setNoAi(true);
        var unready = unreadyChunks(level, piece);
        if (!unready.isEmpty() && waited < 1200) {
            helper.runAfterDelay(10, () -> awaitNaturalChunks(helper, level, piece, forced, waited + 10));
            return;
        }
        try {
            write("goblin-fortress-chunk-readiness.json", Map.of("waitedTicks", waited, "unready", unready));
            helper.assertTrue(unready.isEmpty(), "Every fortress entity section must become loaded and ticking: " + unready);
            for (var plan : piece.residents()) {
                GoblinFortressSpawns.retry(level, plan);
                if (level.getEntity(GoblinFortressSpawns.entityId(plan)) instanceof Mob mob) mob.setNoAi(true);
            }
            // Insertion and section visibility settle on server ticks, independently of block chunk loading.
            helper.runAfterDelay(2, () -> {
                try {
                    verifyNatural(helper, level, piece);
                    helper.succeed();
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                } finally {
                    release(level, forced);
                }
            });
        } catch (Exception | Error failure) {
            release(level, forced);
            throw new RuntimeException(failure);
        }
    }

    private static void verifyNatural(GameTestHelper helper, ServerLevel level, GoblinFortressPiece piece) throws Exception {
        var plans = piece.residents();
        var residents = level.getEntitiesOfClass(Mob.class, AABB.of(piece.getBoundingBox()).inflate(2),
                mob -> piece.encounterId().equals(AuthoredEncounterProgress.encounterId(mob)));
        var counts = residents.stream().collect(Collectors.groupingBy(mob -> ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString(), Collectors.counting()));
        var expected = plans.stream().collect(Collectors.groupingBy(GoblinFortressSpawns.Plan::type, Collectors.counting()));
        var pending = GoblinFortressSpawns.get(level).plans().stream().filter(plan -> plan.encounter().equals(piece.encounterId()))
                .map(plan -> Map.of("index", plan.index(), "type", plan.type(), "position", plan.preferred().toShortString())).toList();
        var walking = walk(local -> level.getBlockState(piece.world(local)));
        var unreachable = unreachable(walking);
        var incorrectBiomes = new ArrayList<String>();
        for (int x = -70; x <= -12; x += 2) for (int z = -48; z <= -17; z += 2)
            checkBiome(level, piece.world(x, 1, z), "dappled_forest", incorrectBiomes);
        for (var pen : GoblinFortressLayout.pens()) {
            if (pen.name().equals("mooshrooms")) continue;
            String biome = pen.name().startsWith("snowy") ? "snowy_forest" : "dappled_forest";
            for (int x = pen.minX(); x <= pen.maxX(); x += 2) for (int z = pen.minZ(); z <= pen.maxZ(); z += 2)
                checkBiome(level, piece.world(x, pen.floor() + 1, z), biome, incorrectBiomes);
        }
        var missingBlocks = new ArrayList<String>();
        for (var room : GoblinFortressLayout.rooms()) for (var local : GoblinFortressLayout.spawnPositions(room)) {
            if (!open(level.getBlockState(piece.world(local)))
                    || !open(level.getBlockState(piece.world(local.above())))) missingBlocks.add(room.name() + "@" + local.toShortString());
            var expectedSupport = GoblinFortressLayout.stateAt(local.getX(), local.getY() - 1, local.getZ());
            var support = level.getBlockState(piece.world(local.below()));
            if (expectedSupport != null && !support.is(expectedSupport.getBlock()))
                missingBlocks.add(room.name() + " floor@" + local.below().toShortString() + ": expected=" + expectedSupport + ", actual=" + support);
        }
        for (var local : GoblinFortressLayout.lootPositions())
            if (!level.getBlockState(piece.world(local)).is(Blocks.CHEST)) missingBlocks.add("chest@" + local.toShortString());
        var preservedContent = new java.util.TreeMap<String, Integer>();
        var missingContent = new ArrayList<String>();
        for (int x = -70; x <= -12; x++) for (int z = -48; z <= -17; z++) for (int y = 0; y <= 1; y++) {
            var authored = GoblinFortressLayout.stateAt(x, y, z);
            if (authored == null || !(authored.is(Blocks.FARMLAND) || authored.is(Blocks.BEETROOTS)
                    || authored.is(Blocks.WHEAT) || authored.is(Blocks.PUMPKIN) || authored.is(Blocks.ATTACHED_PUMPKIN_STEM))) continue;
            verifyAuthoredContent(level, piece, new BlockPos(x, y, z), authored, preservedContent, missingContent);
        }
        var fixtures = Set.of(Blocks.MYCELIUM, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK,
                Blocks.BROWN_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM, Blocks.ENCHANTING_TABLE, Blocks.BOOKSHELF,
                Blocks.ANVIL, Blocks.BLAST_FURNACE, Blocks.SMITHING_TABLE, Blocks.LOOM, Blocks.LECTERN,
                Blocks.BREWING_STAND, Blocks.GRINDSTONE, Blocks.RAIL, Blocks.COPPER_ORE, Blocks.DEEPSLATE_IRON_ORE);
        for (var room : GoblinFortressLayout.rooms()) for (int x = room.minX(); x <= room.maxX(); x++)
            for (int z = room.minZ(); z <= room.maxZ(); z++) for (int y = room.floor(); y <= room.floor() + 8; y++) {
                var authored = GoblinFortressLayout.stateAt(x, y, z);
                if (authored != null && fixtures.contains(authored.getBlock()))
                    verifyAuthoredContent(level, piece, new BlockPos(x, y, z), authored, preservedContent, missingContent);
            }
        int indoorSnowCount = 0;
        var indoorSnowByRoom = new java.util.TreeMap<String, Integer>();
        var indoorSnowSamples = new ArrayList<String>();
        var mushroomRooms = Set.of("mushroom_nursery", "mushroom_garden", "mushroom_hall", "mooshrooms");
        for (var room : GoblinFortressLayout.rooms()) {
            if (!mushroomRooms.contains(room.name())) continue;
            int roomSnow = 0;
            for (int x = room.minX(); x <= room.maxX(); x++) for (int z = room.minZ(); z <= room.maxZ(); z++)
                for (int y = room.floor() + 1; y <= room.floor() + 10; y++) {
                    var local = new BlockPos(x, y, z);
                    var state = level.getBlockState(piece.world(local));
                    var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (state.is(Blocks.SNOW) || state.is(snownee.snow.CoreModule.TILE_BLOCK.get())
                            || id != null && id.getNamespace().equals("snowrealmagic")) {
                        roomSnow++;
                        if (indoorSnowSamples.size() < 20) indoorSnowSamples.add(room.name() + "@" + local.toShortString() + "=" + state);
                    }
                }
            indoorSnowByRoom.put(room.name(), roomSnow);
            indoorSnowCount += roomSnow;
        }
        var queried = residents.stream().map(Mob::getUUID).collect(Collectors.toSet());
        var progress = AuthoredEncounterProgress.get(level);
        var ledger = progress.save(new CompoundTag()).getList("Encounters", net.minecraft.nbt.Tag.TAG_COMPOUND).stream()
                .map(CompoundTag.class::cast).filter(tag -> tag.getUUID("Id").equals(piece.encounterId())).findFirst().orElseThrow();
        var observed = new HashSet<Integer>();
        for (var value : ledger.getList("Defenders", net.minecraft.nbt.Tag.TAG_COMPOUND)) observed.add(((CompoundTag) value).getInt("Index"));
        var planDiagnostics = new ArrayList<Map<String, Object>>();
        for (var plan : plans) {
            var entity = level.getEntity(GoblinFortressSpawns.entityId(plan));
            var entry = new java.util.LinkedHashMap<String, Object>();
            entry.put("index", plan.index()); entry.put("type", plan.type()); entry.put("uuid", GoblinFortressSpawns.entityId(plan).toString());
            entry.put("preferred", plan.preferred().toShortString()); entry.put("livestock", plan.livestock());
            entry.put("preferredBlock", level.getBlockState(plan.preferred()).toString());
            entry.put("preferredSupport", level.getBlockState(plan.preferred().below()).toString());
            entry.put("preferredHead", level.getBlockState(plan.preferred().above()).toString());
            entry.put("ledgerObserved", observed.contains(plan.index()));
            entry.put("canPlaceDefender", !plan.livestock() && progress.canPlaceDefender(piece.encounterId(), plan.index()));
            entry.put("loadedByUuid", entity != null); entry.put("inAreaQuery", queried.contains(GoblinFortressSpawns.entityId(plan)));
            entry.put("chunkEntitiesLoaded", level.areEntitiesLoaded(new ChunkPos(plan.preferred()).toLong()));
            entry.put("chunkEntityTicking", level.isPositionEntityTicking(plan.preferred()));
            if (entity != null) { entry.put("actual", entity.position().toString()); entry.put("removed", entity.isRemoved()); }
            planDiagnostics.add(entry);
        }
        var stairDifferences = new ArrayList<Map<String, Object>>();
        for (int x = -8; x <= 8; x++) for (int z = -10; z <= 4; z++) for (int y = -1; y <= 17; y++) {
            var expectedState = GoblinFortressLayout.stateAt(x, y, z);
            var actual = level.getBlockState(piece.world(x, y, z));
            if (expectedState != null && !expectedState.equals(actual)) stairDifferences.add(Map.of(
                    "local", new BlockPos(x, y, z).toShortString(), "expected", expectedState.toString(), "actual", actual.toString()));
        }
        var report = new java.util.LinkedHashMap<String, Object>();
        report.put("origin", piece.origin().toShortString()); report.put("inward", piece.inward().getName());
        report.put("counts", counts); report.put("expected", expected); report.put("pending", pending);
        report.put("walkingCells", walking.size()); report.put("unreachable", unreachable); report.put("incorrectBiomes", incorrectBiomes);
        report.put("missingBlocks", missingBlocks); report.put("ledgerObservedDefenders", observed.size());
        report.put("defeatedDefenders", progress.defeatedCount(piece.encounterId())); report.put("planDiagnostics", planDiagnostics);
        report.put("unreadyChunks", unreadyChunks(level, piece)); report.put("stairDifferences", stairDifferences);
        report.put("preservedContent", preservedContent); report.put("missingContent", missingContent);
        report.put("indoorSnowCount", indoorSnowCount); report.put("indoorSnowByRoom", indoorSnowByRoom);
        report.put("indoorSnowSamples", indoorSnowSamples);
        write("goblin-fortress-report.json", report);
        writeViews(level, piece);
        helper.assertTrue(counts.equals(expected) && pending.isEmpty(), "Every real fortress defender and animal must insert exactly once: " + counts + "; pending=" + pending);
        helper.assertTrue(residents.stream().filter(AuthoredEncounterProgress::isDefender).count() == GoblinFortressPiece.DEFENDER_COUNT,
                "All 193 fixed defenders must exist as tracked entities");
        helper.assertTrue(residents.stream().allMatch(mob -> level.noCollision(mob)), "No authored resident may spawn in a wall or another mob");
        helper.assertTrue(incorrectBiomes.isEmpty(), "Authored farming and livestock must match their actual biomes: " + incorrectBiomes);
        helper.assertTrue(missingBlocks.isEmpty(), "Natural terrain decoration must preserve every room and chest: " + missingBlocks);
        helper.assertTrue(missingContent.isEmpty(), "Authored crops, mushroom gardens, and furnished workplaces must survive placement: " + missingContent);
        helper.assertTrue(indoorSnowCount == 0 && indoorSnowByRoom.size() == 4,
                "The four roofed mushroom rooms must remain free of natural snow and snowlogged plants: " + indoorSnowByRoom + "; " + indoorSnowSamples);
        helper.assertTrue(preservedContent.getOrDefault("minecraft:beetroots", 0) > 100
                        && preservedContent.getOrDefault("minecraft:mycelium", 0) > 100
                        && preservedContent.getOrDefault("minecraft:mushroom_stem", 0) > 0
                        && preservedContent.getOrDefault("minecraft:red_mushroom_block", 0) > 0
                        && preservedContent.getOrDefault("minecraft:brown_mushroom_block", 0) > 0
                        && preservedContent.getOrDefault("minecraft:enchanting_table", 0) > 0
                        && preservedContent.getOrDefault("minecraft:blast_furnace", 0) > 0,
                "The live fortress must contain the requested mature farm, giant mushrooms, enchanting, and forging equipment");
        helper.assertTrue(unreachable.isEmpty() && walking.contains(GoblinFortressLayout.boss()), "Actual block placement disconnected rooms: " + unreachable);
        var boundary = BossStructureBounds.get(level).nearby(new AABB(piece.world(0, 1, -90)));
        helper.assertTrue(boundary.stream().anyMatch(value -> piece.encounterId().equals(value.encounter())),
                "Fishing grounds outside the curtain wall must remain within the fortress protection boundary");
        for (var plan : plans) helper.assertFalse(GoblinFortressSpawns.retry(level, plan), "Revisiting generated chunks must never duplicate their residents");
        level.getServer().saveEverything(true, true, true);
    }

    private static void checkBiome(ServerLevel level, BlockPos pos, String name, List<String> failures) {
        if (!level.getBiome(pos).is(new ResourceLocation("aoemod", name)) && failures.size() < 30)
            failures.add(name + "@" + pos.toShortString() + "=" + level.getBiome(pos).unwrapKey().orElseThrow().location());
    }

    private static void verifyAuthoredContent(ServerLevel level, GoblinFortressPiece piece, BlockPos local, BlockState expected,
            Map<String, Integer> preserved, List<String> missing) {
        var actual = level.getBlockState(piece.world(local));
        boolean matches = actual.is(expected.getBlock());
        if (matches && expected.is(Blocks.BEETROOTS)) matches = actual.getValue(BlockStateProperties.AGE_3) == 3;
        if (matches && expected.is(Blocks.WHEAT)) matches = actual.getValue(BlockStateProperties.AGE_7) == 7;
        if (matches && expected.is(Blocks.FARMLAND)) matches = actual.getValue(BlockStateProperties.MOISTURE) == 7;
        String id = ForgeRegistries.BLOCKS.getKey(expected.getBlock()).toString();
        if (matches) preserved.merge(id, 1, Integer::sum);
        else if (missing.size() < 60) missing.add(id + "@" + local.toShortString() + ": " + actual);
    }

    private static void release(ServerLevel level, List<ChunkPos> chunks) {
        for (var chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
    }

    private static boolean open(BlockState state) {
        return state != null && (state.isAir() || state.getBlock() instanceof StairBlock
                || state.getBlock() instanceof FenceGateBlock || state.getBlock() instanceof BaseRailBlock
                || state.is(Blocks.SNOW) && state.getValue(SnowLayerBlock.LAYERS) <= 2);
    }

    private static Set<BlockPos> walk(Function<BlockPos, BlockState> states) {
        var visited = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        var cache = new HashMap<BlockPos, Boolean>();
        var start = GoblinFortressLayout.entrance();
        queue.add(start); visited.add(start);
        while (!queue.isEmpty()) {
            var from = queue.removeFirst();
            for (var direction : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                var to = from.relative(direction).above(dy);
                if (to.getX() < GoblinFortressLayout.MIN_X || to.getX() > GoblinFortressLayout.MAX_X
                        || to.getZ() < GoblinFortressLayout.MIN_Z || to.getZ() > GoblinFortressLayout.MAX_Z
                        || to.getY() <= GoblinFortressLayout.MIN_Y || to.getY() >= GoblinFortressLayout.MAX_Y - 1
                        || visited.contains(to)) continue;
                boolean possible = cache.computeIfAbsent(to, pos -> {
                    var feet = states.apply(pos); var head = states.apply(pos.above()); var floor = states.apply(pos.below());
                    return open(feet) && open(head) && floor != null && !floor.isAir() && floor.getFluidState().isEmpty();
                });
                if (!possible) continue;
                var clearance = dy > 0 ? from.above(2) : dy < 0 ? to.above(2) : null;
                if (clearance != null && !open(states.apply(clearance))) continue;
                visited.add(to); queue.addLast(to);
            }
        }
        return visited;
    }

    private static List<String> unreachable(Set<BlockPos> walking) {
        var result = new ArrayList<String>();
        var rooms = new ArrayList<>(GoblinFortressLayout.rooms()); rooms.addAll(GoblinFortressLayout.pens());
        for (var room : rooms) if (walking.stream().noneMatch(pos -> pos.getX() > room.minX() && pos.getX() < room.maxX()
                && pos.getZ() > room.minZ() && pos.getZ() < room.maxZ() && pos.getY() == room.floor() + 1)) result.add(room.name());
        return result;
    }

    private static void write(String name, Object value) throws Exception {
        Files.writeString(Path.of(name), new GsonBuilder().setPrettyPrinting().create().toJson(value) + "\n");
    }

    private static void writeViews(ServerLevel level, GoblinFortressPiece piece) throws Exception {
        var views = new ArrayList<Map<String, Object>>();
        view(views, level, piece, "goblin-fortress-overview", new BlockPos(0, 38, -55), new BlockPos(0, 8, 0));
        view(views, level, piece, "goblin-fortress-gate", new BlockPos(0, 4, -84), new BlockPos(0, 5, -39));
        view(views, level, piece, "goblin-fortress-farms", new BlockPos(-7, 15, -52), new BlockPos(-43, 1, -33));
        view(views, level, piece, "goblin-fortress-fishery", new BlockPos(0, 13, -93), new BlockPos(-21, -1, -75));
        view(views, level, piece, "goblin-fortress-poplar-yard", new BlockPos(6, 8, -50), new BlockPos(27, 5, -29));
        view(views, level, piece, "goblin-fortress-snowy-pens", new BlockPos(0, 22, -2), new BlockPos(-34, 13, 23));
        view(views, level, piece, "goblin-fortress-forge", new BlockPos(-17, 3, 18), new BlockPos(-49, 3, 28));
        view(views, level, piece, "goblin-fortress-enchanting", new BlockPos(17, 3, 18), new BlockPos(45, 3, 28));
        view(views, level, piece, "goblin-fortress-stairs", new BlockPos(0, 3, -11), new BlockPos(5, -5, -3));
        view(views, level, piece, "goblin-fortress-mushrooms", new BlockPos(-16, -21, -7), new BlockPos(-45, -20, 0));
        view(views, level, piece, "goblin-fortress-mooshrooms", new BlockPos(16, -21, 18), new BlockPos(44, -21, 28));
        view(views, level, piece, "goblin-fortress-deep-mine", new BlockPos(-17, -45, 47), new BlockPos(-50, -45, 54));
        view(views, level, piece, "goblin-fortress-yeti-passage", new BlockPos(0, 3, 40), new BlockPos(0, 4, 68));
        view(views, level, piece, "goblin-fortress-alpha-yeti", new BlockPos(-8, 4, 66), new BlockPos(0, 4, 77));
        view(views, level, piece, "goblin-fortress-facade", new BlockPos(0, 4, -38), new BlockPos(0, 6, -14));
        view(views, level, piece, "goblin-fortress-dappled-pens", new BlockPos(40, 12, -38), new BlockPos(58, 1, -32));
        write("goblin-fortress-views.json", views);
    }

    private static void view(List<Map<String, Object>> views, ServerLevel level, GoblinFortressPiece piece,
            String name, BlockPos localCamera, BlockPos localTarget) {
        var feet = piece.world(localCamera); var target = piece.world(localTarget);
        var eye = Vec3.atBottomCenterOf(feet).add(0, 1.62, 0);
        var delta = Vec3.atCenterOf(target).subtract(eye);
        views.add(Map.of("label", name, "dimension", level.dimension().location().toString(),
                "x", eye.x, "y", (double) feet.getY(), "z", eye.z,
                "yaw", (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
                "pitch", (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()))));
    }
}
