package net.amurdza.examplemod.gametest;

import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.amurdza.examplemod.worldgen.ShoreSugarCane;
import net.amurdza.examplemod.worldgen.structure.OrcCampStock;
import net.amurdza.examplemod.worldgen.structure.OrcVillageLoot;
import net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampProcessor;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaDocumentRevisionTests {
    private static final String[] ANIMALS = {"alexsmobs:bison", "minecraft:sheep", "alexsmobs:gazelle", "biodiverse:oryx",
            "alexsmobs:emu", "alexsmobs:kangaroo", "twilightforest:boar", "biodiverse:wildebeest"};

    private SavannaDocumentRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void sixteenTentsRetainCropAreaWithSixWideRoadsAndEnclosedPens(GameTestHelper helper) {
        var level = savanna(helper);
        var piece = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(0, 200, 0));
        helper.assertTrue(piece.template().getSize().getX() == 197
                        && piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getInt("SettlementVersion") == 11,
                "New camps must select the version-eleven 197-block template");
        var blocks = templateBlocks(level, SavannaOrcCampPiece.TEMPLATE);
        helper.assertTrue(blocks.values().stream().filter(s -> s.is(Blocks.CHEST)).count() == 64
                        && blocks.values().stream().filter(s -> s.is(Blocks.RED_BED)).count() == 256,
                "All sixteen tents need four chests and eight complete beds");
        helper.assertTrue(blocks.values().stream().noneMatch(s -> s.is(Blocks.PODZOL)),
                "New villages must have grass under their mushrooms and no authored podzol anywhere");
        assertGeometry(helper, p -> blocks.getOrDefault(p, defaultState(p)));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void defendedCampRosterAndIdentitySurvivePartialReload(GameTestHelper helper) {
        var level = savanna(helper);
        var origin = new BlockPos(0, 200, 0);
        var context = StructurePieceSerializationContext.fromLevel(level);
        var piece = new SavannaOrcCampPiece(level.getStructureManager(), origin);
        var roster = SavannaOrcVillage.residentPlan();
        helper.assertTrue(roster.size() == 127 && roster.stream().map(SavannaOrcVillage.Resident::index).distinct().count() == 127,
                "The defender roster must contain exactly 127 stable, distinct placement indices");
        var counts = new HashMap<String, Integer>();
        var forests = new HashMap<String, Integer>();
        var huts = new HashMap<String, Integer>();
        for (var resident : roster) {
            counts.merge(resident.type(), 1, Integer::sum);
            var pos = resident.local();
            int x = pos.getX() - SavannaOrcVillage.CENTER, z = pos.getZ() - SavannaOrcVillage.CENTER;
            helper.assertTrue(pos.getY() == 3, "Every resident must begin at the authored floor, never a roof or canopy");
            if (resident.forest()) {
                helper.assertTrue(Math.min(Math.abs(x), Math.abs(z)) > 31 && Math.max(Math.abs(x), Math.abs(z)) >= 59,
                        "Outdoor residents must stay in the forest corners, beyond crops and pens");
                forests.merge(Integer.signum(x) + ":" + Integer.signum(z), 1, Integer::sum);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                    helper.assertTrue(SavannaCampVegetation.reservedLocal(pos.getX() + dx, pos.getZ() + dz, true),
                            "Native tree trunks and leaves must preserve each resident's complete clearance");
            } else {
                huts.merge(Math.floorDiv(x + 33, 18) + ":" + Math.floorDiv(z + 33, 18), 1, Integer::sum);
            }
        }
        helper.assertTrue(counts.equals(Map.of("orcz:bigorcboss", 1, "orcz:orcshield", 63, "orcz:orcsavagemace", 63)),
                "The roster must contain one boss and exactly 63 of each guard species");
        helper.assertTrue(forests.size() == 4 && forests.values().stream().allMatch(count -> count == 16)
                        && huts.size() == 16 && huts.get("1:1") == 3
                        && huts.entrySet().stream().allMatch(entry -> entry.getValue() == (entry.getKey().equals("1:1") ? 3 : 4)),
                "The boss hut holds three residents, other huts four, and each forest corner sixteen");
        var tag = piece.createTag(context);
        var partial = new BitSet(); partial.set(0); partial.set(20); partial.set(63); partial.set(126);
        tag.putLongArray("VillageResidents", partial.toLongArray());
        var reloaded = new SavannaOrcCampPiece(context, tag);
        var saved = reloaded.createTag(context);
        helper.assertTrue(reloaded.isDefendedVillage() && reloaded.encounterId().equals(piece.encounterId())
                        && saved.getUUID("CampEncounterId").equals(piece.encounterId())
                        && BitSet.valueOf(saved.getLongArray("VillageResidents")).equals(partial),
                "Partial chunk saves must preserve the encounter UUID and defender indices across both BitSet words");
        helper.assertFalse(new SavannaOrcCampPiece(level.getStructureManager(), origin).encounterId().equals(piece.encounterId()),
                "An explicitly replaced camp must receive a fresh encounter identity");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedVersionTenKeepsItsShallowGateAndOriginalPopulation(GameTestHelper helper) {
        var level = savanna(helper);
        var context = StructurePieceSerializationContext.fromLevel(level);
        var tag = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(0, 200, 0)).createTag(context);
        tag.putInt("SettlementVersion", 10);
        tag.remove("CampEncounterId");
        tag.putLongArray("VillageResidents", new long[]{-1L});
        tag.putLongArray("VillageAnimals", new long[]{-1L});
        tag.putLongArray("VillageCushions", new long[]{-1L, -1L});
        var old = new SavannaOrcCampPiece(context, tag);
        var saved = old.createTag(context);
        helper.assertTrue(saved.getInt("SettlementVersion") == 10 && saved.getString("Template").equals("aoemod:orc_village_v10")
                        && old.isShallowGateVillage() && !old.isDefendedVillage() && old.encounterId() == null,
                "V10 pieces must keep their archived template and must not join the new 127-defender system");
        var blocks = templateBlocks(level, new ResourceLocation("aoemod:orc_village_v10"));
        for (int direction = 0; direction < 4; direction++) {
            helper.assertTrue(blocks.get(gate(direction, 0, 7, 0)).is(Blocks.ACACIA_LOG)
                            && blocks.getOrDefault(gate(direction, 0, 7, 1), Blocks.AIR.defaultBlockState()).isAir(),
                    "V10 must retain its four-high, one-deep original gate");
            helper.assertFalse(SavannaCampVegetation.reserved(old, old.templatePosition().offset(gate(direction, -4, 3, 1)), true),
                    "V10 must retain its old free inner gate row in the planting mask");
        }
        helper.assertTrue(BitSet.valueOf(saved.getLongArray("VillageResidents")).cardinality() == 64
                        && new SavannaOrcCampPiece(context, saved).createTag(context).equals(saved),
                "V10 must keep its 64 completed residents and stable serialization instead of adding 63 defenders");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedVersionNineKeepsItsDeepGatesNarrowPenGatesAndPodzol(GameTestHelper helper) {
        var level = savanna(helper);
        var context = StructurePieceSerializationContext.fromLevel(level);
        var tag = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(0, 200, 0)).createTag(context);
        tag.putInt("SettlementVersion", 9);
        tag.putLongArray("VillageResidents", new long[]{-1L});
        tag.putLongArray("VillageCushions", new long[]{-1L, -1L});
        tag.putLongArray("VillageAnimals", new long[]{-1L});
        var old = new SavannaOrcCampPiece(context, tag);
        var saved = old.createTag(context);
        helper.assertTrue(saved.getString("Template").equals("aoemod:orc_village_v9") && saved.getInt("SettlementVersion") == 9
                        && old.isRevisedSquareVillage() && !old.isShallowGateVillage()
                        && old.template().getSize().getX() == 197 && old.getBoundingBox().getXSpan() == 237,
                "Saved V9 pieces must retain their archived template, geometry and version");
        var blocks = templateBlocks(level, new ResourceLocation("aoemod:orc_village_v9"));
        for (int direction = 0; direction < 4; direction++) for (int depth = 0; depth < 2; depth++) {
            helper.assertTrue(blocks.getOrDefault(gate(direction, -4, 3, depth), Blocks.AIR.defaultBlockState()).is(Blocks.ACACIA_LOG)
                            && blocks.getOrDefault(gate(direction, 0, 7, depth), Blocks.AIR.defaultBlockState()).is(Blocks.ACACIA_LOG),
                    "Archived V9 gates must keep both rows of their uprights and beam");
        }
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) for (boolean swap : new boolean[]{false, true}) {
            for (int b = 15; b <= 19; b++) {
                var pos = local(mirror(swap ? b : 59, sx), 3, mirror(swap ? 59 : b, sz));
                helper.assertTrue(blocks.get(pos).is(b >= 16 && b <= 18 ? Blocks.ACACIA_FENCE_GATE : Blocks.ACACIA_FENCE),
                        "Archived V9 pens must retain their original three-wide gates");
            }
        }
        for (var patch : SavannaOrcVillage.mushroomPatches(old.templatePosition())) for (var pos : patch) {
            helper.assertTrue(blocks.get(pos.below().subtract(old.templatePosition())).is(Blocks.PODZOL)
                            && SavannaCampVegetation.reserved(old, pos, false) && SavannaCampVegetation.reserved(old, pos, true),
                    "Archived V9 mushroom soil and both saved planting masks must remain intact");
        }
        for (String key : new String[]{"VillageResidents", "VillageCushions", "VillageAnimals"})
            helper.assertTrue(java.util.Arrays.equals(saved.getLongArray(key), tag.getLongArray(key)),
                    "V9 reload must preserve completed population bits: " + key);
        helper.assertTrue(new SavannaOrcCampPiece(context, saved).createTag(context).equals(saved),
                "Repeated V9 serialization must remain stable without an implicit upgrade");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedVersionEightKeepsItsGeometryMasksAndPopulationBits(GameTestHelper helper) {
        var level = savanna(helper);
        var context = StructurePieceSerializationContext.fromLevel(level);
        var tag = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(0, 200, 0)).createTag(context);
        tag.putInt("SettlementVersion", 8);
        tag.putInt("TPX", -100); tag.putInt("TPZ", -100);
        tag.putLongArray("VillageResidents", new long[]{0xFFFFFFFFFL});
        tag.putLongArray("VillageCushions", new long[]{-1L, 255L});
        tag.putLongArray("VillageAnimals", new long[]{0xFFFFFFFFL});
        var old = new SavannaOrcCampPiece(context, tag);
        var saved = old.createTag(context);
        helper.assertTrue(old.template().getSize().getX() == 201 && old.getBoundingBox().getXSpan() == 241
                        && saved.getString("Template").equals("aoemod:orc_village_v8") && saved.getInt("SettlementVersion") == 8,
                "Existing V8 pieces must retain their archived template and footprint");
        for (String key : new String[]{"VillageResidents", "VillageCushions", "VillageAnimals"})
            helper.assertTrue(java.util.Arrays.equals(saved.getLongArray(key), tag.getLongArray(key)),
                    "An archived camp must retain all completed population bits: " + key);
        helper.assertTrue(new SavannaOrcCampPiece(context, saved).createTag(context).equals(saved),
                "Archived V8 serialization must remain stable across repeated reloads");
        var oldRoad = old.templatePosition().offset(100 + 40, 2, 100);
        helper.assertTrue(SavannaCampVegetation.reserved(old, oldRoad, true),
                "Archived road columns must retain the archived vegetation reservation");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mushroomPatchesStayInSeparateChunksAtEveryOriginAlignment(GameTestHelper helper) {
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            var origin = new BlockPos(4096 + dx, 100, -4096 + dz);
            var patches = SavannaOrcVillage.mushroomPatches(origin);
            helper.assertTrue(patches.size() == 8 && patches.equals(SavannaOrcVillage.mushroomPatches(origin)),
                    "Each camp needs eight reproducible mushroom patches");
            var byCorner = new HashMap<String, List<ChunkPos>>();
            for (var patch : patches) {
                helper.assertTrue(patch.size() == 10 && patch.stream().distinct().count() == 10
                                && patch.stream().map(ChunkPos::new).distinct().count() == 1,
                        "Each five-red/five-brown patch must occupy ten distinct cells in one actual chunk");
                BlockPos first = patch.get(0).subtract(origin).offset(-SavannaOrcVillage.CENTER, 0, -SavannaOrcVillage.CENTER);
                String key = Integer.signum(first.getX()) + ":" + Integer.signum(first.getZ());
                byCorner.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new ChunkPos(patch.get(0)));
                for (var pos : patch) {
                    var local = pos.subtract(origin);
                    helper.assertTrue(SavannaCampVegetation.reservedLocal(local.getX(), local.getZ(), false)
                                    && SavannaCampVegetation.reservedLocal(local.getX(), local.getZ(), true),
                            "Groundcover and tree writes must both preserve authored mushroom patches");
                }
            }
            helper.assertTrue(byCorner.size() == 4 && byCorner.values().stream()
                            .allMatch(chunks -> chunks.size() == 2 && !chunks.get(0).equals(chunks.get(1))),
                    "Each forest corner needs its own pair of separate mushroom chunks");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sixLaneRampsStopBeforeTheFlatTerrainRowAcrossChunkClips(GameTestHelper helper) {
        var level = savanna(helper);
        var origin = new BlockPos(20483, 126, 20483);
        for (int drop : new int[]{0, 1, 4, 80}) {
            var plan = SavannaOrcVillage.planGround(128, (x, z) -> 128 - drop);
            var written = new HashMap<BlockPos, BlockState>();
            BoundingBox[] clip = {null};
            var world = (WorldGenLevel) Proxy.newProxyInstance(SavannaDocumentRevisionTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getBlockState")) return written.getOrDefault(args[0], Blocks.AIR.defaultBlockState());
                        if (method.getName().equals("getFluidState")) return written.getOrDefault(args[0], Blocks.AIR.defaultBlockState()).getFluidState();
                        if (method.getName().equals("setBlock")) {
                            var pos = ((BlockPos) args[0]).immutable();
                            helper.assertTrue(clip[0].isInside(pos), "Ramp writes must respect their chunk clip");
                            written.put(pos, (BlockState) args[1]); return true;
                        }
                        try { return method.invoke(level, args); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            for (int direction = 3; direction >= 0; direction--) for (int step = 20; step >= 0; step--)
                for (int across = -4; across <= 3; across++) {
                    int x = origin.getX() + SavannaOrcVillage.rampX(direction, step, across);
                    int z = origin.getZ() + SavannaOrcVillage.rampZ(direction, step, across);
                    clip[0] = new BoundingBox(x, level.getMinBuildHeight(), z, x, level.getMaxBuildHeight() - 1, z);
                    SavannaOrcVillage.placeGround(world, clip[0], origin, plan);
                    boolean path = written.entrySet().stream().anyMatch(e -> e.getKey().getX() == x
                            && e.getKey().getZ() == z && e.getValue().is(Blocks.DIRT_PATH));
                    boolean expected = across >= -3 && across <= 2 && (step == 0 || step < drop);
                    helper.assertTrue(path == expected,
                            "Six-lane ramp must omit its first flat row: drop=" + drop + ", step=" + step + ", lane=" + across);
                }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void unevenSixLaneRampsEndBeforeAFlatTreadAndKeepSupportedGrassTransitions(GameTestHelper helper) {
        var level = savanna(helper);
        var origin = new BlockPos(20735, 126, -20735);
        java.util.function.IntBinaryOperator terrain = (x, z) -> {
            int dx = x - SavannaOrcVillage.CENTER, dz = z - SavannaOrcVillage.CENTER;
            int across = Math.abs(dx) > Math.abs(dz) ? dz : dx;
            return across < 0 ? 124 : 126;
        };
        var plan = SavannaOrcVillage.planGround(128, terrain);
        var written = new HashMap<BlockPos, BlockState>();
        Function<BlockPos, BlockState> state = pos -> written.getOrDefault(pos,
                (pos.getY() <= terrain.applyAsInt(pos.getX() - origin.getX(), pos.getZ() - origin.getZ())
                        ? Blocks.GRASS_BLOCK : Blocks.AIR).defaultBlockState());
        BoundingBox[] clip = {null};
        var world = (WorldGenLevel) Proxy.newProxyInstance(SavannaDocumentRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBlockState")) return state.apply((BlockPos) args[0]);
                    if (method.getName().equals("getFluidState")) return state.apply((BlockPos) args[0]).getFluidState();
                    if (method.getName().equals("setBlock")) {
                        var pos = ((BlockPos) args[0]).immutable();
                        helper.assertTrue(clip[0].isInside(pos), "Uneven entrance writes must respect their column clip");
                        written.put(pos, (BlockState) args[1]); return true;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        // Reverse placement order exercises the same plan across separate clips.
        for (int direction = 3; direction >= 0; direction--) {
            helper.assertTrue(plan.length(direction) == 2,
                    "The former third tread would be flat at Y126 and must be omitted");
            for (int step = 3; step >= 0; step--) for (int across = -3; across <= 2; across++) {
                int x = origin.getX() + SavannaOrcVillage.rampX(direction, step, across);
                int z = origin.getZ() + SavannaOrcVillage.rampZ(direction, step, across);
                clip[0] = new BoundingBox(x, level.getMinBuildHeight(), z, x, level.getMaxBuildHeight() - 1, z);
                SavannaOrcVillage.placeGround(world, clip[0], origin, plan);
                int top = step < 3 ? 128 - step : across < 0 ? 125 : 126;
                BlockPos surface = new BlockPos(x, top, z);
                helper.assertTrue(state.apply(surface).is(step < 3 ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK),
                        "All six lanes must descend together, then meet a supported grass transition");
                helper.assertTrue(state.apply(surface.above()).isAir(), "The entrance route must stay clear");
                for (int depth = 1; depth <= 14; depth++) helper.assertFalse(state.apply(surface.below(depth)).isAir(),
                        "Each entrance and transition column must have a continuous foundation");
                if (step == 3) {
                    helper.assertTrue(126 - top >= 0 && 126 - top <= 1,
                            "The final descending tread must not drop two blocks into a low terrain lane");
                    helper.assertFalse(written.entrySet().stream().anyMatch(entry -> entry.getKey().getX() == x
                                    && entry.getKey().getZ() == z && entry.getValue().is(Blocks.DIRT_PATH)),
                            "No lane may retain the former flat final path row");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sixtyFourOpenedChestsKeepTheOldTotalLootAndLegacyShares(GameTestHelper helper) {
        var level = savanna(helper);
        var origin = new BlockPos(30400, 240, 30400);
        var table = level.getServer().getLootData().getLootTable(SavannaOrcCampProcessor.LOOT_TABLE);
        var data = new CompoundTag();
        data.putLong(OrcVillageLoot.SEED, 9418153); data.putLong(OrcVillageLoot.ORIGIN, origin.asLong());
        data.putInt(OrcCampStock.STOCK_TAG, 53);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                .withLuck(17).create(LootContextParamSets.CHEST);
        var budget = quantities(OrcVillageLoot.budget(table, params, data));
        for (int count : new int[]{36, 64}) {
            if (count == 64) data.putInt(OrcVillageLoot.COUNT, count);
            var opened = new ArrayList<ItemStack>();
            for (int index = count - 1; index >= 0; index--) {
                var pos = origin.east(index * 2);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 18);
                var chest = (ChestBlockEntity) level.getBlockEntity(pos);
                chest.getPersistentData().merge(data); chest.getPersistentData().putInt(OrcVillageLoot.INDEX, index);
                chest.setLootTable(SavannaOrcCampProcessor.LOOT_TABLE, index * 4729L);
                for (int slot = 0; slot < chest.getContainerSize(); slot++) opened.add(chest.getItem(slot).copy());
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            }
            helper.assertTrue(!budget.isEmpty() && quantities(opened).equals(budget),
                    "Opening all " + count + " chest shares backwards must retain exactly the old total loot budget");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void naturalCampKeepsSixtyFourAnimalsAndProtectedPondsAfterDecoration(GameTestHelper helper) throws Exception {
        var level = savanna(helper);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:orc_camp"));
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        for (int rx = 111; rx < 119; rx++) for (int rz = 113; rz < 121; rz++) {
            var owner = new ChunkPos(SavannaCampPlacement.findCenter(SavannaCampPlacement.terrainLand(random), rx, rz));
            var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                    level.getStructureManager(), level.getSeed(), owner, level, structure.biomes()::contains);
            if (structure.findValidGenerationPoint(context).isEmpty()) continue;
            var start = level.getChunk(owner.x, owner.z, ChunkStatus.STRUCTURE_STARTS, true).getStartForStructure(structure);
            helper.assertTrue(start != null && start.isValid() && start.getPieces().get(0) instanceof SavannaOrcCampPiece,
                    "The real registered structure must generate a native camp piece");
            var piece = (SavannaOrcCampPiece) start.getPieces().get(0);
            var bounds = piece.getBoundingBox();
            for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++)
                for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) level.getChunk(x, z);
            var origin = piece.templatePosition();
            // Force the shore feature at every pen pond, rather than relying
            // on its random natural rarity to happen during this worldgen run.
            var pondChunks = new java.util.HashSet<ChunkPos>();
            for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) for (boolean swap : new boolean[]{false, true})
                for (int a = 69; a <= 77; a++) for (int b = 13; b <= 21; b++)
                    pondChunks.add(new ChunkPos(origin.offset(local(mirror(swap ? b : a, sx), 3, mirror(swap ? a : b, sz)))));
            for (var chunk : pondChunks) helper.assertTrue(ShoreSugarCane.placePatchInChunk(level, chunk.x, chunk.z) == 0,
                    "Random shore patches must reject every penned pond, even when invoked explicitly");
            assertGeometry(helper, p -> level.getBlockState(origin.offset(p)));
            level.getServer().overworld().setDayTime(6000);
            for (var patch : SavannaOrcVillage.mushroomPatches(origin)) for (int i = 0; i < patch.size(); i++) {
                var pos = patch.get(i);
                var state = level.getBlockState(pos);
                var ground = level.getBlockState(pos.below());
                helper.assertTrue(state.is(i < 5 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM)
                                && ground.is(Blocks.GRASS_BLOCK),
                        "Every deterministic corner mushroom must survive native decoration on grass: " + pos);
                helper.assertTrue(state.canSurvive(level, pos)
                                && state.updateShape(Direction.DOWN, ground, level, pos, pos.below()).is(state.getBlock()),
                        "Mushrooms on grass must survive daylight and support-neighbor updates: " + pos);
            }
            var residents = level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked);
            var counts = new HashMap<String, Integer>();
            residents.forEach(mob -> counts.merge(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString(), 1, Integer::sum));
            for (String id : ANIMALS) helper.assertTrue(counts.getOrDefault(id, 0) == 8, "Each pen must have eight animals: " + id + "=" + counts);
            helper.assertTrue(residents.size() == 191 && counts.getOrDefault("orcz:bigorcboss", 0) == 1
                            && counts.getOrDefault("orcz:orcshield", 0) == 63 && counts.getOrDefault("orcz:orcsavagemace", 0) == 63,
                    "The huts and forests must contain exactly 127 orcs (one boss, 63 shield and 63 mace), plus 64 livestock");
            var cushions = level.getEntitiesOfClass(Cushion.class, AABB.of(bounds));
            helper.assertTrue(cushions.size() == 128 && cushions.stream().allMatch(Cushion::survives),
                    "Sixteen tents require 128 supported cushions");
            SavannaOrcCampGenerationTests.assertNoResidentCollisions(helper, level, residents);
            var serialization = StructurePieceSerializationContext.fromLevel(level);
            var saved = piece.createTag(serialization);
            helper.assertTrue(BitSet.valueOf(saved.getLongArray("VillageResidents")).cardinality() == 127
                            && BitSet.valueOf(saved.getLongArray("VillageAnimals")).cardinality() == 64
                            && BitSet.valueOf(saved.getLongArray("VillageCushions")).cardinality() == 128,
                    "Natural chunk clipping must save every completed population bit");
            var reloaded = new SavannaOrcCampPiece(serialization, saved);
            var roundtrip = reloaded.createTag(serialization);
            SavannaOrcVillage.populate(level, bounds, reloaded.templatePosition(),
                    BitSet.valueOf(roundtrip.getLongArray("VillageResidents")), BitSet.valueOf(roundtrip.getLongArray("VillageCushions")),
                    BitSet.valueOf(roundtrip.getLongArray("VillageAnimals")), reloaded.encounterId());
            helper.assertTrue(level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked).size() == 191
                            && level.getEntitiesOfClass(Cushion.class, AABB.of(bounds)).size() == 128,
                    "Reloading population state must not duplicate any inhabitant");
            for (int x : new int[]{bounds.minX(), bounds.maxX()}) for (int z : new int[]{bounds.minZ(), bounds.maxZ()})
                helper.assertTrue(generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random) > generator.getSeaLevel(),
                        "Natural camp apron corners must stay clear of river water");
            exportViews(level, piece);
            helper.succeed(); return;
        }
        helper.fail("No valid natural camp among 64 dry-divide placement cells");
    }

    private static void assertGeometry(GameTestHelper h, Function<BlockPos, BlockState> state) {
        // Four 12-block tents plus three six-block gaps occupy 66 blocks.
        for (int cx : new int[]{-27, -9, 9, 27}) for (int cz : new int[]{-27, -9, 9, 27})
            h.assertTrue(state.apply(local(cx - 1, 3, cz - 1)).is(Blocks.CAMPFIRE), "Every grid cell must retain its authored tent hearth");
        for (int sign : new int[]{-1, 1}) for (int a = 33; a <= 38; a++)
            for (int b = -32; b <= 32; b++) {
                h.assertTrue(state.apply(local(mirror(a, sign), 2, b)).is(Blocks.DIRT_PATH)
                                && state.apply(local(b, 2, mirror(a, sign))).is(Blocks.DIRT_PATH),
                        "Six clear road blocks must separate outer tents from the crop fences");
            }
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) for (boolean swap : new boolean[]{false, true}) {
            for (int a = 59; a <= 87; a++) for (int b = 3; b <= 31; b++) {
                var p = local(mirror(swap ? b : a, sx), 3, mirror(swap ? a : b, sz));
                var s = state.apply(p);
                if (a == 59 || a == 87 || b == 3 || b == 31) {
                    boolean gate = (a == 59 || a == 87) && b >= 15 && b <= 19
                            || (b == 3 || b == 31) && a >= 71 && a <= 75;
                    h.assertTrue(s.is(gate ? Blocks.ACACIA_FENCE_GATE : Blocks.ACACIA_FENCE),
                            "Each 29 by 29 pen needs four centered five-wide gates and a continuous fence: " + p);
                    if (gate) h.assertFalse(s.getValue(BlockStateProperties.OPEN), "Every authored pen gate must start closed");
                }
                else for (int y = 2; y <= 5; y++) h.assertFalse(state.apply(p.atY(y)).is(Blocks.SUGAR_CANE), "Pen ponds must never acquire sugarcane");
                if (a >= 70 && a <= 76 && b >= 14 && b <= 20)
                    h.assertTrue(state.apply(p.atY(2)).is(Blocks.WATER), "Every pen must have its centered seven-square pond");
                boolean hay = (a == 64 || a == 65 || a == 81 || a == 82) && (b == 8 || b == 9 || b == 25 || b == 26);
                if (hay) h.assertTrue(s.is(Blocks.HAY_BLOCK), "Four two-square hay piles must retain four clear diagonal spaces from the fence corners");
            }
        }
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            for (int start : new int[]{63, 79}) for (int a = start; a < start + 7; a++) for (int b = start; b < start + 7; b++)
                h.assertTrue(state.apply(local(mirror(a, sx), 2, mirror(b, sz))).is(Blocks.GRASS_BLOCK),
                        "All mushroom patch ground must stay grass without residual podzol");
            for (int a = 3; a <= 52; a++) for (int b = 3; b <= 52; b++) {
                boolean boundary = Math.max(a, b) == 52 || Math.min(a, b) == 3 && Math.max(a, b) >= 39
                        || Math.max(a, b) == 39;
                if (!boundary) continue;
                var s = state.apply(local(mirror(a, sx), 3, mirror(b, sz)));
                h.assertTrue(s.is(Blocks.ACACIA_FENCE) || s.is(Blocks.ACACIA_FENCE_GATE),
                        "The L-shaped crop plots must be enclosed along their complete inner and outer edges");
                boolean aligned = (a == 39 || a == 52) && b >= 8 && b <= 47 && (b - 8) % 12 < 4
                        || (b == 39 || b == 52) && a >= 8 && a <= 47 && (a - 8) % 12 < 4;
                if (aligned) h.assertTrue(s.is(Blocks.ACACIA_FENCE_GATE), "Crop pond alignments need fence gates");
            }
            for (int a = 4; a <= 51; a++) for (int b = 4; b <= 51; b++) {
                if (Math.max(a, b) < 40) continue;
                h.assertFalse(state.apply(local(mirror(a, sx), 2, mirror(b, sz))).is(Blocks.PODZOL),
                        "Seven complete 12-square crop plots per corner must leave no side podzol remainder");
            }
            for (int tileA : new int[]{4, 16, 28, 40}) for (int tileB : new int[]{4, 16, 28, 40}) {
                if (Math.max(tileA, tileB) != 40) continue;
                for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) {
                    var p = local(mirror(tileA + 4 + a, sx), 2, mirror(tileB + 4 + b, sz));
                    boolean edge = a == 0 || a == 3 || b == 0 || b == 3;
                    h.assertTrue(state.apply(p).is(edge ? Blocks.SUGAR_CANE : Blocks.WATER), "All 28 crop ponds retain their deterministic cane ring");
                    if (edge) h.assertTrue(state.apply(p).getValue(BlockStateProperties.WATERLOGGED)
                                    && state.apply(p.above()).is(Blocks.SUGAR_CANE) && state.apply(p.above(2)).is(Blocks.SUGAR_CANE),
                            "Crop cane must retain one submerged and two dry sections");
                }
            }
        }
        for (int direction = 0; direction < 4; direction++) for (int depth = 0; depth < 2; depth++)
            for (int across = -5; across <= 4; across++) for (int y = 3; y <= 9; y++) {
                var p = gate(direction, across, y, depth);
                var block = state.apply(p);
                if (across >= -3 && across <= 2 && y <= 7)
                    h.assertTrue(block.isAir(), "Main gates require six clear blocks across and five above the path");
                else h.assertTrue(block.is(Blocks.ACACIA_WOOD)
                                && block.getValue(BlockStateProperties.AXIS) == (direction % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z),
                        "Every pillar and beam block must use horizontal acacia wood parallel to its wall on both depth rows");
            }
        for (int side : new int[]{-96, 95}) for (boolean swap : new boolean[]{false, true}) for (int a = -96; a <= 95; a++) {
            if (a >= -5 && a <= 4) continue;
            int phase = a < -5 ? (a + 96) % 7 : (a - 4) % 7;
            var p = local(swap ? side : a, 3, swap ? a : side);
            h.assertTrue(state.apply(p).is(phase == 0 ? Blocks.ACACIA_LOG : ModBlocks.ACACIA_PALISADE.get()),
                    "Exactly six palisade panels must separate regular posts, including corners");
            for (int y = 4; y <= 12; y++) {
                var skull = state.apply(p.atY(y));
                if (!skull.is(Blocks.SKELETON_SKULL)) continue;
                boolean corner = a == -96 || a == 95;
                int rotation = skull.getValue(SkullBlock.ROTATION);
                h.assertTrue(corner ? rotation % 4 == 2 : rotation % 4 == 0,
                        "Only corner skulls may face diagonally");
                if (!corner) h.assertTrue(rotation == (swap ? (side < 0 ? 12 : 4) : (side < 0 ? 0 : 8)),
                        "Side skulls must face directly out from their original square side");
            }
        }
    }

    private static void exportViews(ServerLevel level, SavannaOrcCampPiece piece) throws Exception {
        var c = piece.templatePosition().offset(SavannaOrcVillage.CENTER, 2, SavannaOrcVillage.CENTER);
        var views = List.of(view("savanna_v11_warmup", c, -.5, 5, 107, 180, 8),
                view("savanna_v11_gate_oblique", c, 10, 7, 106, 135, 18),
                view("savanna_v11_pen", c, 93, 22, 17, 90, 50),
                view("savanna_v11_mushrooms", c, 108, 15, 88, 90, 25),
                view("savanna_v11_tents", c, 0, 32, 39, 180, 40));
        Files.writeString(Path.of("savanna-v11-views.json"), new GsonBuilder().setPrettyPrinting().create().toJson(views));
        Files.writeString(Path.of("savanna-v11-fixture.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                Map.of("dimension", GameplayDimensions.SAVANNA.location().toString(), "center", List.of(c.getX(), c.getY(), c.getZ()), "seed", level.getSeed())));
        level.getServer().saveEverything(true, true, true);
    }

    private static Map<String, Object> view(String label, BlockPos c, double x, double y, double z, float yaw, float pitch) {
        return Map.of("label", label, "dimension", GameplayDimensions.SAVANNA.location().toString(),
                "x", c.getX() + x, "y", c.getY() + y, "z", c.getZ() + z, "yaw", yaw, "pitch", pitch);
    }

    private static Map<String, Integer> quantities(Iterable<ItemStack> stacks) {
        var result = new HashMap<String, Integer>();
        for (var stack : stacks) if (!stack.isEmpty()) result.merge(ForgeRegistries.ITEMS.getKey(stack.getItem()) + ":" + stack.getTag(), stack.getCount(), Integer::sum);
        return result;
    }

    private static Map<BlockPos, BlockState> templateBlocks(ServerLevel level, ResourceLocation id) {
        var data = level.getStructureManager().getOrCreate(id).save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var result = new HashMap<BlockPos, BlockState>();
        for (var entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) entry; var pos = block.getList("pos", Tag.TAG_INT);
            result.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state"))));
        }
        return result;
    }

    private static ServerLevel savanna(GameTestHelper helper) { return helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA); }
    private static int mirror(int value, int sign) { return sign > 0 ? value : -value - 1; }
    private static BlockPos local(int x, int y, int z) { return new BlockPos(x + SavannaOrcVillage.CENTER, y, z + SavannaOrcVillage.CENTER); }
    private static BlockState defaultState(BlockPos p) { return (p.getY() == 2 ? Blocks.GRASS_BLOCK : Blocks.AIR).defaultBlockState(); }
    private static BlockPos gate(int direction, int across, int y, int depth) {
        int x = across, z = 95 - depth;
        for (int i = 0; i < direction; i++) { int previous = x; x = -z - 1; z = previous; }
        return local(x, y, z);
    }
}
