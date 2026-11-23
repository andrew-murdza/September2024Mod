package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.BossStructureProtection;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.BranchedBossCaves;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.amurdza.examplemod.worldgen.structure.SpiderCaveLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Actual authored blocks, navigation, resident persistence, and screenshots for the expanded cave. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiderCaveNovember21Tests {
    private static final Path REPORTS = Path.of("build", "reports");

    private SpiderCaveNovember21Tests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void expandedLayoutMirrorsAndKeepsLegacySavedCaves(GameTestHelper helper) {
        var serial = StructurePieceSerializationContext.fromLevel(helper.getLevel());
        var canonical = new BranchedBossCaves("spider", 100, 0, true);
        int expandedFloor = floorCount(canonical);
        int legacyFloor = floorCount(new BranchedBossCaves("spider", 100, 0, false));
        int campFloor = 0;
        for (int x = 0; x < SavannaOrcVillage.SIZE; x++) for (int z = 0; z < SavannaOrcVillage.SIZE; z++) {
            if (SavannaOrcVillage.contains(x, z)) campFloor++;
        }
        helper.assertTrue(expandedFloor > legacyFloor && expandedFloor < campFloor,
                "Cave floor tiles must increase but stay below the orc camp: old=" + legacyFloor
                        + ", expanded=" + expandedFloor + ", camp=" + campFloor);
        for (int seed = 0; seed < 4; seed++) {
            int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
            var mirrored = new BranchedBossCaves("spider", 100, seed, true);
            for (int x = -64; x <= 64; x++) for (int z = -64; z <= 64; z++) {
                helper.assertTrue(java.util.Objects.equals(canonical.column(x, z), mirrored.column(x * sx, z * sz)),
                        "Every mirrored shell/floor/ceiling must match the authored geometry");
            }
            var fresh = new BossLairPiece("spider", new BlockPos(0, 100, 0), seed);
            var tag = fresh.createTag(serial);
            helper.assertTrue(tag.getInt("SpiderCaveVersion") > 0, "New caves need an explicit saved layout version");
            var saved = new BossLairPiece(serial, tag);
            helper.assertTrue(floorCount(saved.branchLayout()) == expandedFloor,
                    "Newly saved caves retain the expanded layout");
            tag.remove("SpiderCaveVersion");
            tag.putInt("ResidentsPlaced", 127);
            var old = new BossLairPiece(serial, tag);
            helper.assertTrue(floorCount(old.branchLayout()) == legacyFloor
                            && old.branchLayout().residents().size() == 7 && old.branchLayout().lootPositions().size() == 5,
                    "Unversioned saved caves retain their original seven residents, five chests and geometry");
            helper.assertTrue(old.createTag(serial).getInt("ResidentsPlaced") == 127,
                    "Migration must retain the original saved placement bits");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void farmOffspringInheritCaveMembershipFromEitherParent(GameTestHelper helper) {
        var level = helper.getLevel();
        var home = helper.absolutePos(new BlockPos(3, 2, 3));
        var box = new AABB(home).inflate(4, 3, 4);
        var boundaries = BossStructureBounds.get(level);
        String caveKey = "lair:spider:offspring-test:" + home.asLong();
        String otherKey = "lair:yeti:offspring-test:" + home.asLong();
        var boundary = new BossStructureBounds.Boundary(caveKey, box, null);
        var parent = ModEntities.ARTHROPLEURA.get().create(level);
        var mate = ModEntities.ARTHROPLEURA.get().create(level);
        parent.moveTo(home.getX() + .5, home.getY(), home.getZ() + .5, 0, 0);
        mate.moveTo(home.getX() + 1.5, home.getY(), home.getZ() + .5, 0, 0);
        boundaries.record(caveKey, box, null);
        try {
            helper.assertFalse(StructureSpawnedMobData.isMarked(parent.getBreedOffspring(level, mate)),
                    "Natural parents must not acquire cave membership merely by standing inside its boundary");
            StructureSpawnedMobData.mark(parent, home);
            // Both the normal child and every extra twin use the same offspring factory.
            for (int sibling = 0; sibling < 2; sibling++) {
                var baby = parent.getBreedOffspring(level, mate);
                assertFarmBaby(helper, baby, home, boundary);
            }
            var fromMate = mate.getBreedOffspring(level, parent);
            assertFarmBaby(helper, fromMate, home, boundary);
            var tag = new CompoundTag();
            fromMate.saveWithoutId(tag);
            var restored = ModEntities.ARTHROPLEURA.get().create(level);
            restored.load(tag);
            helper.assertTrue(restored.isBaby() && StructureSpawnedMobData.isMarked(restored)
                            && BossStructureProtection.belongs(restored, boundary),
                    "Saved farm offspring must retain their baby age and cave membership");
            assertFarmBaby(helper, restored, home, boundary);

            parent.moveTo(box.maxX + 2, home.getY(), home.getZ() + .5);
            helper.assertFalse(StructureSpawnedMobData.isMarked(parent.getBreedOffspring(level, mate)),
                    "A parent relocated outside its cave must not tether new offspring to the old cave");
            parent.moveTo(home.getX() + .5, home.getY(), home.getZ() + .5);
            boundaries.remove(caveKey);
            boundaries.record(otherKey, box, null);
            helper.assertFalse(StructureSpawnedMobData.isMarked(parent.getBreedOffspring(level, mate)),
                    "Marked parents in other structures must retain ordinary offspring behavior");
        } finally {
            boundaries.remove(caveKey);
            boundaries.remove(otherKey);
        }
        helper.succeed();
    }

    private static void assertFarmBaby(GameTestHelper helper, net.amurdza.examplemod.entity.ArthropleuraEntity baby,
                                       BlockPos home, BossStructureBounds.Boundary boundary) {
        helper.assertTrue(baby != null && StructureSpawnedMobData.isMarked(baby)
                        && StructureSpawnedMobData.home(baby).equals(home)
                        && baby.isPersistenceRequired(),
                "Every farm newborn must inherit the resident parent's saved home and persistence");
        baby.setBaby(true);
        baby.moveTo(home.getX() + .5, home.getY(), home.getZ() + .5, 0, 0);
        helper.assertTrue(baby.isBaby() && !baby.isNoAi() && BossStructureProtection.belongs(baby, boundary),
                "Membership must preserve a normal baby and identify it as a cave resident");
        var before = baby.position();
        int entityId = baby.getId();
        try {
            baby.setId((int) -baby.level().getGameTime());
            BossStructureProtection.tick(new LivingEvent.LivingTickEvent(baby));
        } finally {
            baby.setId(entityId);
        }
        helper.assertTrue(baby.position().equals(before), "The outsider guard must not eject a newborn farm resident");
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void decoratedCaveHasConnectedRoomsFixedRosterAndWallLoot(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos center = new BlockPos(32102, level.getMaxBuildHeight() - 48, 32102);
        var serial = StructurePieceSerializationContext.fromLevel(level);
        var piece = new BossLairPiece("spider", center, 0);
        var box = piece.getBoundingBox();
        prepare(level, box, center.getY());
        var chunks = chunks(box);
        Collections.reverse(chunks);
        for (int i = 0; i < chunks.size(); i++) {
            var chunk = chunks.get(i);
            long neighbors = neighborRingFingerprint(level, chunk, box);
            place(level, piece, chunk, center);
            helper.assertTrue(neighborRingFingerprint(level, chunk, box) == neighbors,
                    "Generating a cave chunk must not write into its neighbors: " + chunk);
            if (i == chunks.size() / 2) piece = new BossLairPiece(serial, piece.createTag(serial));
        }
        var layout = piece.branchLayout();
        var walking = walk(level, box, center.offset(layout.entrance()));
        for (var room : SpiderCaveLayout.chambers()) {
            var feet = center.offset(room.center()).above();
            helper.assertTrue(walking.containsKey(feet), "The decorated " + room.theme()
                    + " room must be walkable from the entrance: " + feet + "; visited=" + walking.size());
        }
        var boss = center.offset(SpiderCaveLayout.residents().get(0).pos());
        helper.assertTrue(walking.containsKey(boss) && walking.get(boss) >= 75,
                "The queen needs a substantial connected descent below the entrance");

        var expected = new TreeMap<String, Long>();
        for (var resident : SpiderCaveLayout.residents()) expected.merge(resident.id(), 1L, Long::sum);
        var mobs = residents(level, box);
        helper.assertTrue(mobs.size() > 64, "This regression must exercise saved resident bits beyond both int and long");
        helper.assertTrue(roster(mobs).equals(expected), "Each cave needs exactly its authored roster: " + roster(mobs)
                + "; expected=" + expected);
        helper.assertTrue(expected.getOrDefault("aoemod:spider_queen", 0L) == 1
                        && mobs.size() - expected.getOrDefault("aoemod:arthropleura", 0L) <= 97,
                "There must be one queen and at most 96 other monsters");
        helper.assertTrue(!expected.containsKey("minecraft:drowned"), "The final design omits drowned");
        helper.assertTrue(level.getEntitiesOfClass(Mob.class, AABB.of(box), mob -> true).size() == mobs.size(),
                "Random passengers or incidental mobs must not change the authored roster");
        for (String id : List.of("minecraft:spider", "minecraft:cave_spider", "aoemod:bogged", "minecraft:slime", "twilightforest:slime_beetle")) {
            helper.assertTrue(expected.getOrDefault(id, 0L) > 0, "The cave needs its requested monster type: " + id);
        }
        helper.assertTrue(expected.get("aoemod:bogged") < expected.get("minecraft:spider"),
                "Bogged must remain a smaller share of the population than spiders");
        var slimeRoom = SpiderCaveLayout.chambers().stream().filter(room -> room.theme() == SpiderCaveLayout.Theme.SLIME).findFirst().orElseThrow();
        for (String id : List.of("minecraft:slime", "twilightforest:slime_beetle")) {
            helper.assertTrue(SpiderCaveLayout.residents().stream().anyMatch(resident -> resident.id().equals(id)
                            && resident.pos().distSqr(slimeRoom.center()) > slimeRoom.radius() * slimeRoom.radius()),
                    "Some " + id + " residents must also inhabit the other themed rooms");
        }
        for (var mob : mobs) {
            helper.assertTrue(level.noCollision(mob), "Authored resident intersects the cave: "
                    + ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()) + " at " + mob.position());
            helper.assertTrue(mob.isPersistenceRequired(), "Authored residents must not despawn");
            mob.setNoAi(true); mob.setInvulnerable(true);
            if (ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString().equals("aoemod:arthropleura")) {
                var local = StructureSpawnedMobData.home(mob).subtract(center);
                helper.assertTrue(SpiderCaveLayout.isFarmPit(local.getX(), local.getZ()),
                        "Every farm animal must start inside the lowered larder pit");
            }
        }

        var palette = new HashMap<Block, Integer>();
        int chestCount = 0, berryVines = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var state = level.getBlockState(cursor);
            if (!state.isAir()) palette.merge(state.getBlock(), 1, Integer::sum);
            helper.assertTrue(state.getFluidState().isEmpty(), "The cave must have no swamp water: " + cursor + " = " + state);
            helper.assertTrue(!(state.getBlock() instanceof CropBlock), "No wild crops belong inside the cave");
            if (state.is(Blocks.CHEST)) {
                chestCount++;
                var facing = state.getValue(ChestBlock.FACING);
                var back = cursor.relative(facing.getOpposite());
                helper.assertTrue(level.getBlockState(back).isFaceSturdy(level, back, facing),
                        "Chest must face into the room with a solid wall directly behind: " + cursor);
                helper.assertTrue(!level.getBlockState(cursor.above()).isRedstoneConductor(level, cursor.above()),
                        "Wall chest lid must be unobstructed");
                helper.assertTrue(walking.containsKey(cursor.relative(facing)), "Each wall chest must have a reachable front");
                var data = level.getBlockEntity(cursor).saveWithFullMetadata();
                helper.assertTrue(data.getString("LootTable").equals("aoemod:chests/spider_lair"),
                        "Every wall chest needs the spider cave loot table");
            }
            if (vine(state.getBlock())) {
                if (state.getValue(BlockStateProperties.BERRIES)) berryVines++;
                BlockPos top = cursor.immutable();
                while (vine(level.getBlockState(top.above()).getBlock())) top = top.above();
                var support = top.above();
                helper.assertTrue(level.getBlockState(support).isFaceSturdy(level, support, Direction.DOWN),
                        "Hanging vines need a real ceiling attachment: " + cursor);
                var local = cursor.subtract(center);
                helper.assertTrue(!BossLairPiece.spiderEntrance(local.getX(), local.getZ(), local.getY(), 0),
                        "Cave vines must leave the entrance clear");
            }
        }
        helper.assertTrue(chestCount == SpiderCaveLayout.lootPositions().size() && chestCount >= 6,
                "Each enlarged chamber needs its wall chest: count=" + chestCount);
        helper.assertTrue(berryVines > 10, "Glowing cave vines must light the new chambers");
        for (Block required : List.of(Blocks.SLIME_BLOCK, Blocks.GRASS, Blocks.FERN, Blocks.RED_MUSHROOM,
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK,
                Blocks.MUSHROOM_STEM, Blocks.BONE_BLOCK, Blocks.WHITE_WOOL, Blocks.COBWEB,
                Blocks.OCHRE_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT,
                Blocks.SPORE_BLOSSOM, Blocks.AZALEA, Blocks.FLOWERING_AZALEA)) {
            helper.assertTrue(palette.containsKey(required), "Missing requested cave material: " + ForgeRegistries.BLOCKS.getKey(required));
        }
        long before = fingerprint(level, box);
        var savedTag = piece.createTag(serial);
        helper.assertTrue(java.util.BitSet.valueOf(savedTag.getLongArray("SpiderResidents")).cardinality() == mobs.size(),
                "Every placed resident needs a saved bit, including indices beyond 64");
        piece = new BossLairPiece(serial, savedTag);
        Collections.reverse(chunks);
        for (var chunk : chunks) place(level, piece, chunk, center);
        helper.assertTrue(fingerprint(level, box) == before,
                "Saved decoration, pits and wall loot must survive reversed chunk replay unchanged");
        helper.assertTrue(roster(residents(level, box)).equals(expected),
                "Saved placement flags beyond index 64 must prevent duplicated or replaced residents");
        export(level, center, box, layout, walking, expected, palette);
        helper.succeed();
    }

    private static boolean vine(Block block) { return block == Blocks.CAVE_VINES || block == Blocks.CAVE_VINES_PLANT; }

    private static int floorCount(BranchedBossCaves layout) {
        int count = 0;
        for (int x = -64; x <= 64; x++) for (int z = -64; z <= 64; z++) {
            var c = layout.column(x, z);
            if (c != null && c.hasAir() && c.ceiling() - c.floor() >= 2) count++;
        }
        return count;
    }

    private static void prepare(ServerLevel level, BoundingBox box, int floor) {
        level.getEntitiesOfClass(Mob.class, AABB.of(box).inflate(2), mob -> true).forEach(Mob::discard);
        for (int x = box.minX() - 1; x <= box.maxX() + 1; x++) for (int z = box.minZ() - 1; z <= box.maxZ() + 1; z++) {
            for (int y = box.minY() - 1; y <= box.maxY() + 1; y++) {
                level.setBlock(new BlockPos(x, y, z), (y <= floor ? Blocks.MOSS_BLOCK : Blocks.AIR).defaultBlockState(), 2);
            }
        }
    }

    private static List<ChunkPos> chunks(BoundingBox box) {
        var result = new ArrayList<ChunkPos>();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) result.add(new ChunkPos(x, z));
        return result;
    }

    private static void place(ServerLevel level, BossLairPiece piece, ChunkPos chunk, BlockPos center) {
        var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(21), clip, chunk, center);
    }

    private static List<Mob> residents(ServerLevel level, BoundingBox box) {
        return level.getEntitiesOfClass(Mob.class, AABB.of(box), StructureSpawnedMobData::isMarked);
    }

    private static Map<String, Long> roster(List<Mob> mobs) {
        Map<String, Long> result = new TreeMap<>();
        for (var mob : mobs) result.merge(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString(), 1L, Long::sum);
        return result;
    }

    private static long fingerprint(ServerLevel level, BoundingBox box) {
        long hash = 0xcbf29ce484222325L;
        for (BlockPos cursor : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var state = level.getBlockState(cursor);
            hash = (hash ^ (state.isAir() ? 0 : Block.getId(state))) * 0x100000001b3L;
        }
        return hash;
    }

    private static long neighborRingFingerprint(ServerLevel level, ChunkPos chunk, BoundingBox box) {
        long hash = 0xcbf29ce484222325L;
        for (int x = chunk.getMinBlockX() - 1; x <= chunk.getMaxBlockX() + 1; x++) {
            for (int z = chunk.getMinBlockZ() - 1; z <= chunk.getMaxBlockZ() + 1; z++) {
                if (x >= chunk.getMinBlockX() && x <= chunk.getMaxBlockX()
                        && z >= chunk.getMinBlockZ() && z <= chunk.getMaxBlockZ()) continue;
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    var state = level.getBlockState(new BlockPos(x, y, z));
                    hash = (hash ^ (state.isAir() ? 0 : Block.getId(state))) * 0x100000001b3L;
                }
            }
        }
        return hash;
    }

    private static boolean canStand(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty();
    }

    private static Map<BlockPos, Integer> walk(ServerLevel level, BoundingBox box, BlockPos start) {
        Map<BlockPos, Integer> visited = new HashMap<>();
        var queue = new ArrayDeque<BlockPos>();
        if (!canStand(level, start)) return visited;
        visited.put(start, 0); queue.add(start);
        while (!queue.isEmpty()) {
            var from = queue.remove();
            for (var direction : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                var to = from.relative(direction).above(dy);
                if (!box.isInside(to) || visited.containsKey(to) || !canStand(level, to)) continue;
                var clearance = dy > 0 ? from.above(2) : dy < 0 ? to.above(2) : null;
                if (clearance != null && !level.getBlockState(clearance).getCollisionShape(level, clearance).isEmpty()) continue;
                visited.put(to, visited.get(from) + 1); queue.add(to);
            }
        }
        return visited;
    }

    private static void export(ServerLevel level, BlockPos center, BoundingBox box, BranchedBossCaves layout,
                               Map<BlockPos, Integer> walking, Map<String, Long> roster, Map<Block, Integer> palette) throws Exception {
        Files.createDirectories(REPORTS);
        var views = new ArrayList<Map<String, Object>>();
        views.add(view("spider-entrance", level, center.offset(55, 2, 55), center.offset(44, 2, 44)));
        for (var room : SpiderCaveLayout.chambers()) {
            var target = center.offset(room.center()).above(2);
            views.add(view("spider-" + room.theme().name().toLowerCase(java.util.Locale.ROOT), level,
                    target.offset(0, 2, room.radius() - 4), target));
        }
        var gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(REPORTS.resolve("spider-nov21-views.json"), gson.toJson(views));
        var materials = new TreeMap<String, Integer>();
        palette.forEach((block, count) -> materials.put(ForgeRegistries.BLOCKS.getKey(block).toString(), count));
        Files.writeString(REPORTS.resolve("spider-nov21-metrics.json"), gson.toJson(Map.of(
                "floorTiles", floorCount(layout), "reachableWalkingCells", walking.size(), "roster", roster, "materials", materials)));
        var image = new BufferedImage(774, 834, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(0x152020)); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int x = -64; x <= 64; x++) for (int z = -64; z <= 64; z++) {
            var c = layout.column(x, z);
            if (c == null || !c.hasAir()) continue;
            int color = Color.HSBtoRGB(.23f + (-c.floor()) * .006f, .48f, .83f);
            if (!walking.containsKey(center.offset(x, c.floor() + 1, z))) color = 0x5f5250;
            graphics.setColor(new Color(color)); graphics.fillRect((x + 64) * 6, (z + 64) * 6, 6, 6);
        }
        graphics.setFont(new Font("SansSerif", Font.BOLD, 15));
        for (var room : SpiderCaveLayout.chambers()) {
            graphics.setColor(Color.WHITE);
            graphics.drawString(room.theme().name(), (room.center().getX() + 64) * 6 - 25,
                    (room.center().getZ() + 64) * 6);
        }
        graphics.drawString("Expanded spider cave: " + floorCount(layout) + " floor tiles, " + roster.values().stream().mapToLong(Long::longValue).sum()
                + " placed residents", 18, 800);
        graphics.dispose();
        ImageIO.write(image, "png", REPORTS.resolve("spider-nov21-plan.png").toFile());
    }

    private static Map<String, Object> view(String label, ServerLevel level, BlockPos camera, BlockPos target) {
        double dx = target.getX() - camera.getX(), dz = target.getZ() - camera.getZ();
        double dy = target.getY() + .5 - (camera.getY() + 1.62);
        return Map.of("label", label, "dimension", level.dimension().location().toString(), "x", camera.getX() + .5,
                "y", camera.getY(), "z", camera.getZ() + .5, "yaw", (float) Math.toDegrees(Math.atan2(-dx, dz)),
                "pitch", (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
    }
}
