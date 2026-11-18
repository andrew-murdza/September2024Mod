package net.amurdza.examplemod.gametest;

import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcSettlement;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.amurdza.examplemod.worldgen.structure.OrcCampStock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaOrcCampGenerationTests {
    private SavannaOrcCampGenerationTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void movedVersionTwoBoundsStayExpandedDuringNativeTemplateWrites(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        var piece = versionTwoPiece(level, new BlockPos(4608, 200, 4096));
        piece.move(32, 3, 48);
        BlockPos origin = piece.templatePosition();
        BoundingBox box = piece.getBoundingBox();
        helper.assertTrue(box.minX() == origin.getX() - SavannaOrcSettlement.RAMP_MARGIN && box.maxX() == origin.getX() + 89 + SavannaOrcSettlement.RAMP_MARGIN
                        && box.minZ() == origin.getZ() - SavannaOrcSettlement.RAMP_MARGIN && box.maxZ() == origin.getZ() + 77 + SavannaOrcSettlement.RAMP_MARGIN,
                "Moved settlement bounds must follow the moved native template origin");
        loadChunks(level, box);
        int[] nativeWrites = {0};
        var world = (net.minecraft.world.level.WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(
                SavannaOrcCampGenerationTests.class.getClassLoader(), new Class<?>[]{net.minecraft.world.level.WorldGenLevel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setBlock")) {
                        var visible = piece.getBoundingBox();
                        helper.assertTrue(visible.getXSpan() == 90 + 2 * SavannaOrcSettlement.RAMP_MARGIN && visible.getZSpan() == 78 + 2 * SavannaOrcSettlement.RAMP_MARGIN
                                        && visible.minX() == origin.getX() - SavannaOrcSettlement.RAMP_MARGIN && visible.minZ() == origin.getZ() - SavannaOrcSettlement.RAMP_MARGIN,
                                "A concurrently selecting chunk must see the expanded footprint even during native template writes");
                        nativeWrites[0]++;
                    }
                    try { return method.invoke(level, args); }
                    catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                });
        var chunk = new ChunkPos(origin.offset(8, 0, 8));
        var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
        piece.postProcess(world, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(197),
                clip, chunk, origin);
        helper.assertTrue(nativeWrites[0] > 100, "Bounds audit must observe real native template block writes");
        level.getEntitiesOfClass(Mob.class, AABB.of(box), StructureSpawnedMobData::isMarked).forEach(net.minecraft.world.entity.Entity::discard);
        level.getEntitiesOfClass(Cushion.class, AABB.of(box)).forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void southernEntranceWalksDownTheFullEightBlockTerrainRange(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        BlockPos origin = new BlockPos(5120, 200, 4096);
        var bounds = SavannaOrcSettlement.bounds(origin, origin.getY() + 17, true);
        loadChunks(level, bounds);
        // A native acacia branch can overhang untouched land inside the bounding box corners.
        var branch = origin.offset(75, 0, 71);
        level.setBlock(branch, Blocks.ACACIA_LOG.defaultBlockState(), 2);
        level.setBlock(branch.below(), Blocks.AIR.defaultBlockState(), 2);
        // A hillside eight blocks below both the west wall and the southern gate.
        for (int x = -10; x <= 0; x++) level.setBlock(origin.offset(x, -6, 39), Blocks.DIRT.defaultBlockState(), 2);
        for (int x = 42; x <= 48; x++) {
            for (int z = 78; z <= 89; z++) level.setBlock(origin.offset(x, -6, z), Blocks.DIRT.defaultBlockState(), 2);
            BlockPos outside = origin.offset(x, -6, 78);
            level.getChunkAt(outside);
            for (int y = -5; y <= 17; y++) level.setBlock(origin.offset(x, y, 78), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(outside, Blocks.MUD.defaultBlockState(), 2);
            for (int z = 69; z <= 77; z++) {
                var bed = origin.offset(x, -7, z);
                level.setBlock(bed, Blocks.DIRT.defaultBlockState(), 2);
                helper.assertTrue(level.getBlockState(bed).is(Blocks.DIRT), "Native Savanna dirt must exist before ramp placement at " + bed);
                for (int y = -6; y < 0; y++) level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                for (int y = 0; y <= 2; y++) level.setBlock(origin.offset(x, y, z), Blocks.ROOTED_DIRT.defaultBlockState(), 2);
            }
        }
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                var chunk = new ChunkPos(x, z);
                var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                SavannaOrcSettlement.place(level, clip, origin, true);
            }
        }
        for (int x = 42; x <= 48; x++) for (int z = 69; z <= 84; z++) {
            int y = z <= 77 ? 1 : 1 - (z - 77);
            BlockPos floor = origin.offset(x, y, z);
            boolean isTransitionEnd = y == -6;
            helper.assertTrue((isTransitionEnd ? level.getBlockState(floor).is(Blocks.DIRT)
                            : level.getBlockState(floor).is(Blocks.DIRT_PATH))
                            && level.getBlockState(floor.above()).isAir() && level.getBlockState(floor.above(2)).isAir(),
                    "The lowered entrance must meet the native terrain with walkable single steps at " + floor);
            for (int h = origin.getY() - 6; h < floor.getY(); h++) {
                BlockPos support = new BlockPos(floor.getX(), h, floor.getZ());
                helper.assertTrue(!level.getBlockState(support).isAir(), "The ramp must have continuous support at " + support);
            }
        }
        for (int x = -6; x <= 0; x++) {
            BlockPos floor = origin.offset(x, 1 + x, 39);
            helper.assertTrue(level.getBlockState(floor).is(Blocks.GRASS_BLOCK) && level.getBlockState(floor.above()).isAir(),
                    "The western wall must also meet terrain with one-block grass steps: " + floor);
        }
        helper.assertTrue(!SavannaOrcSettlement.containsGround(75, 71) && !SavannaOrcSettlement.containsEntrance(75, 71)
                        && level.getBlockState(branch).is(Blocks.ACACIA_LOG) && level.getBlockState(branch.below()).isAir(),
                "The settlement must preserve native overhangs outside its rounded footprint");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void legacySavedCampKeepsItsFootprintAndDoesNotGainNewLivestock(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        var context = StructurePieceSerializationContext.fromLevel(level);
        var created = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(4864, 200, 4096));
        var saved = created.createTag(context);
        saved.putString("Template", "orcz:orc_camp_normal_loot");
        saved.remove("SettlementVersion");
        saved.putInt("ResidentsPlaced", 127);
        saved.putInt("CushionsPlaced", 15);
        var legacy = new SavannaOrcCampPiece(context, saved);
        BlockPos origin = legacy.templatePosition();
        helper.assertTrue(legacy.getBoundingBox().getXSpan() == 70 && legacy.getBoundingBox().getZSpan() == 42
                        && legacy.getBoundingBox().minX() == origin.getX() - 18,
                "Saved old camps must retain their original template and wing footprint");
        loadChunks(level, legacy.getBoundingBox());
        placeChunk(level, legacy, new ChunkPos(origin.offset(13, 3, 39)));
        var persisted = legacy.createTag(context);
        helper.assertTrue(persisted.getInt("AnimalsPlaced") == 0 && !persisted.contains("SettlementVersion")
                        && persisted.getInt("ResidentsPlaced") == 127 && persisted.getInt("CushionsPlaced") == 15,
                "Legacy replay must preserve saved inhabitants and never append new settlement livestock");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void naturalSavannaGridGeneratesTheCompleteNativeCampOnDryLand(GameTestHelper helper) throws Exception {
        ServerLevel level = savanna(helper);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod", "orc_camp"));
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(new ResourceLocation("aoemod", "orc_camps"));
        helper.assertTrue(set.placement() instanceof SavannaCampPlacement
                        && SavannaCampPlacement.X_SPACING == 1152 && SavannaCampPlacement.Z_SPACING == 960,
                "Fresh camps must use the complete-square dry-divide placement cells");
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        for (int rx = 111; rx < 119; rx++) for (int rz = 113; rz < 121; rz++) {
            ChunkPos owner = new ChunkPos(SavannaCampPlacement.findCenter(SavannaCampPlacement.terrainLand(random), rx, rz));
            var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                    level.getStructureManager(), level.getSeed(), owner, level, structure.biomes()::contains);
            var candidate = structure.findValidGenerationPoint(context);
            if (candidate.isEmpty()) continue;
            var chunk = level.getChunk(owner.x, owner.z, ChunkStatus.STRUCTURE_STARTS, true);
            var start = chunk.getStartForStructure(structure);
            helper.assertTrue(start != null && start.isValid() && start.getPieces().size() == 1
                            && start.getPieces().get(0) instanceof SavannaOrcCampPiece,
                    "The registered dry-divide camp must use a full native template piece");
            var piece = (SavannaOrcCampPiece) start.getPieces().get(0);
            var box = piece.getBoundingBox();
            helper.assertTrue(box.getXSpan() == SavannaOrcVillage.SIZE + 2 * SavannaOrcVillage.MARGIN
                            && box.getZSpan() == SavannaOrcVillage.SIZE + 2 * SavannaOrcVillage.MARGIN,
                    "New natural camps must use the nine-tent square village and its terrain apron");
            for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!piece.containsSurfaceColumn(new BlockPos(x, piece.templatePosition().getY(), z))) continue;
                int height = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                helper.assertTrue(height > generator.getSeaLevel(), "A natural camp must not overlap lake water");
            }
            loadChunks(level, box);
            for (int x : new int[]{box.minX() >> 4, box.maxX() >> 4}) for (int z : new int[]{box.minZ() >> 4, box.maxZ() >> 4}) {
                helper.assertTrue(Math.max(Math.abs(x - owner.x), Math.abs(z - owner.z)) <= 8,
                        "Fresh camp corners must remain within native eight-chunk reference reach");
                helper.assertTrue(level.getChunk(x, z).getReferencesForStructure(structure).contains(owner.toLong()),
                        "Every far camp corner needs its actual native start reference");
            }
            for (int x : new int[]{box.minX(), box.maxX()}) for (int z : new int[]{box.minZ(), box.maxZ()})
                helper.assertTrue(generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random) > generator.getSeaLevel(),
                        "The square camp's complete apron corners must remain inland");
            assertVillageBlocks(helper, level, piece);
            BlockPos origin = piece.templatePosition();
            for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int localX = x - origin.getX(), localZ = z - origin.getZ();
                if (!SavannaOrcVillage.contains(localX, localZ)) continue;
                BlockPos bottom = new BlockPos(x, origin.getY(), z);
                if (level.getBlockState(bottom).isSolidRender(level, bottom)) {
                    helper.assertTrue(!level.getBlockState(bottom.below()).isAir(),
                            "Camp foundations must join the hillside at " + bottom);
                }
            }
            assertVillageResidents(helper, level, box);
            SavannaNovember16Tests.assertPlacedRevision(helper, level, piece);
            SavannaNovember16Tests.exportNaturalViews(level, piece);
            helper.assertTrue(net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludes(level, origin.offset(SavannaOrcVillage.CENTER, 3, SavannaOrcVillage.CENTER)),
                    "The actual natural structure start must exclude wild crop and mushroom patches");
            helper.succeed();
            return;
        }
        helper.fail("No natural Savanna camp accepted among 64 dry-divide placement cells");
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void largeDryDividesFitTheExistingFoundationAndRampCapacity(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod", "orc_camp"));
        var generator = (net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int accepted = 0, previouslyRejected = 0;
        int extent = SavannaOrcVillage.CENTER + SavannaOrcVillage.MARGIN;
        for (long seed : new long[]{5125636729086549175L, 0, 12345}) {
            var random = net.minecraft.world.level.levelgen.RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            int acceptedForSeed = 0;
            for (int cell = 0; cell < 4; cell++) {
                var center = SavannaCampPlacement.findCenter(SavannaCampPlacement.terrainLand(random), 111 + cell, 113 + cell);
                var owner = new ChunkPos(center);
                var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                        level.getStructureManager(), seed, owner, level, structure.biomes()::contains);
                var candidate = structure.findValidGenerationPoint(context);
                if (candidate.isEmpty()) continue;
                int floor = candidate.orElseThrow().position().getY() + 2;
                int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
                // Check both edges for every X, including the last two columns
                // between the site's four-block samples and exact apron corner.
                for (int dx = -extent; dx <= extent; dx++) for (int dz : new int[]{-extent, 0, extent}) {
                    int x = center.getX() + dx, z = center.getZ() + dz;
                    int top = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random) - 1;
                    helper.assertTrue(top >= generator.getSeaLevel(), "Accepted full apron must remain dry at " + x + "," + z);
                    helper.assertTrue(floor - top <= 14 && floor - top <= SavannaOrcVillage.MARGIN,
                            "Existing foundation and single-step apron must reach every accepted natural column");
                    lowest = Math.min(lowest, top); highest = Math.max(highest, top);
                }
                helper.assertTrue(Math.abs(((center.getX() - extent) >> 4) - owner.x) <= 8
                                && Math.abs(((center.getX() + extent) >> 4) - owner.x) <= 8
                                && Math.abs(((center.getZ() - extent) >> 4) - owner.z) <= 8
                                && Math.abs(((center.getZ() + extent) >> 4) - owner.z) <= 8,
                        "Siting must not move the camp outside its owner's native reference radius");
                if (highest - lowest > 8) previouslyRejected++;
                acceptedForSeed++; accepted++;
            }
            helper.assertTrue(acceptedForSeed > 0, "Expanded natural camps must remain selectable for seed " + seed);
        }
        helper.assertTrue(accepted >= 3,
                "The current compact footprint must have accepted dry sites across all three tested seeds");
        AOEMod.LOGGER.info("Natural current camp siting: {} accepted sites, {} beyond the former relief limit", accepted, previouslyRejected);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void versionTwoChunkClippingAndReloadKeepOriginalSeatsGuardsAndStock(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        var piece = versionTwoPiece(level, new BlockPos(4096, 200, 4096));
        BoundingBox box = piece.getBoundingBox();
        loadChunks(level, box);
        var first = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
        BlockPos untouched = new BlockPos(box.maxX(), box.minY(), box.maxZ());
        level.setBlock(untouched, Blocks.ACACIA_PLANKS.defaultBlockState(), 2);
        placeChunk(level, piece, first);
        helper.assertTrue(level.getBlockState(untouched).is(Blocks.ACACIA_PLANKS),
                "A camp piece must not write outside the chunk currently being generated");
        var context = StructurePieceSerializationContext.fromLevel(level);
        piece = new SavannaOrcCampPiece(context, piece.createTag(context));
        placeAllChunks(level, piece);
        assertCampBlocks(helper, level, box);
        assertCampResidents(helper, level, box);
        piece = new SavannaOrcCampPiece(context, piece.createTag(context));
        placeAllChunks(level, piece);
        assertCampResidents(helper, level, box);
        level.getEntitiesOfClass(Mob.class, AABB.of(box), StructureSpawnedMobData::isMarked)
                .forEach(net.minecraft.world.entity.Entity::discard);
        level.getEntitiesOfClass(Cushion.class, AABB.of(box)).forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void bothNativeFeatureCommandsPlaceTheSavannaCamp(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        for (int index = 0; index < 2; index++) {
            BlockPos pos = new BlockPos(43520 + index * 512, 200, 40960);
            var expected = new SavannaOrcCampPiece(level.getStructureManager(), pos.above(4));
            var bounds = expected.getBoundingBox();
            loadChunks(level, bounds);
            String feature = index == 0 ? "camp" : "camp_the_other";
            int result = level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack().withLevel(level)
                            .withPosition(Vec3.atLowerCornerOf(pos)).withSuppressedOutput(),
                    "place feature orcz:" + feature + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            helper.assertTrue(result == 1, "Both native Orcz feature commands must work in Savanna: " + feature);
            assertVillageBlocks(helper, level, expected);
            assertVillageResidents(helper, level, bounds);
            helper.assertTrue(net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages.get(level).contains(pos),
                    "Both command variants must record the camp for crop exclusion");
            level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked)
                    .forEach(net.minecraft.world.entity.Entity::discard);
            level.getEntitiesOfClass(Cushion.class, AABB.of(bounds)).forEach(net.minecraft.world.entity.Entity::discard);
        }
        helper.succeed();
    }

    /** Build the real serialized version-two format without a test-only production constructor. */
    private static SavannaOrcCampPiece versionTwoPiece(ServerLevel level, BlockPos center) {
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

    private static void assertVillageBlocks(GameTestHelper helper, ServerLevel level, SavannaOrcCampPiece piece) {
        BlockPos origin = piece.templatePosition();
        int chests = 0, redWool = 0, farmland = 0;
        var indices = new java.util.HashSet<Integer>();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(piece.template().getSize().getX() - 1, piece.template().getSize().getY() - 1, piece.template().getSize().getZ() - 1))) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.RED_WOOL)) redWool++;
            if (state.is(Blocks.FARMLAND)) farmland++;
            if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) continue;
            chests++;
            var data = chest.getPersistentData();
            helper.assertTrue(data.getInt(OrcCampStock.STOCK_TAG) == OrcCampStock.selection(level.getSeed(), origin)
                            && data.contains(net.amurdza.examplemod.worldgen.structure.OrcVillageLoot.INDEX),
                    "Each new village chest must retain its common stock and individual loot-share index");
            indices.add(data.getInt(net.amurdza.examplemod.worldgen.structure.OrcVillageLoot.INDEX));
        }
        var template = piece.template().save(new net.minecraft.nbt.CompoundTag());
        var palette = template.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
        int expectedWool = 0, expectedFarmland = 0;
        for (var entry : template.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            String id = palette.getCompound(((net.minecraft.nbt.CompoundTag) entry).getInt("state")).getString("Name");
            if (id.equals("minecraft:red_wool")) expectedWool++;
            if (id.equals("minecraft:farmland")) expectedFarmland++;
        }
        helper.assertTrue(chests == 36 && indices.size() == 36 && redWool == expectedWool
                        && farmland == expectedFarmland && expectedWool > 1000 && expectedFarmland > 480,
                "Actual new camps must preserve every authored red roof and crop floor, plus all 36 unique chest shares");
        int center = SavannaOrcVillage.CENTER, radius = SavannaOrcVillage.RADIUS;
        for (int[] gate : new int[][]{{center, center - radius}, {center + radius - 1, center},
                {center, center + radius - 1}, {center - radius, center}}) {
            helper.assertTrue(level.getBlockState(origin.offset(gate[0], 2, gate[1])).is(Blocks.DIRT_PATH)
                            && level.getBlockState(origin.offset(gate[0], 7, gate[1])).is(Blocks.ACACIA_LOG),
                    "Every cardinal side needs the authored gate");
            for (int y = 3; y <= 6; y++) helper.assertTrue(level.getBlockState(origin.offset(gate[0], y, gate[1])).isAir(),
                    "Every village entrance must have a clear passage");
        }
    }

    private static void assertVillageResidents(GameTestHelper helper, ServerLevel level, BoundingBox bounds) {
        var cushions = level.getEntitiesOfClass(Cushion.class, AABB.of(bounds));
        helper.assertTrue(cushions.size() == 72 && cushions.stream().allMatch(cushion -> cushion.getColor() == DyeColor.RED && cushion.survives()),
                "New camps must contain eight supported red cushions in each of nine tents");
        var residents = level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked);
        var expected = new java.util.HashMap<String, Integer>();
        for (String id : new String[]{"alexsmobs:bison", "minecraft:sheep", "alexsmobs:gazelle", "biodiverse:oryx", "alexsmobs:emu",
                "alexsmobs:kangaroo", "twilightforest:boar", "biodiverse:wildebeest"}) expected.put(id, 4);
        expected.put("orcz:orcshield", 17); expected.put("orcz:orcsavagemace", 18); expected.put("orcz:bigorcboss", 1);
        var actual = new java.util.HashMap<String, Integer>();
        residents.forEach(mob -> actual.merge(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString(), 1, Integer::sum));
        helper.assertTrue(actual.equals(expected),
                "New camps require 36 orcs plus eight separate groups of four livestock: " + actual);
        assertNoResidentCollisions(helper, level, residents);
    }

    static void assertNoResidentCollisions(GameTestHelper helper, ServerLevel level, java.util.List<Mob> residents) {
        for (Mob mob : residents) {
            if (level.noCollision(mob)) continue;
            var blocks = BlockPos.betweenClosedStream(mob.getBoundingBox()).filter(pos -> !level.getBlockState(pos)
                    .getCollisionShape(level, pos, net.minecraft.world.phys.shapes.CollisionContext.of(mob)).isEmpty())
                    .map(pos -> pos.immutable() + "=" + level.getBlockState(pos)).toList();
            helper.fail("Village resident intersects furnishings, fences or another actor: "
                    + ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()) + " at " + mob.position()
                    + ", box=" + mob.getBoundingBox() + ", blocks=" + blocks
                    + ", entity collisions=" + level.getEntityCollisions(mob, mob.getBoundingBox()).size());
        }
    }

    private static void assertCampBlocks(GameTestHelper helper, ServerLevel level, BoundingBox box) {
        int wool = 0, woolSlabs = 0, chests = 0, charcoal = 0, palisades = 0, wheat = 0, carrots = 0, paths = 0;
        var charcoalBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("quark", "charcoal_block"));
        var woolSlab = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", "black_wool_slab"));
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.BLACK_WOOL)) wool++;
            if (state.is(woolSlab)) woolSlabs++;
            if (state.is(Blocks.CHEST)) chests++;
            if (state.is(charcoalBlock)) charcoal++;
            if (state.is(ModBlocks.ACACIA_PALISADE.get())) {
                palisades++;
                // Global palisade behavior now deliberately follows vanilla walls,
                // including tall sides and interior posts. Compare the actual generated
                // connections with a vanilla wall placed against these same neighbors.
                var context = new net.minecraft.world.item.context.DirectionalPlaceContext(level, pos,
                        net.minecraft.core.Direction.DOWN, new net.minecraft.world.item.ItemStack(Blocks.COBBLESTONE_WALL),
                        net.minecraft.core.Direction.UP);
                var nativeWall = Blocks.COBBLESTONE_WALL.getStateForPlacement(context);
                // Version two deliberately retains authored center posts even where
                // vanilla would omit one. Its arms and waterlogging still match exactly.
                helper.assertTrue(nativeWall != null
                                && state.setValue(WallBlock.UP, nativeWall.getValue(WallBlock.UP)).getValues().equals(nativeWall.getValues())
                                && (!nativeWall.getValue(WallBlock.UP) || state.getValue(WallBlock.UP)),
                        "Generated legacy palisade connections differ from vanilla walls at " + pos
                                + ": actual=" + state + ", expected=" + nativeWall);
            }
            if (state.is(Blocks.WHEAT)) wheat++;
            if (state.is(Blocks.CARROTS)) carrots++;
            if (state.is(Blocks.DIRT_PATH)) paths++;
            helper.assertTrue(!new ResourceLocation("orcz", "orcsymbolred")
                    .equals(ForgeRegistries.BLOCKS.getKey(state.getBlock())), "Camp must not retain the red Orcz symbol");
        }
        helper.assertTrue(wool == 10 && woolSlabs == 130 && chests == 8 && charcoal == 5 && palisades >= 400,
                "Rebuilt camp must keep all seven native roofs/chests and one curved log-and-palisade boundary: "
                        + wool + "/" + woolSlabs + "/" + chests + "/" + charcoal + "; acacia palisades=" + palisades);
        helper.assertTrue(wheat == 184 && (carrots == 92 || carrots == 0) && paths > 300,
                "Actual camp generation must preserve both cultivated food plots and dirt paths; wheat=" + wheat
                        + ", carrots=" + carrots + ", paths=" + paths);
        BlockPos origin = new BlockPos(box.minX() + SavannaOrcSettlement.RAMP_MARGIN, box.minY() + 13, box.minZ() + SavannaOrcSettlement.RAMP_MARGIN);
        for (int plot = 0; plot < 2; plot++) {
            int minX = plot == 0 ? 30 : 52;
            for (int x = minX; x <= minX + 8; x++) for (int z = 28; z <= 50; z++) {
                if (x == minX + 4) continue;
                BlockPos soil = origin.offset(x, 2, z);
                helper.assertTrue(level.getBlockState(soil).is(Blocks.FARMLAND)
                                && level.getBlockState(soil).getValue(BlockStateProperties.MOISTURE) == 7
                                && level.getBlockState(soil.above()).equals(OrcCampStock.matureCrop(OrcCampStock.selection(level.getSeed(), origin), plot * 2 + (x < minX + 4 ? 0 : 1))),
                        "Every planted plot cell must hold a crop on hydrated farmland at " + soil);
            }
        }
        for (int minX : new int[]{15, 65}) for (int x = minX; x < minX + 3; x++) for (int z = 47; z <= 49; z++) {
            BlockPos water = origin.offset(x, 2, z);
            helper.assertTrue(level.getBlockState(water).is(Blocks.WATER) && level.getBlockState(water.below()).is(Blocks.MUD),
                    "Actual livestock ponds must retain all nine water cells over mud at " + water);
        }
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyStockSelectionUsesItsLocalMeatAndCrops(GameTestHelper helper) {
        String[][] products = {
                {"wildernature:bison_meat", "wildernature:cooked_bison_meat", "minecraft:mutton", "minecraft:cooked_mutton"},
                {"wildernature:venison", "wildernature:cooked_venison", "wildernature:venison", "wildernature:cooked_venison"},
                {"wildernature:cassowary_meat", "wildernature:cooked_cassowary_meat", "alexsmobs:kangaroo_meat", "alexsmobs:cooked_kangaroo_meat"},
                {"minecraft:porkchop", "minecraft:cooked_porkchop", "minecraft:beef", "minecraft:cooked_beef"}
        };
        for (int stock = 0; stock < 256; stock++) {
            var expected = new java.util.HashSet<String>();
            for (int herd = 0; herd < 4; herd++) {
                helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(OrcCampStock.animal(stock, herd)))
                                && net.amurdza.examplemod.util.RetainedContent.isKeptEntity(new ResourceLocation(OrcCampStock.animal(stock, herd))),
                        "Every selectable camp animal must be registered");
                int pair = ((stock >> herd) & 1) * 2;
                expected.add(products[herd][pair]);
                expected.add(products[herd][pair + 1]);
            }
            if ((stock & 4) == 0) expected.addAll(java.util.List.of("alexsmobs:emu_egg", "alexsmobs:boiled_emu_egg", "alexsmobs:emu_feather"));
            expected.addAll((stock & 16) == 0 ? java.util.List.of("minecraft:carrot", "aoemod:carrot_soup")
                    : java.util.List.of("minecraft:beetroot", "minecraft:beetroot_seeds", "minecraft:beetroot_soup"));
            expected.addAll((stock & 32) == 0 ? java.util.List.of("farmersdelight:cabbage", "farmersdelight:cabbage_seeds", "farmersdelight:cabbage_rolls")
                    : java.util.List.of("minecraft:potato", "minecraft:baked_potato"));
            var observed = new java.util.HashSet<String>();
            var random = RandomSource.create(stock);
            for (int chest = 0; chest < 120; chest++) {
                var stacks = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
                OrcCampStock.addProvisions(stock, random, stacks);
                stacks.forEach(stack -> observed.add(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()));
            }
            helper.assertTrue(observed.equals(expected), "Unregistered, missing or unrelated provisions in camp stock " + stock
                    + " expected " + expected + " got " + observed);
        }
        helper.succeed();
    }

    private static void assertCampChestStock(GameTestHelper helper, ServerLevel level, BoundingBox box, BlockPos origin) {
        int stock = OrcCampStock.selection(level.getSeed(), origin);
        var staples = java.util.Set.of("bone", "leather", "wheat", "bread", "wheat_seeds", "acacia_log", "charcoal");
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), origin.getY() + 3, box.minZ(),
                box.maxX(), origin.getY() + 5, box.maxZ())) {
            if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) continue;
            helper.assertTrue(chest.getPersistentData().contains(OrcCampStock.STOCK_TAG)
                    && chest.getPersistentData().getInt(OrcCampStock.STOCK_TAG) == stock, "Chest must retain its camp's inventory selection");
            int food = 0;
            var partial = new java.util.HashSet<net.minecraft.world.item.Item>();
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                var stack = chest.getItem(slot);
                if (stack.isEmpty()) continue;
                var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (!staples.contains(id.getPath())) food++;
                if (stack.getCount() < stack.getMaxStackSize()) {
                    helper.assertTrue(partial.add(stack.getItem()), "The chest must contain at most one partial stack of " + id);
                }
            }
            helper.assertTrue(food > 0, "Opening the chest must generate provisions from this camp's herds and fields");
        }
    }

    private static void exportCampViews(ServerLevel level, BlockPos origin) throws Exception {
        List<Map<String, Object>> views = List.of(
                campView(origin, "orc_camp_compound", 45, 80, 115, 180, 48),
                campView(origin, "orc_camp_crops", 45, 24, 65, 180, 39),
                campView(origin, "orc_camp_pond", 16, 11, 49, 135, 45),
                campView(origin, "orc_camp_palisade", 45, 10, -5, 0, 35));
        Path output = Path.of("build", "reports", "orc-camp-views.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(views) + "\n");
        level.getServer().saveEverything(true, true, true);
    }

    private static Map<String, Object> campView(BlockPos origin, String label,
                                               double x, double y, double z, float yaw, float pitch) {
        return Map.of("label", label, "dimension", GameplayDimensions.SAVANNA.location().toString(),
                "x", origin.getX() + x, "y", origin.getY() + y, "z", origin.getZ() + z, "yaw", yaw, "pitch", pitch);
    }

    private static void assertCampResidents(GameTestHelper helper, ServerLevel level, BoundingBox box) {
        var seats = level.getEntitiesOfClass(Cushion.class, AABB.of(box));
        helper.assertTrue(seats.size() == 4 && seats.stream().allMatch(seat -> seat.getColor() == DyeColor.RED && seat.survives()),
                "The four campfire seats must be supported red cushions without duplicates");
        var residents = level.getEntitiesOfClass(Mob.class, AABB.of(box), StructureSpawnedMobData::isMarked);
        helper.assertTrue(residents.size() == 23 && residents.stream().allMatch(level::noCollision),
                "Camp must keep six guards, one boss, two pens of eight livestock at unobstructed positions, without duplicates");
        var origin = new BlockPos(box.minX() + SavannaOrcSettlement.RAMP_MARGIN, box.minY() + 13, box.minZ() + SavannaOrcSettlement.RAMP_MARGIN);
        int stock = OrcCampStock.selection(level.getSeed(), origin);
        for (int herd = 0; herd < 4; herd++) {
            String id = OrcCampStock.animal(stock, herd);
            helper.assertTrue(residents.stream().filter(mob -> new ResourceLocation(id)
                            .equals(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()))).count() == 4,
                    "Each of the two pens must retain exactly four of each selected species without respawning duplicates: " + id);
        }
        helper.assertTrue(residents.stream().filter(net.minecraft.world.entity.animal.Sheep.class::isInstance)
                        .map(net.minecraft.world.entity.animal.Sheep.class::cast).allMatch(sheep -> sheep.getColor() == DyeColor.BROWN),
                "Every sheep selected for the first pen must be brown");
        for (String id : new String[]{"orcshield", "orcsavagemace", "bigorcboss"}) {
            helper.assertTrue(residents.stream().filter(mob -> new ResourceLocation("orcz", id)
                            .equals(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()))).count() == (id.equals("bigorcboss") ? 1 : 3),
                    "Each Savanna camp must contain three of each selected guard and one boss: " + id);
        }
    }

    private static ServerLevel savanna(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
    }

    private static void loadChunks(ServerLevel level, BoundingBox box) {
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) level.getChunk(x, z);
        }
    }

    private static void placeAllChunks(ServerLevel level, SavannaOrcCampPiece piece) {
        var box = piece.getBoundingBox();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) placeChunk(level, piece, new ChunkPos(x, z));
        }
    }

    private static void placeChunk(ServerLevel level, SavannaOrcCampPiece piece, ChunkPos chunk) {
        var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(9173),
                clip, chunk, piece.templatePosition());
    }
}
