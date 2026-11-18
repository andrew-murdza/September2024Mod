package net.amurdza.examplemod.gametest;

import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.OrcCampStock;
import net.amurdza.examplemod.worldgen.structure.OrcVillageLoot;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaNovemberRevisionTests {
    private static final String[] ANIMALS = {"alexsmobs:bison", "minecraft:sheep", "alexsmobs:gazelle", "biodiverse:oryx",
            "alexsmobs:emu", "alexsmobs:kangaroo", "twilightforest:boar", "biodiverse:wildebeest"};

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void authoredVillageKeepsNineTentsAndFourGatesWithinCubicOutline(GameTestHelper helper) {
        SavannaRoundTwoTests.expandedTemplateHasCompleteTentsPensAndStockade(helper);
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void villagePopulationPersistsAcrossChunksAndPieceReload(GameTestHelper helper) throws Exception {
        var level = savanna(helper);
        int reviewCenter = 14336;
        int ground = 0;
        for (int dx = -SavannaOrcVillage.RADIUS; dx <= SavannaOrcVillage.RADIUS; dx += 4)
            for (int dz = -SavannaOrcVillage.RADIUS; dz <= SavannaOrcVillage.RADIUS; dz += 4) {
            if (SavannaOrcVillage.contains(dx + SavannaOrcVillage.CENTER, dz + SavannaOrcVillage.CENTER)) ground = Math.max(ground,
                    level.getChunkSource().getGenerator().getBaseHeight(reviewCenter + dx, reviewCenter + dz,
                            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState()) - 1);
        }
        var piece = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(reviewCenter, ground + 2, reviewCenter));
        var bounds = piece.getBoundingBox();
        var chunks = new ArrayList<ChunkPos>();
        var forcedHere = new ArrayList<ChunkPos>();
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            level.getChunk(x, z);
            chunks.add(chunk);
            if (level.setChunkForced(x, z, true)) forcedHere.add(chunk);
        }
        // Block chunks load before their asynchronous entity-region reads. A
        // rerun must wait for every saved actor before replacing this fixture.
        helper.startSequence().thenWaitUntil(() -> {
            for (var chunk : chunks) helper.assertTrue(level.areEntitiesLoaded(chunk.toLong()),
                    "Waiting for persisted Savanna scene entities in " + chunk);
        }).thenExecute(() -> {
            try {
                populateVillageFixture(helper, level, piece, chunks);
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            } finally {
                for (var chunk : forcedHere) level.setChunkForced(chunk.x, chunk.z, false);
            }
        }).thenSucceed();
    }

    private static void populateVillageFixture(GameTestHelper helper, ServerLevel level, SavannaOrcCampPiece piece,
            List<ChunkPos> chunks) throws java.io.IOException {
        var context = StructurePieceSerializationContext.fromLevel(level);
        var origin = piece.templatePosition(); var bounds = piece.getBoundingBox();
        level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked).forEach(Entity::discard);
        level.getEntitiesOfClass(Cushion.class, AABB.of(bounds)).forEach(Entity::discard);
        level.getEntitiesOfClass(ItemEntity.class, AABB.of(bounds)).forEach(Entity::discard);
        {
            for (int i = 0; i < chunks.size(); i++) {
                if (i == chunks.size() / 2) piece = new SavannaOrcCampPiece(context, piece.createTag(context));
                place(level, piece, chunks.get(i));
            }
            var saved = piece.createTag(context);
            BlockPos placedCenter = origin.offset(SavannaOrcVillage.CENTER, 2, SavannaOrcVillage.CENTER);
            var directRecord = net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages.get(level);
            var savedDirectRecord = net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages.load(directRecord.save(new CompoundTag()));
            helper.assertTrue(directRecord.contains(placedCenter) && savedDirectRecord.contains(placedCenter.east(SavannaOrcVillage.RADIUS))
                            && !savedDirectRecord.contains(placedCenter.east(SavannaOrcVillage.RADIUS + 1)),
                    "Direct piece placement must persist its exact square exclusion before the feature command runs");
            helper.assertTrue(BitSet.valueOf(saved.getLongArray("VillageResidents")).cardinality() == 36
                            && BitSet.valueOf(saved.getLongArray("VillageCushions")).cardinality() == 72
                            && BitSet.valueOf(saved.getLongArray("VillageAnimals")).cardinality() == 32,
                    "Chunk clipping and mid-generation NBT reload must preserve all population bits");
            var residents = level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked);
            var cushions = level.getEntitiesOfClass(Cushion.class, AABB.of(bounds));
            helper.assertTrue(residents.size() == 68 && cushions.size() == 72, "A complete village requires 36 orcs, 32 livestock and 72 cushions");
            helper.assertTrue(cushions.stream().allMatch(cushion -> cushion.getColor() == DyeColor.RED && cushion.survives()), "Every cushion must be red and supported");
            for (String id : ANIMALS) helper.assertTrue(residents.stream().filter(mob -> id.equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())))).count() == 4,
                    "Each of the eight penned species needs exactly four animals: " + id);
            SavannaOrcCampGenerationTests.assertNoResidentCollisions(helper, level, residents);
            for (var entry : templateBlocks(level).entrySet()) {
                BlockPos local = entry.getKey(); BlockState expected = entry.getValue();
                if (expected.is(Blocks.FARMLAND) || expected.is(Blocks.GRASS) || expected.is(Blocks.TALL_GRASS)
                        || expected.is(Blocks.ACACIA_LEAVES) || expected.is(Blocks.PODZOL)) {
                    helper.assertTrue(level.getBlockState(origin.offset(local)).equals(expected),
                            "Ground placement must preserve the template's crops, complete grass, trees and podzol: " + local);
                }
            }
            // Ground is placed before the template. Its grass ledge must not
            // repaint the middle or outer lanes of the eight-wide gate path.
            assertGatePaths(helper, level, origin, "chunk-clipped template placement");
            // Replay only the population stage: replacing an already-generated template would reset opened chests.
            var reloaded = new SavannaOrcCampPiece(context, saved);
            var residentsBits = BitSet.valueOf(reloaded.createTag(context).getLongArray("VillageResidents"));
            var cushionsBits = BitSet.valueOf(saved.getLongArray("VillageCushions"));
            var animalsBits = BitSet.valueOf(saved.getLongArray("VillageAnimals"));
            SavannaOrcVillage.populate(level, bounds, origin, residentsBits, cushionsBits, animalsBits, reloaded.encounterId());
            helper.assertTrue(level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked).size() == 68
                            && level.getEntitiesOfClass(Cushion.class, AABB.of(bounds)).size() == 72,
                    "Reloading population state must never duplicate inhabitants");
            BlockPos center = origin.offset(SavannaOrcVillage.CENTER, 2, SavannaOrcVillage.CENTER);
            var camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:orc_camp"));
            ChunkPos owner = new ChunkPos(center);
            var start = new net.minecraft.world.level.levelgen.structure.StructureStart(camp, owner, 0,
                    new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(List.of(piece)));
            level.getChunk(owner.x, owner.z).setStartForStructure(camp, start);
            for (ChunkPos chunk : chunks) level.getChunk(chunk.x, chunk.z).addReferenceForStructure(camp, owner.toLong());
            helper.assertTrue(net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludes(level, center)
                            && net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludesPatch(level, center.getX() + SavannaOrcVillage.RADIUS, center.getZ())
                            && !net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludes(level, center.offset(SavannaOrcVillage.RADIUS + 1, 0, SavannaOrcVillage.RADIUS + 1)),
                    "Actual native structure starts must exclude all intersecting wild crop/mushroom patches without excluding outside land");
            // Exercise the real command route too, after clearing the prior fixture's inhabitants.
            residents.forEach(Entity::discard); cushions.forEach(Entity::discard);
            BlockPos command = origin.offset(SavannaOrcVillage.CENTER, -4, SavannaOrcVillage.CENTER);
            int result = level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                    .withLevel(level).withSuppressedOutput(), "place feature orcz:camp " + command.getX() + " " + command.getY() + " " + command.getZ());
            helper.assertTrue(result == 1, "The actual Savanna feature command must build the authored village");
            var manual = net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages.get(level);
            var loaded = net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages.load(manual.save(new CompoundTag()));
            helper.assertTrue(manual.contains(center) && loaded.contains(center) && !loaded.contains(center.offset(SavannaOrcVillage.RADIUS + 1, 0, SavannaOrcVillage.RADIUS + 1)),
                    "Command-placed camp exclusion must survive its actual SavedData round trip");
            level.getChunk(owner.x, owner.z).setStartForStructure(camp, net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START);
            helper.assertTrue(net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludes(level, center),
                    "Manual village records must participate in the live crop exclusion policy even without a native structure start");
            var commandResidents = level.getEntitiesOfClass(Mob.class, AABB.of(bounds), StructureSpawnedMobData::isMarked);
            var commandCushions = level.getEntitiesOfClass(Cushion.class, AABB.of(bounds));
            helper.assertTrue(commandResidents.size() == 68 && commandCushions.size() == 72,
                    "The command-created scene must contain exactly 36 orcs, 32 livestock and 72 cushions after saved-entity cleanup; got "
                            + commandResidents.size() + " mobs and " + commandCushions.size() + " cushions");
            for (String id : ANIMALS) helper.assertTrue(commandResidents.stream()
                            .filter(mob -> id.equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())))).count() == 4,
                    "The command-created scene needs exactly four animals of each penned species: " + id);
            int groundcover = 0;
            int nativeTreeLogs = 0;
            var authored = templateBlocks(level);
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                if (!SavannaOrcVillage.contains(x - origin.getX(), z - origin.getZ())) continue;
                BlockPos pos = new BlockPos(x, center.getY() + 1, z);
                var state = level.getBlockState(pos);
                if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                        || state.is(net.minecraft.tags.BlockTags.FLOWERS)) groundcover++;
                for (int dy = 1; dy <= 20; dy++) {
                    BlockPos log = new BlockPos(x, center.getY() + dy, z);
                    if (!level.getBlockState(log).is(Blocks.ACACIA_LOG)) continue;
                    BlockPos local = log.subtract(origin);
                    if (authored.getOrDefault(local, Blocks.AIR.defaultBlockState()).is(Blocks.ACACIA_LOG)) continue;
                    nativeTreeLogs++;
                    helper.assertFalse(net.amurdza.examplemod.worldgen.SavannaCampVegetation.reservedLocal(local.getX(), local.getZ(), true),
                            "Native tree trunks and branches must not enter pens or authored columns: " + log);
                }
            }
            helper.assertTrue(groundcover > 0, "Command placement must replay the native biome's groundcover on the raised mound");
            assertGatePaths(helper, level, origin, "feature command and native vegetation replay");
            SavannaNovember16Tests.assertPlacedRevision(helper, level, piece);
            AOEMod.LOGGER.info("Savanna camp native vegetation: {} groundcover columns and {} tree-log blocks", groundcover, nativeTreeLogs);
            helper.assertTrue(commandResidents.stream().filter(mob -> "orcz".equals(
                            ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getNamespace())).count() == 36,
                    "The command-created scene needs exactly 36 orcs");
            // Replacing opened test chests and crops may drop their old contents.
            level.getEntitiesOfClass(ItemEntity.class, AABB.of(bounds)).forEach(Entity::discard);
            exportViews(level, origin);
        }
    }

    private static void assertGatePaths(GameTestHelper helper, ServerLevel level, BlockPos origin, String phase) {
        for (int direction = 0; direction < 4; direction++) {
            for (int distance = SavannaOrcVillage.RADIUS - 1; distance <= SavannaOrcVillage.RADIUS + 1; distance++) for (int across = -4; across <= 3; across++) {
                int dx = direction < 2 ? across : direction == 2 ? -distance : distance;
                int dz = direction >= 2 ? across : direction == 0 ? -distance : distance;
                BlockPos path = origin.offset(SavannaOrcVillage.CENTER + dx, 2, SavannaOrcVillage.CENTER + dz);
                helper.assertTrue(level.getBlockState(path).is(Blocks.DIRT_PATH),
                        "All eight gate-path lanes across the boundary and flat ledge must survive " + phase + ": " + path);
            }
        }
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void allThirtySixOpenedChestsShareExactlyThePreviousEightChestBudget(GameTestHelper helper) {
        var level = savanna(helper);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).atY(245);
        var data = new CompoundTag(); data.putLong(OrcVillageLoot.SEED, 9418153); data.putLong(OrcVillageLoot.ORIGIN, origin.asLong());
        data.putInt(OrcCampStock.STOCK_TAG, 53);
        var table = level.getServer().getLootData().getLootTable(SavannaOrcCampProcessor.LOOT_TABLE);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin)).withLuck(17).create(LootContextParamSets.CHEST);
        Map<String, Integer> expected = quantities(OrcVillageLoot.budget(table, params, data));
        List<ItemStack> opened = new ArrayList<>();
        for (int order = 35; order >= 0; order--) {
            BlockPos pos = origin.east(order * 2);
            // Setting an identical chest state retains its old inventory when
            // this isolated QA world is reused. Start with a fresh block entity.
            level.removeBlockEntity(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 18);
            var chest = (ChestBlockEntity) level.getBlockEntity(pos);
            chest.getPersistentData().merge(data); chest.getPersistentData().putInt(OrcVillageLoot.INDEX, order);
            chest.setLootTable(SavannaOrcCampProcessor.LOOT_TABLE, order * 4729L);
            for (int slot = 0; slot < chest.getContainerSize(); slot++) opened.add(chest.getItem(slot).copy());
        }
        helper.assertTrue(!expected.isEmpty() && quantities(opened).equals(expected), "Actual reversed chest openings must sum to exactly eight old rolls without 36-fold loot inflation");
        for (int order = 0; order < 36; order++) {
            BlockPos pos = origin.east(order * 2);
            level.removeBlockEntity(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void palisadeUsesVanillaLowTallSidesAndMountDropsOnlyTwoBones(GameTestHelper helper) {
        var level = savanna(helper); BlockPos pos = helper.absolutePos(BlockPos.ZERO).atY(240);
        for (boolean roof : new boolean[]{false, true}) {
            BlockState above = (roof ? Blocks.ACACIA_PLANKS : Blocks.AIR).defaultBlockState();
            level.setBlock(pos.above(), above, 18);
            level.setBlock(pos.east(), Blocks.ACACIA_LOG.defaultBlockState(), 18);
            level.setBlock(pos.west(), Blocks.ACACIA_LOG.defaultBlockState(), 18);
            var actual = ModBlocks.ACACIA_PALISADE.get().defaultBlockState().setValue(WallBlock.EAST_WALL, WallSide.LOW).setValue(WallBlock.WEST_WALL, WallSide.LOW).setValue(WallBlock.UP, false);
            var expected = Blocks.COBBLESTONE_WALL.withPropertiesOf(actual).updateShape(Direction.UP, above, level, pos, pos.above());
            actual = actual.updateShape(Direction.UP, above, level, pos, pos.above());
            helper.assertTrue(actual.getValues().equals(expected.getValues()) && actual.getValue(WallBlock.EAST_WALL) == (roof ? WallSide.TALL : WallSide.LOW),
                    "Palisade must run vanilla wall side/post updates, including tall sides under full blocks");
        }
        var horse = ModEntities.SKELETON_HORSEMAN.get().create(level);
        horse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        try {
            helper.assertTrue(horse.getName().getString().equals("Skeleton Horse") && horse.getPassengers().size() == 1, "Mount keeps its skeleton horse name and separate rider");
            for (int looting : new int[]{0, 3}) {
                var drops = new ArrayList<ItemEntity>(); drops.add(new ItemEntity(level, 0, 0, 0, new ItemStack(Items.ARROW, 4)));
                var event = new LivingDropsEvent(horse, level.damageSources().generic(), drops, looting, true);
                CustomMobDrops.addConfiguredDrops(event);
                helper.assertTrue(event.getDrops().size() == 1 && event.getDrops().iterator().next().getItem().is(Items.BONE)
                                && event.getDrops().iterator().next().getItem().getCount() == 2,
                        "Mount drops exactly two bones regardless of looting and never keeps skeleton arrows");
                event.getDrops().forEach(Entity::discard);
            }
        } finally { horse.getPassengers().forEach(Entity::discard); horse.discard(); }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void everySpawnableCampFloorAndTentRoofHasBlockLight(GameTestHelper helper) {
        SavannaRoundTwoTests.darkCampExcludesAmbientUndeadAndLivestockButKeepsAuthoredResidents(helper);
    }

    private static void exportViews(ServerLevel level, BlockPos origin) throws java.io.IOException {
        level.setDayTime(6000);
        double x = origin.getX() + SavannaOrcVillage.CENTER + .5, y = origin.getY() + 2, z = origin.getZ() + SavannaOrcVillage.CENTER + .5;
        List<Map<String, Object>> views = List.of(
                view("camp_overhead", x, y + 110, z + 80, 180, 55),
                view("camp_gate", x, y + 5, z + SavannaOrcVillage.RADIUS + 9, 180, 8),
                view("camp_tent_interior", x, y + 1.65, z + 3.5, 180, 5),
                view("camp_pens", x + SavannaOrcVillage.RADIUS + 9, y + 14, z + 15, 90, 25),
                view("camp_palisade_and_shared_pen", x + SavannaOrcVillage.RADIUS + 6, y + 9, z + 26, 90, 20),
                view("camp_mushroom_patch", x + 7, y + 8, z + 55, 180, 50),
                view("camp_crop_corner", x + 65, y + 20, z + 65, 135, 45),
                view("camp_twelve_rows_and_cane", x + 59, y + 9, z + 25, 90, 35));
        var output = java.nio.file.Path.of("camp-compatibility-current-views.json").toAbsolutePath();
        java.nio.file.Files.writeString(output, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(views));
        level.getServer().saveEverything(true, true, true);
    }

    private static Map<String, Object> view(String label, double x, double y, double z, float yaw, float pitch) {
        return Map.of("label", label, "dimension", GameplayDimensions.SAVANNA.location().toString(), "x", x, "y", y,
                "z", z, "yaw", yaw, "pitch", pitch, "dayTime", 6000);
    }

    private static Map<String, Integer> quantities(Iterable<ItemStack> stacks) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) counts.merge(ForgeRegistries.ITEMS.getKey(stack.getItem()) + ":" + stack.getTag(), stack.getCount(), Integer::sum);
        return counts;
    }

    private static Map<BlockPos, BlockState> templateBlocks(ServerLevel level) {
        CompoundTag nbt = level.getStructureManager().getOrCreate(SavannaOrcCampPiece.TEMPLATE).save(new CompoundTag());
        var palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, BlockState> result = new HashMap<>();
        for (Tag tag : nbt.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) tag; var pos = block.getList("pos", Tag.TAG_INT);
            result.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state"))));
        }
        return result;
    }

    private static BlockState state(Map<BlockPos, BlockState> blocks, int x, int y, int z) { return blocks.getOrDefault(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState()); }
    private static ServerLevel savanna(GameTestHelper helper) { return helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA); }
    private static void place(ServerLevel level, SavannaOrcCampPiece piece, ChunkPos chunk) {
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1941),
                new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ()), chunk, piece.templatePosition());
    }
}
