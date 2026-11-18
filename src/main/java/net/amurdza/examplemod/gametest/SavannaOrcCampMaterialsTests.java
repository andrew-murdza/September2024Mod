package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampProcessor;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaOrcCampMaterialsTests {
    private static final ResourceLocation VERSION_TWO_TEMPLATE = new ResourceLocation("aoemod:orc_settlement");

    private SavannaOrcCampMaterialsTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void versionTwoCampPaletteIsAllowedAndKeepsNativeShapesAndLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var template = level.getStructureManager().getOrCreate(VERSION_TWO_TEMPLATE);
        CompoundTag before = template.save(new CompoundTag());
        var original = contents(level, template);
        var converted = process(level, original, BlockPos.ZERO, new StructurePlaceSettings());
        helper.assertTrue(converted.size() == original.size(), "Camp conversion must retain every native block position");
        Map<String, Integer> counts = new HashMap<>();
        Map<BlockPos, StructureBlockInfo> convertedByPos = new HashMap<>();
        for (var info : converted) {
            var id = ForgeRegistries.BLOCKS.getKey(info.state().getBlock());
            counts.merge(id.toString(), 1, Integer::sum);
            convertedByPos.put(info.pos(), info);
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(level, info.pos(), info.state()),
                    "Camp material is still forbidden in Savanna: " + id + " at " + info.pos());
        }
        helper.assertTrue(counts.getOrDefault("minecraft:black_wool", 0) == 10
                        && counts.getOrDefault("minecraft:black_wool_slab", 0) == 130,
                "The boss tent and six small tents must retain their panels and half-height roof pieces");
        helper.assertTrue(counts.getOrDefault("alexscaves:smooth_bone_stairs", 0) == 16
                        && counts.getOrDefault("alexscaves:smooth_bone_slab", 0) == 12
                        && counts.getOrDefault("alexscaves:smooth_bone_wall", 0) == 0
                        && counts.getOrDefault("alexscaves:smooth_bone", 0) == 2,
                "All seven native roof horn profiles must survive; no tower decorations are copied");
        helper.assertTrue(counts.getOrDefault("quark:charcoal_block", 0) == 5
                        && counts.getOrDefault("minecraft:bone_block", 0) == 0,
                "The native charcoal fire remains, while the old bone pit and pond bones are absent");
        helper.assertTrue(counts.getOrDefault("minecraft:acacia_fence", 0) == 341
                        && counts.getOrDefault("minecraft:acacia_slab", 0) == 14
                        && counts.getOrDefault("aoemod:acacia_palisade", 0) == 774,
                "One continuous curved wall must retain log-post gaps; house and pen fences keep their native profiles");
        helper.assertTrue(counts.getOrDefault("minecraft:grass_block", 0) > 500
                        && counts.getOrDefault("minecraft:dirt_path", 0) > 300
                        && counts.getOrDefault("minecraft:rooted_dirt", 0) > 0
                        && counts.getOrDefault("minecraft:mud", 0) > 0,
                "The camp ground must have a grass surface over rooted dirt, dirt and mud");
        var seats = SavannaOrcCampProcessor.cushionSeats(template);
        helper.assertTrue(seats.size() == 4, "The four campfire seats must be converted to cushion positions");
        for (BlockPos seat : seats) helper.assertTrue(convertedByPos.get(seat).state().isAir()
                        && !convertedByPos.get(seat.below()).state().getShape(level, seat.below()).isEmpty(),
                "Each removed stone seat must have empty space and a supporting surface for a cushion");
        for (var info : original) {
            var result = convertedByPos.get(info.pos());
            if (info.state().is(Blocks.CHEST)) {
                helper.assertTrue(result.nbt() != null && result.nbt().getString("LootTable").equals(SavannaOrcCampProcessor.LOOT_TABLE.toString()),
                        "Every decorated house chest must use Savanna-only loot");
            } else helper.assertTrue(java.util.Objects.equals(info.nbt(), result.nbt()), "Camp decoration data must remain intact");
            if (info.state().is(Blocks.WATER)) helper.assertTrue(convertedByPos.get(info.pos().below()).state().is(Blocks.MUD),
                    "Crop irrigation and livestock ponds must have mud bottoms");
            if (info.state().hasProperty(BlockStateProperties.SLAB_TYPE)
                    && result.state().hasProperty(BlockStateProperties.SLAB_TYPE)) {
                helper.assertTrue(info.state().getValue(BlockStateProperties.SLAB_TYPE) == result.state().getValue(BlockStateProperties.SLAB_TYPE),
                        "Wool and bone slabs must preserve upper/lower/double states");
            }
            var outside = SavannaOrcCampProcessor.INSTANCE.processBlock(helper.getLevel(), BlockPos.ZERO, BlockPos.ZERO,
                    info, info, new StructurePlaceSettings());
            helper.assertTrue(outside.equals(info), "The Savanna palette must not alter native Overworld camps");
        }
        helper.assertTrue(before.equals(template.save(new CompoundTag())), "Camp conversion must not mutate the cached settlement template");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void versionTwoHousesAndStockPensKeepTheirSavedCurvedPalisade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var template = level.getStructureManager().getOrCreate(VERSION_TWO_TEMPLATE);
        var source = contents(level, template);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (var info : source) blocks.put(info.pos(), info.state());
        helper.assertTrue(template.getSize().getX() == 90 && template.getSize().getZ() == 78,
                "Saved version-two settlements must retain their original compound template");
        helper.assertTrue(source.stream().filter(info -> info.state().is(Blocks.CHEST)).count() == 8
                        && source.stream().filter(info -> info.state().is(Blocks.ACACIA_FENCE_GATE)).count() == 6
                        && source.stream().filter(info -> info.state().is(Blocks.WHEAT)).count() == 184
                        && source.stream().filter(info -> info.state().is(Blocks.CARROTS)).count() == 92
                        && source.stream().filter(info -> info.state().is(block("farmersdelight:cabbages"))).count() == 92,
                "All native house chests, two stock gates and both irrigated crop plots must remain");
        var interior = new java.util.HashSet<BlockPos>();
        var pending = new java.util.ArrayDeque<BlockPos>();
        pending.add(new BlockPos(45, 3, 39));
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (interior.contains(pos)) continue;
            BlockState state = blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            // Close the intentional south entrance during the boundary audit.
            if (pos.getX() >= 42 && pos.getX() <= 48 && pos.getZ() >= 71
                    || state.is(ModBlocks.ACACIA_PALISADE.get()) || state.is(Blocks.ACACIA_LOG)) continue;
            helper.assertTrue(pos.getX() > 0 && pos.getX() < 89 && pos.getZ() > 0 && pos.getZ() < 77,
                    "The curved perimeter has an unintended opening: " + pos);
            interior.add(pos);
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) pending.add(pos.relative(direction));
        }
        for (BlockPos center : SavannaOrcSettlement.HOUSE_CENTERS) {
            helper.assertTrue(interior.contains(center) && blocks.get(center).isAir() && blocks.get(center.above()).isAir()
                            && blocks.get(center.below()).is(Blocks.ACACIA_PLANKS),
                    "Each native house must be enclosed and have a usable interior floor: " + center);
        }
        for (int x : new int[]{14,26,64,76}) for (int z : new int[]{28,50}) {
            helper.assertTrue(interior.contains(new BlockPos(x, 3, z)), "Both complete stock pens belong inside the perimeter");
        }
        helper.assertTrue(blocks.get(new BlockPos(2, 3, 39)).is(ModBlocks.ACACIA_PALISADE.get())
                        || blocks.get(new BlockPos(2, 3, 39)).is(Blocks.ACACIA_LOG), "The west wall follows the ellipse");
        helper.assertTrue(!blocks.containsKey(new BlockPos(2, 3, 3)), "The perimeter must curve inward at the corners, not form a rectangle");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void transformedCampKeepsPalisadeConnectionsAndLayeredGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var palisade = block("orcz:palisadedarkoak").defaultBlockState()
                .setValue(WallBlock.NORTH_WALL, WallSide.TALL).setValue(WallBlock.EAST_WALL, WallSide.LOW)
                .setValue(WallBlock.SOUTH_WALL, WallSide.NONE).setValue(WallBlock.WEST_WALL, WallSide.NONE);
        List<StructureBlockInfo> original = new ArrayList<>();
        for (int y = 0; y < 4; y++) original.add(new StructureBlockInfo(new BlockPos(0, y, 0), Blocks.COARSE_DIRT.defaultBlockState(), null));
        original.add(new StructureBlockInfo(new BlockPos(1, 4, 0), palisade, null));
        BlockPos origin = new BlockPos(19, 80, -31);
        for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
            var settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror);
            var converted = process(level, original, origin, settings);
            Block[] layers = {Blocks.DIRT, Blocks.ROOTED_DIRT, Blocks.ROOTED_DIRT, Blocks.GRASS_BLOCK};
            for (int y = 0; y < 4; y++) {
                helper.assertTrue(converted.get(y).pos().equals(StructureTemplate.calculateRelativePosition(settings, new BlockPos(0, y, 0)).offset(origin))
                                && converted.get(y).state().is(layers[y]),
                        "Ground layers must follow the original column through every rotation and mirror");
            }
            BlockState actual = converted.get(4).state().mirror(mirror).rotate(rotation);
            BlockState expected = ModBlocks.ACACIA_PALISADE.get().withPropertiesOf(palisade).mirror(mirror).rotate(rotation);
            helper.assertTrue(actual.equals(expected), "Acacia palisades preserve vanilla low/tall connections through rotation and mirroring");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void palisadesPreserveVanillaLowTallConnectionsAndPosts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var sides = List.of(WallBlock.NORTH_WALL, WallBlock.EAST_WALL, WallBlock.SOUTH_WALL, WallBlock.WEST_WALL);
        for (int mask = 0; mask < 16; mask++) for (WallSide arm : new WallSide[]{WallSide.LOW, WallSide.TALL}) for (boolean post : new boolean[]{false, true}) {
            BlockState source = ModBlocks.ACACIA_PALISADE.get().defaultBlockState().setValue(WallBlock.UP, post);
            for (int side = 0; side < 4; side++) source = source.setValue(sides.get(side), (mask & 1 << side) == 0 ? WallSide.NONE : arm);
            var info = new StructureBlockInfo(BlockPos.ZERO, source, null);
            BlockState actual = process(level, List.of(info), BlockPos.ZERO, new StructurePlaceSettings()).get(0).state();
            helper.assertTrue(actual.equals(source), "Template conversion must preserve every vanilla wall connection/post state, mask=" + mask);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void stackedPalisadesMatchVanillaWallsAfterNeighborUpdates(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 10, 2));
        for (int copy = 0; copy < 2; copy++) {
            BlockPos start = origin.south(copy * 6);
            var wall = copy == 0 ? ModBlocks.ACACIA_PALISADE.get() : Blocks.COBBLESTONE_WALL;
            for (int x = -1; x <= 5; x++) for (int y = 0; y <= 4; y++) for (int z = -1; z <= 1; z++)
                level.setBlock(start.offset(x, y, z), Blocks.AIR.defaultBlockState(), 18);
            for (int y = 0; y < 3; y++) for (int x = 0; x < 5; x++) level.setBlock(start.offset(x, y, 0),
                    wall.defaultBlockState().setValue(WallBlock.WEST_WALL, x == 0 ? WallSide.NONE : WallSide.LOW)
                            .setValue(WallBlock.EAST_WALL, x == 4 ? WallSide.NONE : WallSide.LOW).setValue(WallBlock.UP, x == 0 || x == 4), 3);
            level.setBlock(start.offset(2, 3, 0), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(start.offset(0, 0, 1), wall.defaultBlockState(), 3);
        }
        for (int y = 0; y < 3; y++) for (int x = 0; x < 5; x++) helper.assertTrue(
                level.getBlockState(origin.offset(x, y, 0)).getValues().equals(level.getBlockState(origin.offset(x, y, 6)).getValues()),
                "Stacked palisades must follow the same tall arms and posts as vanilla walls after caps and corners update");
        level.setBlock(origin.offset(4, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(4, 0, 6), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(origin.offset(3, 0, 0)).getValues().equals(level.getBlockState(origin.offset(3, 0, 6)).getValues()),
                "Removing a neighbor must disconnect the palisade exactly like a vanilla wall");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rebuiltTemplateKeepsOuterBushesAndThreeByThreeMudBottomPonds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var template = level.getStructureManager().getOrCreate(VERSION_TWO_TEMPLATE);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (var info : contents(level, template)) blocks.put(info.pos(), info.state());
        int bushes = 0;
        for (var entry : blocks.entrySet()) {
            if (!entry.getValue().is(Blocks.ACACIA_LEAVES) || entry.getKey().getY() > 4) continue;
            bushes++;
            BlockPos pos = entry.getKey();
            double dx = (pos.getX() - 45) / 41.3, dz = (pos.getZ() - 39) / 34.3;
            helper.assertTrue(dx * dx + dz * dz >= 1, "The inside of the palisade must contain no edging bushes");
        }
        helper.assertTrue(bushes > 100, "The outer palisade bushes must remain");
        for (int minX : new int[]{15, 65}) for (int x = minX; x < minX + 3; x++) for (int z = 47; z <= 49; z++) {
            helper.assertTrue(blocks.get(new BlockPos(x, 2, z)).is(Blocks.WATER)
                            && blocks.get(new BlockPos(x, 1, z)).is(Blocks.MUD),
                    "Each livestock pond must have nine water cells over nine mud cells");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bothHornedBuildingsHaveMirroredBoneProfilesInEveryCampTemplate(GameTestHelper helper) {
        var level = helper.getLevel();
        for (String biome : List.of("normal", "desert", "snow")) {
            int dy = biome.equals("normal") ? 0 : 1;
            int dz = biome.equals("snow") ? -1 : 0;
            var template = level.getStructureManager().getOrCreate(new ResourceLocation("orcz", "orc_camp_" + biome + "_loot"));
            var blocks = new HashMap<BlockPos, BlockState>();
            for (var info : contents(level, template)) blocks.put(info.pos(), info.state());
            long palisades = blocks.values().stream().filter(state -> state.is(ModBlocks.ACACIA_PALISADE.get())).count();
            helper.assertTrue(palisades > 250 && blocks.values().stream().noneMatch(state ->
                            state.is(block("orcz:palisadedarkoak")) || state.is(block("orcz:pallisade"))),
                    "Every native camp biome must use acacia palisades: " + biome);
            helper.assertTrue(blocks.values().stream().noneMatch(state -> {
                var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                return id.getNamespace().equals("orcz") && (id.getPath().startsWith("orcsymbol")
                        || id.getPath().startsWith("orc_wood") || id.getPath().equals("scrapblock"));
            }), "No camp may reintroduce removed symbols, wood, or scrap: " + biome);
            var stairs = block("alexscaves:smooth_bone_stairs").defaultBlockState();
            var slab = block("alexscaves:smooth_bone_slab").defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
            // Small building: a low inner arm and an outward-facing raised tip.
            assertHornPair(helper, blocks, new BlockPos(12, 8 + dy, 32 + dz), new BlockPos(18, 8 + dy, 32 + dz),
                    stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.WEST)
                            .setValue(BlockStateProperties.HALF, Half.BOTTOM), Mirror.FRONT_BACK);
            assertHornPair(helper, blocks, new BlockPos(13, 8 + dy, 32 + dz), new BlockPos(17, 8 + dy, 32 + dz), slab, Mirror.FRONT_BACK);
            // Large building: two stair steps joined by the reference's full middle block.
            assertHornPair(helper, blocks, new BlockPos(24, 9 + dy, 13 + dz), new BlockPos(24, 9 + dy, 23 + dz),
                    block("alexscaves:smooth_bone").defaultBlockState(), Mirror.LEFT_RIGHT);
            assertHornPair(helper, blocks, new BlockPos(24, 10 + dy, 13 + dz), new BlockPos(24, 10 + dy, 23 + dz),
                    stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)
                            .setValue(BlockStateProperties.HALF, Half.BOTTOM), Mirror.LEFT_RIGHT);
            assertHornPair(helper, blocks, new BlockPos(24, 9 + dy, 14 + dz), new BlockPos(24, 9 + dy, 22 + dz),
                    stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)
                            .setValue(BlockStateProperties.HALF, Half.BOTTOM), Mirror.LEFT_RIGHT);
        }
        helper.succeed();
    }

    private static void assertHornPair(GameTestHelper helper, Map<BlockPos, BlockState> blocks, BlockPos left, BlockPos right,
                                      BlockState expectedLeft, Mirror mirror) {
        helper.assertTrue(expectedLeft.equals(blocks.get(left)) && expectedLeft.mirror(mirror).equals(blocks.get(right)),
                "Bone horn pair must preserve its reference profile and mirror about the building center: " + left + " / " + right);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void buriedBushesBecomeGroundWhileExposedBushesRemain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockPos bush = new BlockPos(1, 1, 1);
        List<StructureBlockInfo> original = new ArrayList<>();
        original.add(new StructureBlockInfo(bush, Blocks.OAK_LEAVES.defaultBlockState(), null));
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            original.add(new StructureBlockInfo(bush.relative(direction), Blocks.GRASS_BLOCK.defaultBlockState(), null));
        }
        var converted = process(level, original, new BlockPos(0, 280, 0), new StructurePlaceSettings());
        helper.assertTrue(converted.get(0).state().is(Blocks.GRASS_BLOCK), "Four ground neighbors must fill a buried bush with lawn");
        original.set(1, new StructureBlockInfo(original.get(1).pos(), Blocks.AIR.defaultBlockState(), null));
        converted = process(level, original, new BlockPos(0, 280, 0), new StructurePlaceSettings());
        helper.assertTrue(converted.get(0).state().is(Blocks.ACACIA_LEAVES), "A bush with an exposed side must remain");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void perimeterBushesReadMissingTemplateNeighborsFromTransformedTerrain(GameTestHelper helper) {
        ServerLevel savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        Map<BlockPos, BlockState> terrain = new HashMap<>();
        java.util.Set<BlockPos> reads = new java.util.HashSet<>();
        WorldGenLevel world = (WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("hasChunkAt")) return true;
                    if (method.getName().equals("getBlockState")) {
                        BlockPos pos = ((BlockPos) args[0]).immutable();
                        reads.add(pos);
                        return terrain.getOrDefault(pos, Blocks.AIR.defaultBlockState());
                    }
                    try {
                        return method.invoke(savanna, args);
                    } catch (java.lang.reflect.InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
        BlockPos bush = new BlockPos(1, 1, 1), origin = new BlockPos(37, 280, -21);
        List<StructureBlockInfo> original = new ArrayList<>();
        original.add(new StructureBlockInfo(bush, Blocks.OAK_LEAVES.defaultBlockState(), null));
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (direction != net.minecraft.core.Direction.EAST) {
                original.add(new StructureBlockInfo(bush.relative(direction), Blocks.GRASS_BLOCK.defaultBlockState(), null));
            }
        }
        for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
            var settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror);
            BlockPos outside = StructureTemplate.calculateRelativePosition(settings, bush.east()).offset(origin);
            terrain.clear();
            reads.clear();
            terrain.put(outside, Blocks.DIRT.defaultBlockState());
            var converted = process(world, original, origin, settings);
            helper.assertTrue(converted.get(0).state().is(Blocks.GRASS_BLOCK) && reads.contains(outside),
                    "A perimeter bush's missing fourth template neighbor must use the rotated/mirrored terrain position");
            terrain.clear();
            converted = process(world, original, origin, settings);
            helper.assertTrue(converted.get(0).state().is(Blocks.ACACIA_LEAVES),
                    "The same perimeter bush must remain when the outside terrain leaves one side exposed");
        }
        helper.succeed();
    }

    private static List<StructureBlockInfo> process(ServerLevelAccessor level, List<StructureBlockInfo> source,
            BlockPos origin, StructurePlaceSettings settings) {
        List<StructureBlockInfo> converted = new ArrayList<>();
        for (var info : source) {
            var current = new StructureBlockInfo(StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(origin), info.state(), info.nbt());
            converted.add(SavannaOrcCampProcessor.INSTANCE.processBlock(level, origin, BlockPos.ZERO, info, current, settings));
        }
        return SavannaOrcCampProcessor.INSTANCE.finalizeProcessing(level, origin, BlockPos.ZERO, source, converted, settings);
    }

    private static List<StructureBlockInfo> contents(ServerLevel level, StructureTemplate template) {
        CompoundTag data = template.save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        List<StructureBlockInfo> result = new ArrayList<>();
        for (Tag entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            var position = block.getList("pos", Tag.TAG_INT);
            result.add(new StructureBlockInfo(new BlockPos(position.getInt(0), position.getInt(1), position.getInt(2)),
                    NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state"))),
                    block.contains("nbt", Tag.TAG_COMPOUND) ? block.getCompound("nbt") : null));
        }
        return result;
    }

    private static Block block(String id) { return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)); }
}
