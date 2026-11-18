package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.DragonFruitCropBlock;
import net.amurdza.examplemod.registry.RetainedDragonFruit;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DragonFruitPatches;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DragonFruitTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void freshDesertChunksGenerateTheCompleteWildFruitCluster(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        List<DragonFruitPatches.Patch> patches = List.of();
        for (int tile = 21; tile < 25 && patches.isEmpty(); tile++) patches = DragonFruitPatches.plan(desert, tile, -11);
        helper.assertTrue(patches.size() == 4, "Seeded desert fixtures must contain a complete sand/red-sand reservation");
        int total = 0;
        for (var patch : patches) {
            int chunkX = Math.floorDiv(patch.x(), 16), chunkZ = Math.floorDiv(patch.z(), 16);
            desert.getChunk(chunkX, chunkZ);
            int plants = 0;
            for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
                int top = desert.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int y = top - 1; y <= top + 1; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!desert.getBlockState(pos).is(RetainedDragonFruit.WILD.get())) continue;
                    var soil = desert.getBlockState(pos.below());
                    helper.assertTrue(soil.is(Blocks.SAND) || soil.is(Blocks.RED_SAND), "Generated fruit must stand on sand or red sand");
                    plants++;
                }
            }
            helper.assertTrue(plants == 5, "Fresh owner chunk must contain five wild dragon fruits: " + chunkX + "," + chunkZ + " has " + plants);
            total += plants;
        }
        helper.assertTrue(total == 20, "Fresh world generation must produce all twenty roots");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void fourCompletePatchesPreferTheCentral120AndStayInTheir240Tile(GameTestHelper helper) {
        for (int tile : new int[]{-2, -1, 0, 1}) {
            var patches = DragonFruitPatches.plan(91827, tile, -tile, (x, z) -> true);
            helper.assertTrue(patches.size() == 4 && patches.equals(DragonFruitPatches.plan(91827, tile, -tile, (x, z) -> true)),
                    "A dry tile must have one reproducible four-patch cluster");
            var chunks = new HashSet<ChunkPos>();
            for (var patch : patches) {
                helper.assertTrue(DragonFruitPatches.centralFootprint(patch, tile, -tile), "Suitable central land must take priority");
                chunks.add(new ChunkPos(Math.floorDiv(patch.x(), 16), Math.floorDiv(patch.z(), 16)));
            }
            helper.assertTrue(chunks.size() == 4 && chunks.stream().mapToInt(p -> p.x).max().orElseThrow()
                            - chunks.stream().mapToInt(p -> p.x).min().orElseThrow() == 1
                            && chunks.stream().mapToInt(p -> p.z).max().orElseThrow()
                            - chunks.stream().mapToInt(p -> p.z).min().orElseThrow() == 1,
                    "Four patches must occupy a 2x2 square of adjacent chunks");
            int minX = tile * 240, minZ = -tile * 240;
            var fallback = DragonFruitPatches.plan(91827, tile, -tile,
                    (x, z) -> x >= minX && x < minX + 32 && z >= minZ && z < minZ + 32);
            helper.assertTrue(fallback.size() == 4, "A complete sandy corner must be used when the center is underwater");
            for (var patch : fallback) helper.assertTrue(patch.x() - 2 >= minX && patch.x() + 3 < minX + 32
                            && patch.z() - 2 >= minZ && patch.z() + 3 < minZ + 32,
                    "Fallback footprints must stay inside their suitable owner chunks");
        }
        int[] queries = {0};
        helper.assertTrue(DragonFruitPatches.plan(1, -1, 0, (x, z) -> { queries[0]++; return false; }).isEmpty()
                        && queries[0] <= 240 * 240, "All-water tiles must stay empty and sample each column at most once");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void competingVegetationStillAllowsTwentyRootsWithoutDuplicateDecoration(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        Map<BlockPos, BlockState> writes = new HashMap<>();
        WorldGenLevel fixture = (WorldGenLevel) Proxy.newProxyInstance(DragonFruitTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    java.util.function.Function<BlockPos, BlockState> state = pos -> writes.getOrDefault(pos, fixtureState(pos));
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getSeed": return 91827L;
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : 71;
                        case "getBlockState": return state.apply((BlockPos) args[0]);
                        case "getFluidState": return state.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return state.apply((BlockPos) args[0]).isAir();
                        case "ensureCanWrite", "canSeeSky": return true;
                        case "getBlockEntity": return null;
                        case "getRawBrightness": return 15;
                        case "setBlock": writes.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        case "blockUpdated", "sendBlockUpdated", "updateNeighborsAt": return null;
                        default:
                            try { return method.invoke(level, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
        var patches = DragonFruitPatches.plan(91827, -1, 0, (x, z) -> true);
        for (var patch : patches) helper.assertTrue(DragonFruitPatches.place(fixture, patch) == 5, "Every dry sand patch must place exactly five plants");
        helper.assertTrue(writes.values().stream().filter(state -> state.is(RetainedDragonFruit.WILD.get())).count() == 20,
                "Dense competing dead bushes and tall grass must still permit exactly twenty wild plants");
        for (var entry : writes.entrySet()) if (entry.getValue().is(RetainedDragonFruit.WILD.get())) {
            helper.assertTrue(fixture.getBlockState(entry.getKey().above()).isAir(), "Replacing tall grass must remove its upper half");
        }
        var first = Map.copyOf(writes);
        for (var patch : patches) helper.assertTrue(DragonFruitPatches.place(fixture, patch) == 0, "Repeating decoration must not duplicate a patch");
        helper.assertTrue(writes.equals(first), "Duplicate decoration changed the original cluster");
        helper.succeed();
    }

    private static BlockState fixtureState(BlockPos pos) {
        boolean sand = Math.floorMod(pos.getX(), 2) == 0;
        if (pos.getY() < 71) return (sand ? Blocks.SAND : Blocks.RED_SAND).defaultBlockState();
        if (pos.getY() == 71) return (sand ? Blocks.DEAD_BUSH : Blocks.TALL_GRASS).defaultBlockState();
        if (pos.getY() == 72 && !sand) return Blocks.TALL_GRASS.defaultBlockState()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        return Blocks.AIR.defaultBlockState();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void retainedFruitSeedsAndCropLifecycleRemainUsable(GameTestHelper helper) {
        helper.assertTrue(RetainedDragonFruit.SEEDS.get() instanceof ItemNameBlockItem
                        && ((ItemNameBlockItem) RetainedDragonFruit.SEEDS.get()).getBlock() == RetainedDragonFruit.BUDDING.get(),
                "Seeds must plant the original budding crop");
        for (var item : List.of(RetainedDragonFruit.FRUIT.get(), RetainedDragonFruit.SEEDS.get(), RetainedDragonFruit.WILD_ITEM.get())) {
            helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(item)), "Restored farming item was hidden from the catalog");
        }
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new ResourceLocation("aoemod:retained/collectorsreap/dragon_fruit_seeds_from_pink_dragon_fruit")).isPresent(),
                "Harvested fruit must craft plantable seeds");
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        BlockPos pos = new BlockPos(0, 220, 0);
        var soil = Blocks.FARMLAND;
        BlockState old = desert.getBlockState(pos), oldSoil = desert.getBlockState(pos.below());
        try {
            desert.setBlock(pos.below(), soil.defaultBlockState(), 2);
            desert.setBlock(pos, RetainedDragonFruit.BUDDING.get().getStateForAge(3), 2);
            RetainedDragonFruit.BUDDING.get().growPastMaxAge(desert.getBlockState(pos), desert, pos, RandomSource.create(1));
            helper.assertTrue(desert.getBlockState(pos).is(RetainedDragonFruit.CROP.get()) && desert.getBlockState(pos).canSurvive(desert, pos),
                    "Budding crop must become a surviving fruiting crop on desert farmland");
            for (int i = 0; i < 4; i++) RetainedDragonFruit.CROP.get().performBonemeal(desert, RandomSource.create(i), pos, desert.getBlockState(pos));
            helper.assertTrue(desert.getBlockState(pos).getValue(DragonFruitCropBlock.AGE) == 3, "The restored fruiting crop must ripen");
        } finally {
            desert.setBlock(pos, old, 2);
            desert.setBlock(pos.below(), oldSoil, 2);
        }
        helper.succeed();
    }
}
