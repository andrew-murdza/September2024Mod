package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.world.components.structures.YetiCaveComponent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real block placement, walking connectivity, saved placement flags and reviewable scene exports. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossStructureExpansionTests {
    private static final Path REPORTS = Path.of("build", "reports");
    private static final List<Map<String, Object>> VIEWS = new ArrayList<>();
    private static final List<Map<String, Object>> METRICS = new ArrayList<>();
    private BossStructureExpansionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void expandedBossLayoutsHaveWalkableBranchesAndPersistedResidents(GameTestHelper helper) throws Exception {
        for (String kind : List.of("spider", "wither")) {
            var dimension = kind.equals("spider") ? GameplayDimensions.TROPICAL : GameplayDimensions.NETHER;
            ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
            BlockPos center = new BlockPos(12288, level.getMaxBuildHeight() - 48, 12288);
            var piece = new BossLairPiece(kind, center, 40);
            // Keep this historical five-pocket/7-resident regression on old saved geometry.
            // SpiderCaveNovember21Tests covers newly generated decorated caves.
            if (kind.equals("spider")) {
                var context = StructurePieceSerializationContext.fromLevel(level);
                var saved = piece.createTag(context);
                saved.remove("SpiderCaveVersion");
                piece = new BossLairPiece(context, saved);
            }
            var box = piece.getBoundingBox();
            prepare(level, box, center.getY());
            var serial = StructurePieceSerializationContext.fromLevel(level);
            var chunks = chunks(box);
            // Reverse traversal plus a mid-placement reload exercises the chunk
            // boundary through both branches and preserves each resident bit.
            Collections.reverse(chunks);
            for (int index = 0; index < chunks.size(); index++) {
                place(level, piece, chunks.get(index), center);
                if (index == chunks.size() / 2) piece = new BossLairPiece(serial, piece.createTag(serial));
            }
            var layout = piece.branchLayout();
            BlockPos outside = kind.equals("spider") ? center.offset(58, 1, 58) : center.offset(0, 1, 40);
            var connected = walk(level, box, outside);
            for (BlockPos local : layout.branches()) helper.assertTrue(connected.containsKey(center.offset(local)),
                    kind + " branch is disconnected or lacks a solid floor/two blocks of headroom: " + local);
            BlockPos boss = center.offset(layout.residents().get(0));
            helper.assertTrue(connected.containsKey(boss), kind + " boss chamber must be walkable from outside: " + boss);
            helper.assertTrue(boss.getY() < center.getY() && connected.get(boss) >= 50,
                    kind + " encounter must lead along a substantial descending route into a deeper boss chamber");
            if (kind.equals("wither")) assertSealedWitherChamber(helper, level, box, center, boss);
            var chests = snapshot(level, box).entrySet().stream().filter(entry -> entry.getValue().is(Blocks.CHEST)).toList();
            helper.assertTrue(chests.size() == (kind.equals("spider") ? 5 : 3), "Every new pocket/boss room needs its loot chest: " + kind);
            for (var entry : chests) {
                var data = level.getBlockEntity(entry.getKey()).saveWithFullMetadata();
                helper.assertTrue(data.getString("LootTable").equals("aoemod:chests/" + kind + "_lair"), "Chest needs the matching loot table");
                var table = level.getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation(data.getString("LootTable")));
                helper.assertTrue(table != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "Missing lair loot table");
            }
            if (kind.equals("spider")) {
                helper.assertTrue(level.getBlockState(center.offset(30, 5, 30)).is(Blocks.MOSS_BLOCK), "Former dome volume must be solid hillside");
                helper.assertTrue(level.getBlockState(center.offset(44, 8, 44)).is(Blocks.MOSS_BLOCK), "Horizontal approach needs a continuous roof");
                long vines = snapshot(level, box).values().stream().filter(state -> state.is(Blocks.CAVE_VINES)).count();
                helper.assertTrue(vines == 0, "Spider cave pockets must not contain cave vines");
            }
            var residents = residents(level, box);
            helper.assertTrue(residents.size() == 7 && residents.stream().allMatch(level::noCollision),
                    kind + " must preserve its boss and six helpers without collisions: count=" + residents.size()
                            + "; collisions=" + residentCollisionDetails(level, residents));
            String bossId = kind.equals("spider") ? "aoemod:spider_queen" : "minecraft:wither";
            helper.assertTrue(residents.stream().filter(mob -> ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString().equals(bossId))
                            .allMatch(mob -> mob.blockPosition().equals(boss)), "Boss must spawn in the deepest chamber");
            var before = snapshot(level, box);
            piece = new BossLairPiece(serial, piece.createTag(serial));
            Collections.reverse(chunks);
            for (var chunk : chunks) place(level, piece, chunk, center);
            var replayed = snapshot(level, box);
            var differences = before.entrySet().stream().filter(entry -> !entry.getValue().equals(replayed.get(entry.getKey())))
                    .map(entry -> entry.getKey() + ": " + entry.getValue() + " -> " + replayed.get(entry.getKey())).toList();
            helper.assertTrue(differences.isEmpty(), "Saved " + kind + " geometry must be independent of chunk order/replay; differences="
                    + differences.size() + "; first=" + differences.stream().limit(8).toList());
            helper.assertTrue(residents(level, box).size() == 7, "Replaying saved " + kind + " pieces must not duplicate residents");
            freeze(residents);
            exportPlan(kind, level, box, connected, boss, connected.get(boss));
            viewToward(kind + "-exterior", level,
                    kind.equals("spider") ? center.offset(78, 40, 82) : center.offset(48, 30, 58),
                    kind.equals("spider") ? center.offset(30, 7, 30) : center.offset(0, 4, 0));
            viewToward(kind + "-entry", level,
                    kind.equals("spider") ? center.offset(53, 2, 53) : center.offset(0, 2, 35),
                    kind.equals("spider") ? center.offset(44, 2, 44) : center.offset(0, 2, 23));
            for (int index = 0; index < layout.branches().size(); index++) {
                BlockPos pocket = center.offset(layout.branches().get(index));
                viewToward(kind + "-pocket-" + (index + 1), level, pocket.offset(3, 1, 4), pocket.above());
            }
            viewToward(kind + "-boss", level, boss.offset(0, 4, 10), boss.above(2));
        }

        ServerLevel savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockPos campCenter = new BlockPos(12288, savanna.getMaxBuildHeight() - 48, 12288);
        // Keep this saved-settlement connectivity regression on its authored
        // version-two geometry; current villages have their own complete audit.
        var camp = versionTwoCamp(savanna, campCenter);
        var campBox = camp.getBoundingBox();
        prepare(savanna, campBox, campCenter.getY());
        for (var chunk : chunks(campBox)) place(savanna, camp, chunk, campCenter);
        var origin = camp.templatePosition();
        BlockPos campEntry = origin.offset(45, 2, 77);
        var campWalk = walk(savanna, campBox, campEntry);
        for (var house : SavannaOrcSettlement.HOUSE_CENTERS) helper.assertTrue(campWalk.containsKey(origin.offset(house)),
                "Every added camp building must connect through its real three-wide doorway: " + house + "; visited=" + campWalk.size()
                        + "; entry=" + states(savanna, campEntry) + "; house=" + states(savanna, origin.offset(house)));
        BlockPos orcBoss = origin.offset(45, 3, 20);
        helper.assertTrue(campWalk.containsKey(orcBoss), "The settlement entrance must reach the native boss tent");
        var orcs = residents(savanna, campBox);
        helper.assertTrue(orcs.size() == 23 && orcs.stream().allMatch(savanna::noCollision),
                "Saved version-two settlement retains one boss, six guards and sixteen unobstructed pen animals: count="
                        + orcs.size() + "; collisions=" + residentCollisionDetails(savanna, orcs));
        helper.assertTrue(orcs.stream().filter(mob -> ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getNamespace().equals("orcz")).count() == 7,
                "Livestock must not replace any of the seven Orcz residents");
        var campSerial = StructurePieceSerializationContext.fromLevel(savanna);
        camp = new SavannaOrcCampPiece(campSerial, camp.createTag(campSerial));
        for (var chunk : chunks(campBox)) place(savanna, camp, chunk, campCenter);
        helper.assertTrue(residents(savanna, campBox).size() == 23, "Saved settlement replay must not duplicate guards or animals");
        freeze(orcs);
        exportPlan("orc", savanna, campBox, campWalk, orcBoss, campWalk.get(orcBoss));
        viewToward("orc-exterior", savanna, campCenter.offset(-62, 38, 61), campCenter.above(3));
        viewToward("orc-full-compound", savanna, campCenter.offset(0, 74, 30), campCenter.above(2));
        viewToward("orc-courtyard", savanna, origin.offset(45, 6, 69), origin.offset(45, 4, 39));
        viewToward("orc-native-large-house", savanna, origin.offset(41, 5, 21), origin.offset(26, 6, 21));
        viewToward("orc-native-small-house", savanna, origin.offset(55, 5, 69), origin.offset(55, 5, 59));
        viewToward("orc-sheep-pen", savanna, origin.offset(25, 8, 46), origin.offset(15, 3, 39));
        viewToward("orc-boar-pen", savanna, origin.offset(65, 8, 46), origin.offset(75, 3, 39));
        viewToward("orc-irrigated-crops", savanna, origin.offset(45, 16, 65), origin.offset(45, 3, 39));
        viewToward("orc-curved-palisade", savanna, origin.offset(13, 9, 55), origin.offset(4, 5, 39));
        writeReports();
        helper.succeed();
    }

    /** Exercise the production NBT migration path for an existing version-two camp. */
    private static SavannaOrcCampPiece versionTwoCamp(ServerLevel level, BlockPos center) {
        var context = StructurePieceSerializationContext.fromLevel(level);
        var tag = new SavannaOrcCampPiece(level.getStructureManager(), center).createTag(context);
        tag.putString("Template", "aoemod:orc_settlement");
        tag.putInt("SettlementVersion", 2);
        tag.putInt("TPX", center.getX() - 45);
        tag.putInt("TPY", center.getY());
        tag.putInt("TPZ", center.getZ() - 39);
        tag.remove("VillageResidents"); tag.remove("VillageCushions"); tag.remove("VillageAnimals");
        return new SavannaOrcCampPiece(context, tag);
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void expandedYetiApproachKeepsTheNativeEncounterAndContinuousSupport(GameTestHelper helper) throws Exception {
        ServerLevel snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos yetiCenter = new BlockPos(8192, snowy.getMaxBuildHeight() - 48, 8192);
        var yeti = new BossLairPiece("yeti", yetiCenter, 67);
        var yetiBox = yeti.getBoundingBox();
        prepare(snowy, yetiBox, yetiCenter.getY());
        for (var chunk : chunks(yetiBox)) place(snowy, yeti, chunk, yetiCenter);
        var yetiWalk = walk(snowy, yetiBox, yetiCenter.offset(0, 1, 45));
        BlockPos yetiEncounter = yetiCenter.offset(0, 1, 4);
        helper.assertTrue(yetiWalk.containsKey(yetiEncounter) && yetiWalk.get(yetiEncounter) <= 45,
                "Yeti has a short direct walk from outside to the central encounter across the flat chamber floor");
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (int across = -8; across <= 8; across++) {
                int floor = 0;
                var column = yetiCenter.relative(direction, 27).relative(direction.getClockWise(), across);
                helper.assertTrue(canStand(snowy, column.above(floor + 1))
                                && snowy.getBlockState(column.above(floor)).is(Blocks.SNOW_BLOCK),
                        "Each Yeti entrance must use the flat snow approach");
                int ceiling = (Math.abs(across) == 6 ? 15 : Math.min(16, 40 - 4 * Math.abs(across))) + 1;
                helper.assertTrue(snowy.getBlockState(column.above(ceiling - 1)).isAir()
                                && snowy.getBlockState(column.above(ceiling)).is(Blocks.SNOW_BLOCK),
                        "Each arch must taper toward the sides");
            }
        }
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) for (int side : new int[]{-9, 9}) {
            var column = yetiCenter.relative(direction, 25).relative(direction.getClockWise(), side);
            for (int y = 1; y <= 17; y++) helper.assertTrue(snowy.getBlockState(column.above(y)).is(Blocks.SNOW_BLOCK),
                    "An extra perpendicular snow column must conceal the inner ice beside each portal");
        }
        for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
            helper.assertTrue(snowy.getBlockState(yetiCenter.offset(x, 0, z)).is(Blocks.SNOW_BLOCK)
                    && snowy.getBlockState(yetiCenter.offset(x, 18, z)).is(Blocks.BLUE_ICE),
                    "The snow floor and inner blue-ice ceiling must be flat throughout");
            for (int dy = 2; dy < 18; dy++) helper.assertTrue(snowy.getBlockState(yetiCenter.offset(x, dy, z)).isAir(),
                    "The chamber must be clear of icicles");
        }
        var nativeSpawner = ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("twilightforest:alpha_yeti_boss_spawner"));
        helper.assertTrue(snapshot(snowy, yetiBox).values().stream().filter(state -> state.is(nativeSpawner)).count() == 1,
                "Keep exactly one native central Alpha Yeti encounter");
        helper.assertTrue(snapshot(snowy, yetiBox).values().stream().filter(state -> state.is(Blocks.CHEST)).count() == 2,
                "Yeti loot chests must survive the native cave decoration pass");
        freeze(residents(snowy, yetiBox));
        exportPlan("yeti", snowy, yetiBox, yetiWalk, yetiEncounter, yetiWalk.get(yetiEncounter));
        viewToward("yeti-exterior", snowy, yetiCenter.offset(58, 39, 68), yetiCenter.above(7));
        viewToward("yeti-approach", snowy, yetiCenter.offset(0, 3, 42), yetiCenter.offset(0, 3, 14));
        int lootViewIndex = 0;
        for (var chest : snapshot(snowy, yetiBox).entrySet().stream().filter(entry -> entry.getValue().is(Blocks.CHEST))
                .sorted(Map.Entry.comparingByKey()).toList()) {
            lootView("yeti-native-floor-loot-" + (++lootViewIndex), snowy, chest.getKey());
        }
        writeReports();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void mansionKeepsTheLargestRoomAndStoreyCount(GameTestHelper helper) {
        var level = helper.getLevel();
        var serial = StructurePieceSerializationContext.fromLevel(level);
        for (int seed = 0; seed < 32; seed++) {
            var builder = new StructurePiecesBuilder();
            IllagerMansionStructure.addMansionPieces(level.getStructureManager(), BlockPos.ZERO,
                    Rotation.NONE, builder, RandomSource.create(seed));
            int rooms = 0;
            Set<Integer> floors = new HashSet<>();
            for (StructurePiece piece : builder.build().pieces()) {
                if (!(piece instanceof WoodlandMansionPieces.WoodlandMansionPiece)) continue;
                String template = piece.createTag(serial).getString("Template");
                if (template.matches(".*(?:1x1|1x2|2x2)_[a-z0-9_]+")) {
                    rooms++;
                    floors.add(piece.getBoundingBox().minY());
                }
            }
            helper.assertTrue(rooms > 6 && floors.size() >= 2,
                    "Every tested unchanged mansion must exceed the six-building camp and single-storey cave complexes: seed="
                            + seed + ", rooms=" + rooms + ", floors=" + floors);
        }
        helper.succeed();
    }

    private static void prepare(ServerLevel level, BoundingBox box, int floor) {
        var substrate = level.dimension().equals(GameplayDimensions.TROPICAL) ? Blocks.MOSS_BLOCK
                : level.dimension().equals(GameplayDimensions.SAVANNA) ? Blocks.MUD
                : level.dimension().equals(GameplayDimensions.NETHER) ? Blocks.OBSIDIAN : Blocks.SNOW_BLOCK;
        for (var chunk : chunks(new BoundingBox(box.minX() - 2, box.minY(), box.minZ() - 2, box.maxX() + 2, box.maxY(), box.maxZ() + 2))) {
            level.getChunk(chunk.x, chunk.z);
        }
        // These fixed audit arenas are deliberately preserved for screenshots.
        // Clear only their previous authored actors before a fresh test run;
        // the replay phase below still retains and checks all placement flags.
        level.getEntities((net.minecraft.world.entity.Entity) null, AABB.of(box).inflate(2), entity ->
                entity instanceof Mob mob && StructureSpawnedMobData.isMarked(mob)
                        || entity instanceof com.blackgear.vanillabackport.common.level.entities.decoration.Cushion)
                .forEach(net.minecraft.world.entity.Entity::discard);
        for (int x = box.minX() - 2; x <= box.maxX() + 2; x++) for (int z = box.minZ() - 2; z <= box.maxZ() + 2; z++) {
            for (int y = Math.min(floor - 20, box.minY()); y <= box.maxY() + 1; y++) {
                var block = y <= floor ? substrate : Blocks.AIR;
                // Natural Savanna has a full dirt/grass walking surface above
                // mud. Bare mud is 14/16 high and cannot start this deliberately
                // integer-height, full-support walking audit.
                if (y == floor && level.dimension().equals(GameplayDimensions.SAVANNA)) block = Blocks.GRASS_BLOCK;
                level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
            }
        }
    }

    private static List<ChunkPos> chunks(BoundingBox box) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
        return chunks;
    }

    private static BoundingBox clip(ServerLevel level, ChunkPos chunk) {
        return new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
    }

    private static void place(ServerLevel level, StructurePiece piece, ChunkPos chunk, BlockPos center) {
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(197),
                clip(level, chunk), chunk, center);
    }

    private static Map<BlockPos, BlockState> snapshot(ServerLevel level, BoundingBox box) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var state = level.getBlockState(pos);
            // LevelChunk skips air writes into an entirely empty section. The
            // first pass can therefore retain AIR where replay stores CAVE_AIR;
            // both have identical physical geometry. Compare every nonair
            // block/property and fluid exactly, including vine maturity.
            states.put(pos.immutable(), state.isAir() ? Blocks.AIR.defaultBlockState() : state);
        }
        return states;
    }

    private static List<Mob> residents(ServerLevel level, BoundingBox box) {
        return level.getEntitiesOfClass(Mob.class, AABB.of(box), StructureSpawnedMobData::isMarked);
    }

    private static void freeze(List<Mob> mobs) {
        mobs.forEach(mob -> { mob.setNoAi(true); mob.setInvulnerable(true); mob.setPersistenceRequired(); });
    }

    private static List<String> residentCollisionDetails(ServerLevel level, List<Mob> mobs) {
        List<String> collisions = new ArrayList<>();
        for (Mob mob : mobs) {
            if (level.noCollision(mob)) continue;
            AABB bounds = mob.getBoundingBox();
            List<String> blocks = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                    BlockPos.containing(bounds.maxX - 1.0e-7, bounds.maxY - 1.0e-7, bounds.maxZ - 1.0e-7))) {
                BlockState state = level.getBlockState(pos);
                if (state.getCollisionShape(level, pos).toAabbs().stream()
                        .anyMatch(shape -> shape.move(pos).intersects(bounds))) blocks.add(pos + "=" + state);
            }
            var overlapping = level.getEntities(mob, bounds).stream()
                    .map(other -> ForgeRegistries.ENTITY_TYPES.getKey(other.getType()) + " at " + other.position()
                            + " bounds=" + other.getBoundingBox()).toList();
            collisions.add(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()) + " at " + mob.position()
                    + " bounds=" + bounds + " blocks=" + blocks
                    + " entityCollisionShapes=" + level.getEntityCollisions(mob, bounds).size()
                    + " overlappingEntities=" + overlapping);
        }
        return collisions;
    }

    private static boolean canStand(ServerLevel level, BlockPos feet) {
        return (level.getBlockState(feet.below()).isCollisionShapeFullBlock(level, feet.below()) || level.getBlockState(feet.below()).is(Blocks.DIRT_PATH))
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty();
    }

    private static String states(ServerLevel level, BlockPos feet) {
        return feet + " support=" + level.getBlockState(feet.below()) + " feet=" + level.getBlockState(feet)
                + " head=" + level.getBlockState(feet.above());
    }

    private static Map<BlockPos, Integer> walk(ServerLevel level, BoundingBox bounds, BlockPos start) {
        Map<BlockPos, Integer> visited = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        if (!canStand(level, start)) return visited;
        queue.add(start); visited.put(start, 0);
        while (!queue.isEmpty()) {
            var from = queue.remove();
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                var to = from.relative(direction).above(dy);
                if (to.getX() < bounds.minX() - 2 || to.getX() > bounds.maxX() + 2 || to.getZ() < bounds.minZ() - 2 || to.getZ() > bounds.maxZ() + 2
                        || to.getY() < bounds.minY() || to.getY() > bounds.maxY() || visited.containsKey(to) || !canStand(level, to)) continue;
                // The head also needs room while stepping up/down through the doorway.
                var clearance = dy > 0 ? from.above(2) : dy < 0 ? to.above(2) : null;
                if (clearance != null && !level.getBlockState(clearance).getCollisionShape(level, clearance).isEmpty()) continue;
                visited.put(to, visited.get(from) + 1); queue.add(to);
            }
        }
        return visited;
    }

    private static void assertSealedWitherChamber(GameTestHelper helper, ServerLevel level, BoundingBox bounds,
                                                 BlockPos center, BlockPos boss) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(boss); visited.add(boss);
        while (!queue.isEmpty()) {
            BlockPos from = queue.remove();
            for (var direction : net.minecraft.core.Direction.values()) {
                BlockPos to = from.relative(direction);
                // Close only the authored seven-wide/seven-high front mouth
                // for this air-connectivity audit; every other exterior cell
                // must be separated from the real interior by solid obsidian.
                if (to.getZ() == bounds.maxZ() && Math.abs(to.getX() - center.getX()) <= 3
                        && to.getY() > center.getY() && to.getY() <= center.getY() + 7) continue;
                if (visited.contains(to) || !level.getBlockState(to).isAir()) continue;
                helper.assertTrue(bounds.isInside(to), "Wither chamber has an unintended shell opening toward " + to);
                visited.add(to); queue.add(to);
            }
        }
        helper.assertTrue(visited.size() > 2000, "Sealed-shell audit must visit the full branching cave interior");
    }

    private static void exportPlan(String label, ServerLevel level, BoundingBox box, Map<BlockPos, Integer> walking,
                                   BlockPos boss, int route) throws Exception {
        Files.createDirectories(REPORTS);
        var image = new BufferedImage(box.getXSpan() * 5, box.getZSpan() * 5, BufferedImage.TYPE_INT_RGB);
        Map<Long, Integer> floors = new HashMap<>();
        walking.keySet().forEach(pos -> floors.merge(((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL), pos.getY(), Math::min));
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            Integer y = floors.get(((long) x << 32) ^ (z & 0xffffffffL));
            int color = y == null ? 0x222831 : java.awt.Color.HSBtoRGB(.48f + (y - boss.getY()) * .008f, .65f, .9f);
            if (Math.abs(x - boss.getX()) <= 1 && Math.abs(z - boss.getZ()) <= 1) color = 0xffbb33;
            for (int px = 0; px < 5; px++) for (int pz = 0; pz < 5; pz++) image.setRGB((x - box.minX()) * 5 + px, (z - box.minZ()) * 5 + pz, color);
        }
        ImageIO.write(image, "png", REPORTS.resolve("boss-expansion-" + label + ".png").toFile());
        METRICS.add(Map.of("structure", label, "dimension", level.dimension().location().toString(), "bounds", box.toString(),
                "connectedWalkingCells", walking.size(), "shortestBossWalk", route, "boss", List.of(boss.getX(), boss.getY(), boss.getZ())));
    }

    /** Player coordinates are feet positions; aim from the spectator's real eye height. */
    private static void viewToward(String label, ServerLevel level, BlockPos camera, BlockPos target) {
        double dx = target.getX() - camera.getX(), dz = target.getZ() - camera.getZ();
        double dy = target.getY() + .5 - (camera.getY() + 1.62);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        view(label, level, camera, yaw, pitch);
    }

    /** Select a clear native-world sightline; never remove an icicle to obtain a view. */
    private static void lootView(String label, ServerLevel level, BlockPos chest) {
        for (int[] offset : new int[][]{{0, 4}, {4, 0}, {-4, 0}, {0, -4}, {3, 3}, {-3, 3}, {3, -3}, {-3, -3}, {0, 0}}) {
            BlockPos camera = chest.offset(offset[0], 1, offset[1]);
            if (!level.getBlockState(camera).isAir() || !level.getBlockState(camera.above()).isAir()) continue;
            var eye = new net.minecraft.world.phys.Vec3(camera.getX() + .5, camera.getY() + 1.62, camera.getZ() + .5);
            var target = net.minecraft.world.phys.Vec3.atCenterOf(chest);
            var ray = new net.minecraft.world.level.ClipContext(eye, target,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, null);
            if (level.clip(ray).getBlockPos().equals(chest)) {
                viewToward(label, level, camera, chest);
                return;
            }
        }
    }

    private static void view(String label, ServerLevel level, BlockPos pos, float yaw, float pitch) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("dimension", level.dimension().location().toString()); view.put("x", pos.getX() + .5);
        view.put("y", pos.getY()); view.put("z", pos.getZ() + .5); view.put("yaw", yaw); view.put("pitch", pitch); view.put("label", label);
        VIEWS.add(view);
    }

    private static void writeReports() throws Exception {
        Files.createDirectories(REPORTS);
        var gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(REPORTS.resolve("boss-expansion-views.json"), gson.toJson(VIEWS));
        Files.writeString(REPORTS.resolve("boss-expansion-metrics.json"), gson.toJson(METRICS));
    }
}
