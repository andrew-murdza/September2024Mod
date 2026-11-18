package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.MansionRoomCatalogue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember18TemplateTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void oakRoomsKeepSavedVersionsAndReplaceTheRemovedLibraries(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var revised = manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft")
                && id.getPath().startsWith("woodland_mansion/aoe_v6_")).toList();
        h.assertTrue(revised.size() >= 79, "Every room, wall, hall, staircase and roof must have its own new identity");
        for (var id : revised) {
            h.assertTrue(cells(manager.getOrCreate(id)).values().stream().noneMatch(state -> state.is(Blocks.BIRCH_PLANKS)),
                    "All newly generated mansion planks must be oak: " + id);
        }
        h.assertTrue(cells(template(manager, "aoe_compact_ceiling_v3")).values().stream().anyMatch(state -> state.is(Blocks.BIRCH_PLANKS)),
                "Saved earlier mansions must keep their existing birch ceiling resource");
        h.assertTrue(MansionRoomCatalogue.newTemplate("1x1_b4").equals("aoe_v6_1x1_b3")
                        && !MansionRoomCatalogue.SMALL.contains("1x1_b4") && !MansionRoomCatalogue.GROUND_SMALL.contains("1x1_b4"),
                "The small library must always select a bedroom instead");
        var large = cells(template(manager, MansionRoomCatalogue.BEDROOM_LARGE));
        h.assertTrue(count(large, Blocks.BOOKSHELF) == 0 && count(large, Blocks.WHITE_WOOL) == 48
                        && count(large, Blocks.RED_CARPET) == 36 && MansionRoomCatalogue.bedCapacity(MansionRoomCatalogue.BEDROOM_LARGE) == 8,
                "The booth library must become four three-wide beds, counting as eight sleeping places");
        for (int z = 0; z < 15; z++) for (int y = 1; y <= 3; y++)
            h.assertTrue(at(large, 7, y, z).isAir(), "The new bedroom must retain its central walking aisle");
        var mapRoom = cells(template(manager, MansionRoomCatalogue.MAP_ROOM));
        h.assertTrue(count(mapRoom, Blocks.BOOKSHELF) == 0, "The asymmetric map-room bookshelf is removed");
        for (int x = 5; x <= 9; x++) for (int y = 1; y <= 5; y++)
            h.assertTrue(at(mapRoom, x, y, 14).isAir(), "The bookshelf display's wooden surround must be removed too");
        var bedroom = cells(template(manager, "aoe_v6_1x2_d1"));
        h.assertTrue(at(bedroom, 6, 1, 0).isAir() && at(bedroom, 6, 2, 0).isAir(),
                "Both marked bedroom blocks, including the shelf support, must be gone");
        h.assertTrue(count(cells(template(manager, "aoe_compact_2x2_b3")), Blocks.BOOKSHELF) > 0,
                "The old saved booth library template remains unchanged");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void solidRoofAndWindowRailsKeepCornerMasonryAndAddTheSecondCornice(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        for (String name : new String[]{"roof_front", "roof_corner", "roof_inner_corner"}) {
            var roof = cells(template(manager, "aoe_v6_" + name));
            var oldRoof = cells(template(manager, "aoe_compact_" + name + "_v5"));
            var heights = new HashMap<BlockPos, Integer>();
            roof.forEach((pos, state) -> {
                if (!state.isAir()) heights.merge(new BlockPos(pos.getX(), 0, pos.getZ()), pos.getY(), Math::max);
            });
            heights.forEach((column, top) -> {
                for (int y = 0; y <= top; y++)
                    h.assertTrue(!at(roof, column.getX(), y, column.getZ()).isAir(),
                            "The roof stair enclosure must contain solid wood, including missing template cells: " + name + "/" + column);
            });
            h.assertTrue(name.equals("roof_front") ? count(roof, Blocks.COBBLESTONE) == 0
                            : count(roof, Blocks.COBBLESTONE) == count(oldRoof, Blocks.COBBLESTONE),
                    "Only the straight top-floor band disappears; original corner masonry remains");
        }
        var window = cells(template(manager, "aoe_v6_wall_window"));
        var oldWindow = cells(template(manager, "wall_window"));
        for (int z = 1; z <= 5; z++) h.assertTrue(at(window, 0, 1, z).is(Blocks.OAK_LOG)
                        && at(window, 0, 1, z).getValue(BlockStateProperties.AXIS) == net.minecraft.core.Direction.Axis.X,
                "The lower interior window boundary must match the upper end-grain log border");
        h.assertTrue(count(window, Blocks.GLASS_PANE) == count(oldWindow, Blocks.GLASS_PANE)
                        && count(window, Blocks.COBBLESTONE) == count(oldWindow, Blocks.COBBLESTONE)
                        && count(window, Blocks.COBBLESTONE_WALL) == count(oldWindow, Blocks.COBBLESTONE_WALL),
                "Regular windows and the lower-floor exterior cobblestone decorations stay the same size");
        var room = cells(template(manager, "aoe_v6_1x2_c4"));
        for (int inset = 0; inset <= 1; inset++) for (int x = inset; x < 7 - inset; x++) for (int z = inset; z < 15 - inset; z++) {
            if (x != inset && x != 6 - inset && z != inset && z != 14 - inset) continue;
            var stair = at(room, x, 5 + inset, z);
            h.assertTrue(stair.is(Blocks.OAK_STAIRS) && stair.getValue(BlockStateProperties.HALF) == Half.TOP,
                    "The chandelier room needs two complete inset courses of overhead stairs");
            h.assertTrue(at(room, x, 7, z).is(Blocks.OAK_PLANKS), "The cornice must not reopen its former post gaps");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 500)
    public static void vanillaEntranceAndNewStairTorchesSurvivePlacementInEveryRotation(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var manager = level.getStructureManager();
        var vanilla = cells(template(manager, "entrance"));
        var portal = cells(template(manager, MansionRoomCatalogue.ENTRANCE));
        var platform = cells(template(manager, MansionRoomCatalogue.ENTRANCE_PLATFORM));
        for (var entry : portal.entrySet()) {
            var pos = entry.getKey();
            var expected = oakPlanks(at(vanilla, 16 - pos.getZ(), pos.getY(), pos.getX() + 4).rotate(Rotation.COUNTERCLOCKWISE_90));
            h.assertTrue(entry.getValue().equals(expected), "The first two doorway rows must match the real vanilla entrance");
        }
        for (var entry : platform.entrySet()) {
            var pos = entry.getKey();
            var expected = oakPlanks(at(vanilla, 20 - pos.getZ(), pos.getY(), pos.getX() + 2).rotate(Rotation.COUNTERCLOCKWISE_90));
            h.assertTrue(entry.getValue().equals(expected), "The exterior stairs, slabs, posts and carpet must come from vanilla");
        }
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID("plains")));
        int sample = 0;
        for (Rotation rotation : Rotation.values()) {
            var center = new ChunkPos(5100 + sample++ * 8, 5100);
            var origin = center.getMiddleBlockPosition(220);
            var chunks = new ArrayList<ChunkAccess>();
            for (int z = center.z - 2; z <= center.z + 2; z++) for (int x = center.x - 2; x <= center.x + 2; x++) {
                var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level,
                        level.registryAccess().registryOrThrow(Registries.BIOME), null);
                chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> biome, level.getChunkSource().randomState().sampler());
                chunk.setStatus(ChunkStatus.CARVERS);
                chunks.add(chunk);
            }
            var region = new WorldGenRegion(level, chunks, ChunkStatus.FEATURES, 2);
            var clip = new BoundingBox(origin.getX() - 28, 218, origin.getZ() - 28, origin.getX() + 28, 240, origin.getZ() + 28);
            var platformOrigin = origin.offset(MansionRoomCatalogue.ENTRANCE_PLATFORM_OFFSET.rotate(rotation));
            for (var piece : new WoodlandMansionPiece[]{
                    new WoodlandMansionPiece(manager, MansionRoomCatalogue.ENTRANCE, origin, rotation),
                    new WoodlandMansionPiece(manager, MansionRoomCatalogue.ENTRANCE_PLATFORM, platformOrigin, rotation)})
                piece.postProcess(region, level.structureManager().forWorldGenRegion(region), level.getChunkSource().getGenerator(),
                        RandomSource.create(0), clip, center, origin);
            int stairs = 0;
            for (var entry : platform.entrySet()) if (entry.getValue().is(Blocks.COBBLESTONE_STAIRS)) {
                var pos = entry.getKey().rotate(rotation).offset(platformOrigin);
                var actual = region.getBlockState(pos);
                var expected = entry.getValue().rotate(rotation);
                h.assertTrue(actual.is(Blocks.COBBLESTONE_STAIRS)
                                && actual.getValue(BlockStateProperties.HORIZONTAL_FACING) == expected.getValue(BlockStateProperties.HORIZONTAL_FACING)
                                && actual.getValue(BlockStateProperties.HALF) == expected.getValue(BlockStateProperties.HALF),
                        "The Plains material processor must preserve the actual vanilla platform's stairs: " + rotation + "/" + pos);
                stairs++;
            }
            h.assertTrue(stairs == 7, "Every side must have all seven vanilla platform stair blocks");
            for (int z = -3; z <= 1; z++) for (int y = 2; y <= 3; y++)
                h.assertTrue(region.isEmptyBlock(new BlockPos(3, y, z).rotate(rotation).offset(origin)),
                        "The restored outer platform and doorway must retain a clear central entrance");
            var stairOrigin = origin.offset(new BlockPos(16, 0, 0).rotate(rotation));
            var stair = new WoodlandMansionPiece(manager, "aoe_v6_stair_1x1", stairOrigin, rotation);
            stair.postProcess(region, level.structureManager().forWorldGenRegion(region), level.getChunkSource().getGenerator(),
                    RandomSource.create(0), clip, center, origin);
            for (int x : new int[]{2, 4}) {
                var torch = new BlockPos(x, 3, 0).rotate(rotation).offset(stairOrigin);
                var state = region.getBlockState(torch);
                h.assertTrue(state.is(Blocks.WALL_TORCH) && state.canSurvive(region, torch),
                        "Both added lower-doorway torches must remain attached in every rotation");
            }
        }
        h.succeed();
    }

    private static BlockState oakPlanks(BlockState state) {
        return state.is(Blocks.BIRCH_PLANKS) ? Blocks.OAK_PLANKS.defaultBlockState() : state;
    }

    private static long count(Map<BlockPos, BlockState> cells, net.minecraft.world.level.block.Block block) {
        return cells.values().stream().filter(state -> state.is(block)).count();
    }

    private static BlockState at(Map<BlockPos, BlockState> cells, int x, int y, int z) {
        return cells.getOrDefault(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
    }

    private static StructureTemplate template(StructureTemplateManager manager, String name) {
        return manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + name));
    }

    private static Map<BlockPos, BlockState> cells(StructureTemplate template) {
        var data = template.save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var result = new HashMap<BlockPos, BlockState>();
        for (var entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) entry;
            var pos = block.getList("pos", Tag.TAG_INT);
            result.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(block.getInt("state"))));
        }
        return result;
    }
}
