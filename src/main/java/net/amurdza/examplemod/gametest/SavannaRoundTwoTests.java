package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaCampSpawnRules;
import net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampProcessor;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.amurdza.examplemod.worldgen.structure.LegacySavannaOrcVillageV5;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaRoundTwoTests {
    private SavannaRoundTwoTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void expandedTemplateHasCompleteTentsPensAndStockade(GameTestHelper helper) {
        var level = level(helper);
        var template = level.getStructureManager().getOrCreate(new ResourceLocation("aoemod:orc_village_v5"));
        var blocks = blocks(level, new ResourceLocation("aoemod:orc_village_v5"));
        helper.assertTrue(template.getSize().getX() == 125 && template.getSize().getZ() == 125
                        && LegacySavannaOrcVillageV5.CENTER == 62 && LegacySavannaOrcVillageV5.RADIUS == 60,
                "The archived version-five camp must have a125-square template and radius60");
        for (int x = 0; x < 125; x++) for (int z = 0; z < 125; z++) {
            long dx = Math.abs(x - 62), dz = Math.abs(z - 62);
            helper.assertTrue(LegacySavannaOrcVillageV5.contains(x, z) == (dx * dx + dz * dz <= 3600),
                    "The archived version-five camp must use the requested radius60 circle");
        }
        for (int distant : new int[]{6_000_000, -6_000_000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            helper.assertFalse(LegacySavannaOrcVillageV5.contains(distant, distant)
                            || net.amurdza.examplemod.worldgen.structure.LegacySavannaOrcVillageV3.contains(distant, distant),
                    "Distant coordinates must not overflow cubic footprint arithmetic into the camp");
        }
        int totalBeds = 0, chests = 0, posts = 0;
        for (int cx : new int[]{41, 62, 83}) for (int cz : new int[]{41, 62, 83}) {
            int beds = 0, tentChests = 0;
            for (var entry : blocks.entrySet()) {
                BlockPos p = entry.getKey(); BlockState s = entry.getValue();
                if (Math.abs(p.getX() - cx) > 5 || Math.abs(p.getZ() - cz) > 5) continue;
                if (s.is(Blocks.CHEST)) tentChests++;
                if (id(s).equals("minecraft:straw_bed")) {
                    helper.assertTrue(state(blocks, p.below()).is(Blocks.HAY_BLOCK), "Every straw bed half needs its hay floor");
                    beds++;
                }
            }
            helper.assertTrue(beds == 16 && tentChests == 4, "Each tent must retain eight complete beds and four chests");
            totalBeds += beds / 2; chests += tentChests;
        }
        helper.assertTrue(totalBeds == 72 && chests == 36, "The nine tents require72beds and36chests");
        int crowned = 0;
        for (var entry : blocks.entrySet()) if (entry.getValue().is(Blocks.ACACIA_LOG)) {
            BlockPos p = entry.getKey();
            if (p.getY() != 3 || Math.abs(p.getX() - 62) <= 7 || Math.abs(p.getZ() - 62) <= 7) continue;
            int logs = 0, palisades = 0;
            while (state(blocks, p.above(logs)).is(Blocks.ACACIA_LOG)) logs++;
            while (state(blocks, p.above(logs + palisades)).is(ModBlocks.ACACIA_PALISADE.get())) palisades++;
            helper.assertTrue(logs >= 4 && logs <= 5 && palisades >= 1 && palisades <= 2
                            && state(blocks, p.above(logs + palisades)).is(Blocks.ACACIA_FENCE),
                    "Posts must reproduce native Orcz log, palisade and fence stacks: " + p);
            if (state(blocks, p.above(logs + palisades + 1)).is(Blocks.SKELETON_SKULL)) crowned++;
            posts++;
        }
        helper.assertTrue(posts >= 75 && crowned >= 5 && crowned < posts / 3,
                "Frequent native-height log posts need occasional skulls, rather than sparse identical short pillars");
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) for (boolean swap : new boolean[]{false, true}) {
            int interior = 0, water = 0, hay = 0;
            for (int b = 10; b <= 26; b++) {
                int back = 43;
                while (back <= 60) {
                    BlockState at = state(blocks, penPoint(sx, sz, swap, back, b));
                    if (at.is(ModBlocks.ACACIA_PALISADE.get()) || at.is(Blocks.ACACIA_LOG)) break;
                    back++;
                }
                helper.assertTrue(back <= 60, "Every pen row must terminate at the circular stockade");
                if (b == 10 || b == 26) helper.assertTrue(state(blocks, penPoint(sx, sz, swap, back, b)).is(Blocks.ACACIA_LOG),
                        "Pen side fences must meet acacia log posts, never palisade blocks");
                for (int a = 42; a < back; a++) {
                    BlockPos p = penPoint(sx, sz, swap, a, b);
                    BlockState s = state(blocks, p);
                    if (a == 42 || b == 10 || b == 26) helper.assertTrue(s.is(Blocks.ACACIA_FENCE) || s.is(Blocks.ACACIA_FENCE_GATE),
                            "Each pen requires its three inner fence sides");
                    else {
                        interior++;
                        helper.assertFalse(stockade(s), "No duplicate outer fence may consume the curved pen interior: " + p);
                    }
                    if (state(blocks, p.below()).is(Blocks.WATER)) {
                        water++;
                        helper.assertTrue(a >= 53 && a <= 55 && b >= 18 && b <= 20, "Pen ponds belong beside the back curve");
                    }
                    if (s.is(Blocks.HAY_BLOCK)) {
                        hay++;
                        helper.assertTrue((a == 46 || a == 47) && (b == 22 || b == 23), "Each hay arrangement must be2x2");
                    }
                }
            }
            helper.assertTrue(interior == 198 && water == 9 && hay == 4, "Every curved pen needs at least195interiorcells, a3x3pond and2x2hay");
        }
        // Temporarily seal only the four intentional gates, then flood diagonally
        // from outside. A one-cell diagonal stockade gap must not reach the streets.
        Set<BlockPos> reached = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(new BlockPos(0, 3, 0));
        while (!queue.isEmpty()) {
            BlockPos p = queue.removeFirst();
            if (p.getX() < 0 || p.getZ() < 0 || p.getX() >= 125 || p.getZ() >= 125 || !reached.add(p)) continue;
            int dx = Math.abs(p.getX() - 62), dz = Math.abs(p.getZ() - 62);
            if (stockade(state(blocks, p)) || Math.max(dx, dz) >= 55 && Math.min(dx, dz) <= 7) continue;
            helper.assertFalse(dx <= 32 && dz <= 32, "The stockade has an unintended route from outside into the camp");
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0) queue.add(p.offset(x, 0, z));
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void authoredGardensAndVegetationSurviveTheirFinalPlacementOrder(GameTestHelper helper) {
        var blocks = blocks(level(helper), new ResourceLocation("aoemod:orc_village_v5"));
        int podzol = 0, farmland = 0, pumpkins = 0, bushesInside = 0, bushesOutside = 0;
        Set<String> crops = new HashSet<>();
        for (var entry : blocks.entrySet()) {
            BlockPos p = entry.getKey(); BlockState s = entry.getValue();
            if (s.is(Blocks.TORCH)) helper.assertTrue(state(blocks, p.below()).is(Blocks.PUMPKIN), "Restored torches belong above garden pumpkins");
            if (s.is(Blocks.PODZOL)) { podzol++; helper.assertFalse(state(blocks, p.above()).is(Blocks.GRASS) || state(blocks, p.above()).is(Blocks.TALL_GRASS), "Podzol must not grow grass"); }
            helper.assertFalse(s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS), "Lawn vegetation must come from the native feature pass after mound placement");
            if (s.is(Blocks.PUMPKIN)) {
                pumpkins++;
                helper.assertTrue(state(blocks, p.above()).is(Blocks.TORCH), "Every authored pumpkin needs its restored torch");
            }
            if (s.is(Blocks.ACACIA_LEAVES)) {
                int x = p.getX() - 62, z = p.getZ() - 62;
                helper.assertTrue(p.getY() <= 5 && x * x + z * z >= 54 * 54,
                        "Only small native wall-side bushes may be authored, never synthetic canopy trees");
                if (p.getY() == 3) {
                    helper.assertTrue(state(blocks, p.below()).is(Blocks.GRASS_BLOCK), "Every wall bush needs a flat supported floor");
                    if (x * x + z * z <= 3600) bushesInside++; else bushesOutside++;
                }
            }
            if (s.is(Blocks.FARMLAND)) farmland++;
            if (p.getY() >= 3 && Math.abs(p.getX() - 62) <= 17 && Math.abs(p.getZ() - 62) <= 17) crops.add(id(s));
        }
        helper.assertTrue(podzol == 196 && farmland == 694 && pumpkins == 32 && bushesInside >= 20 && bushesOutside >= 20,
                "Preserve both authored gardens, four7x7patches, pumpkin torches and bushes on both sides of the stockade");
        helper.assertTrue(crops.containsAll(Set.of("hearthandharvest:corn_stalk", "minecraft:pumpkin", "minecraft:sugar_cane",
                        "minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots", "farmersdelight:cabbages")),
                "Both full spawn-village crop patterns must survive instead of four generic strips");
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            int red = 0, brown = 0;
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                BlockPos floor = new BlockPos(62 + sx * 34 + dx, 2, 62 + sz * 34 + dz);
                helper.assertTrue(state(blocks, floor).is(Blocks.PODZOL), "Each mushroom patch must be a complete7x7square");
                if (state(blocks, floor.above()).is(Blocks.RED_MUSHROOM)) red++;
                if (state(blocks, floor.above()).is(Blocks.BROWN_MUSHROOM)) brown++;
            }
            helper.assertTrue(red == 5 && brown == 5, "Every patch needs five red and five brown mushrooms on distinct cells");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void darkCampExcludesAmbientUndeadAndLivestockButKeepsAuthoredResidents(GameTestHelper helper) {
        ServerLevel level = level(helper);
        BlockPos center = new BlockPos(18688, 95, 18688);
        PlacedOrcVillages.get(level).record(center);
        for (EntityType<?> type : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK,
                EntityType.PHANTOM, EntityType.WITHER, ModEntities.PARCHED.get(), ModEntities.CAMEL_HUSK_JOCKEY.get(),
                EntityType.LLAMA, EntityType.SHEEP, EntityType.COW, EntityType.PIG}) {
            Mob mob = (Mob) type.create(level);
            mob.moveTo(center, 0, 0);
            helper.assertTrue(SavannaCampSpawnRules.blocked(level, center, mob), "The unlit camp must exclude ambient undead/livestock: " + type);
            var natural = new MobSpawnEvent.PositionCheck(mob, level, MobSpawnType.NATURAL, null);
            DocumentedNaturalSpawns.position(natural);
            helper.assertTrue(natural.getResult() == Event.Result.DENY, "The real final spawn gate must deny ambient camp entrants");
            var authored = new MobSpawnEvent.PositionCheck(mob, level, MobSpawnType.STRUCTURE, null);
            DocumentedNaturalSpawns.position(authored);
            helper.assertTrue(authored.getResult() == Event.Result.DEFAULT, "The camp's own four-per-pen STRUCTURE residents must remain permitted");
            helper.assertFalse(SavannaCampSpawnRules.blocked(level, center.offset(SavannaOrcVillage.RADIUS + 1, 0, 0), mob), "Outside wildlife must not be denied by the camp boundary");
            mob.discard();
        }
        // Model a modded mob whose undead family is not encoded in its registry
        // name. The real entity's MobType must win over the cheap name filter.
        var unnamedUndead = new net.minecraft.world.entity.monster.Spider(EntityType.SPIDER, level) {
            @Override public net.minecraft.world.entity.MobType getMobType() { return net.minecraft.world.entity.MobType.UNDEAD; }
        };
        unnamedUndead.moveTo(center, 0, 0);
        helper.assertFalse(SavannaCampSpawnRules.blocked(level, center, unnamedUndead.getType()),
                "This fixture must bypass the name-based undead optimization");
        helper.assertTrue(SavannaCampSpawnRules.blocked(level, center, unnamedUndead),
                "Modded undead with unrelated registry names must still be forbidden by their real mob family");
        unnamedUndead.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void allEightEntranceCellsDescendTogetherWithoutCenterSpines(GameTestHelper helper) {
        SavannaRoundThreeTests.assertEightLanePlacement(helper);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void oldVersionThreePiecesKeepTheirOriginalFootprintAndPopulationBits(GameTestHelper helper) {
        ServerLevel level = level(helper);
        var context = StructurePieceSerializationContext.fromLevel(level);
        for (int version : new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11}) {
            var piece = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(256, 100, 256));
            var saved = piece.createTag(context);
            saved.putInt("SettlementVersion", version);
            saved.putString("Template", "aoemod:orc_village");
            BitSet animals = new BitSet(); animals.set(0, version == 3 ? 16 : version >= 9 ? 64 : 32);
            BitSet cushions = new BitSet(); cushions.set(0, version >= 9 ? 128 : 72);
            BitSet residents = new BitSet(); residents.set(0, version >= 11 ? 127 : version >= 9 ? 64 : 36);
            saved.putLongArray("VillageAnimals", animals.toLongArray());
            saved.putLongArray("VillageCushions", cushions.toLongArray());
            saved.putLongArray("VillageResidents", residents.toLongArray());
            var restored = new SavannaOrcCampPiece(context, saved);
            int radius = version == 3 ? 44 : version < 6 ? 60 : version == 6 ? 94 : version == 7 ? 105 : version == 8 ? 98 : 96;
            int offset = version == 3 ? 46 : version < 6 ? 62 : version == 6 ? 96 : version == 7 ? 107 : version == 8 ? 100 : 98;
            int size = version == 3 ? 93 : version < 6 ? 125 : version == 6 ? 193 : version == 7 ? 215 : version == 8 ? 201 : 197;
            int span = version == 3 ? 125 : version < 6 ? 165 : version == 6 ? 233 : version == 7 ? 255 : version == 8 ? 241 : 237;
            helper.assertTrue(restored.template().getSize().getX() == size && restored.getBoundingBox().getXSpan() == span
                            && restored.isCircularVillage() == (version == 5) && restored.isSquareVillage() == (version >= 6)
                            && restored.isRevisedSquareVillage() == (version >= 9)
                            && restored.isShallowGateVillage() == (version >= 10)
                            && restored.isDefendedVillage() == (version >= 11)
                            && (restored.encounterId() != null) == (version >= 11),
                    "Saved camp version " + version + " must retain its own template dimensions, ramp bounds and outline");
            BlockPos center = restored.templatePosition().offset(offset, 0, offset);
            for (int x = -radius - 1; x <= radius + 1; x++) for (int z = -radius - 1; z <= radius + 1; z++) {
                long ax = Math.abs((long) x), az = Math.abs((long) z), r = radius;
                boolean expected = version >= 6 ? ax <= r && az <= r
                        : version == 5 ? ax * ax + az * az <= r * r : ax * ax * ax + az * az * az <= r * r * r;
                helper.assertTrue(restored.containsSurfaceColumn(center.offset(x, 0, z)) == expected,
                        "Versions three/four stay cubic, version five stays circular, and versions six through eleven stay square");
            }
            var again = restored.createTag(context);
            String template = version >= 11 ? "aoemod:orc_village" : "aoemod:orc_village_v" + version;
            helper.assertTrue(again.getInt("SettlementVersion") == version && again.getString("Template").equals(template)
                            && BitSet.valueOf(again.getLongArray("VillageAnimals")).equals(animals)
                            && BitSet.valueOf(again.getLongArray("VillageCushions")).equals(cushions)
                            && BitSet.valueOf(again.getLongArray("VillageResidents")).equals(residents),
                    "Reloading must preserve the archived template and all population bits without upgrading the camp");
            var second = new SavannaOrcCampPiece(context, again);
            helper.assertTrue(second.createTag(context).equals(again), "A second reload must leave versioned camp metadata unchanged");
            if (version >= 11) helper.assertTrue(restored.encounterId().equals(second.encounterId())
                            && restored.encounterId().equals(piece.encounterId()),
                    "Version-eleven placement retries and progression must retain their saved encounter identity");
        }
        var origins = new CompoundTag(); origins.putLongArray("Centers", new long[]{new BlockPos(500, 100, 500).asLong()});
        var oldRecords = PlacedOrcVillages.load(origins);
        helper.assertTrue(oldRecords.contains(new BlockPos(544, 100, 500)) && !oldRecords.contains(new BlockPos(545, 100, 500))
                        && oldRecords.contains(new BlockPos(533, 100, 533)),
                "Old center-only command records must retain their radius-forty-four cubic outline");
        var oldVillage = new CompoundTag(); oldVillage.putLong("Center", new BlockPos(1000, 100, 1000).asLong()); oldVillage.putInt("Radius", 60);
        var entries = new net.minecraft.nbt.ListTag(); entries.add(oldVillage);
        var oldTag = new CompoundTag(); oldTag.put("Villages", entries);
        var records = PlacedOrcVillages.load(oldTag);
        helper.assertTrue(records.contains(new BlockPos(1045, 100, 1045)), "A v4 radius record without Circular must remain cubic");
        records.record(new BlockPos(1500, 100, 1500), 60, true);
        records.record(new BlockPos(2000, 100, 2000));
        var reloaded = PlacedOrcVillages.load(records.save(new CompoundTag()));
        helper.assertTrue(reloaded.contains(new BlockPos(1045, 100, 1045))
                        && !reloaded.contains(new BlockPos(1545, 100, 1545)) && reloaded.contains(new BlockPos(1560, 100, 1500))
                        && reloaded.contains(new BlockPos(2000 + SavannaOrcVillage.RADIUS, 100, 2000 + SavannaOrcVillage.RADIUS)) && !reloaded.contains(new BlockPos(2001 + SavannaOrcVillage.RADIUS, 100, 2000)),
                "Saved shape metadata must distinguish legacy cubic/circular and fresh square command camps");
        helper.assertFalse(oldRecords.contains(new BlockPos(6_000_000, 100, 6_000_000)),
                "Distant coordinates must not be excluded by overflow in saved camp records");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void clippedProcessingKeepsTransformedTemplateNeighborContext(GameTestHelper helper) {
        ServerLevel level = level(helper);
        WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    // No placed terrain may mask a missing source-template neighbor.
                    if (method.getName().equals("getBlockState")) return Blocks.AIR.defaultBlockState();
                    if (method.getName().equals("hasChunkAt")) return true;
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        BlockPos bush = new BlockPos(1, 1, 1), seat = new BlockPos(7, 1, 1), ground = new BlockPos(17, 0, 1);
        List<StructureTemplate.StructureBlockInfo> source = new ArrayList<>();
        source.add(new StructureTemplate.StructureBlockInfo(bush, Blocks.OAK_LEAVES.defaultBlockState(), null));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            source.add(new StructureTemplate.StructureBlockInfo(bush.relative(direction), Blocks.DIRT.defaultBlockState(), null));
        }
        source.add(new StructureTemplate.StructureBlockInfo(seat, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), null));
        source.add(new StructureTemplate.StructureBlockInfo(seat.east(4), Blocks.CAMPFIRE.defaultBlockState(), null));
        for (int y = 0; y <= 5; y++) source.add(new StructureTemplate.StructureBlockInfo(
                ground.above(y), Blocks.DIRT.defaultBlockState(), null));
        source.add(new StructureTemplate.StructureBlockInfo(new BlockPos(30, 1, 1), Blocks.STONE.defaultBlockState(), null));
        BlockPos origin = new BlockPos(37, 100, -21), pivot = new BlockPos(3, 0, 2);
        for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
            var settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror)
                    .setRotationPivot(pivot).addProcessor(SavannaOrcCampProcessor.INSTANCE);
            var full = StructureTemplate.processBlockInfos(world, origin, pivot, settings, source);
            for (BlockPos target : List.of(bush, seat, ground)) {
                BlockPos placed = StructureTemplate.calculateRelativePosition(settings, target).offset(origin);
                var clip = new BoundingBox(placed.getX(), placed.getY(), placed.getZ(),
                        placed.getX(), placed.getY(), placed.getZ());
                var clipped = StructureTemplate.processBlockInfos(world, origin, pivot,
                        settings.copy().setBoundingBox(clip), source);
                BlockState expected = full.stream().filter(info -> info.pos().equals(placed)).findFirst().orElseThrow().state();
                helper.assertTrue(expected.is(target.equals(bush) ? Blocks.GRASS_BLOCK : target.equals(seat) ? Blocks.AIR : Blocks.MUD),
                        "The full source must convert an enclosed bush, remove a fire-side cushion and retain full ground-column depth");
                var actual = clipped.stream().filter(info -> info.pos().equals(placed)).findFirst().orElseThrow();
                helper.assertTrue(actual.state().equals(expected),
                        "Chunk clipping must retain source neighbors for " + target + " under " + rotation + "/" + mirror
                                + ": expected " + expected + ", got " + actual.state());
                helper.assertTrue(clipped.stream().allMatch(info -> Math.abs(info.pos().getX() - placed.getX()) <= 4
                                && Math.abs(info.pos().getZ() - placed.getZ()) <= 4),
                        "Processing must discard blocks beyond the four-block context halo");
                helper.assertTrue(clipped.size() < full.size(), "Clipped processing must reduce the intermediate block list");
            }
        }
        helper.succeed();
    }

    static Map<BlockPos, BlockState> blocks(ServerLevel level, ResourceLocation id) {
        CompoundTag nbt = level.getStructureManager().getOrCreate(id).save(new CompoundTag());
        var palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, BlockState> result = new HashMap<>();
        for (Tag tag : nbt.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) tag; var p = block.getList("pos", Tag.TAG_INT);
            result.put(new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2)), NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state"))));
        }
        return result;
    }
    private static BlockPos penPoint(int sx, int sz, boolean swap, int a, int b) {
        return new BlockPos(62 + sx * (swap ? b : a), 3, 62 + sz * (swap ? a : b));
    }
    private static String id(BlockState state) { return ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(); }
    private static boolean stockade(BlockState state) {
        return state.is(ModBlocks.ACACIA_PALISADE.get()) || state.is(Blocks.ACACIA_LOG) || state.is(Blocks.ACACIA_FENCE) || state.is(Blocks.ACACIA_FENCE_GATE);
    }
    private static BlockState state(Map<BlockPos, BlockState> states, BlockPos pos) { return states.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
    private static ServerLevel level(GameTestHelper helper) { return helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA); }
}
