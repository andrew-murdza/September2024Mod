package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.MushroomCaveVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MushroomCaveVegetationTests {
    private static final Set<String> SMALL = Set.of("minecraft:brown_mushroom", "minecraft:red_mushroom",
            "quark:glow_shroom", "twilightforest:mushgloom");

    @GameTest(template = "plant_gameplay_empty")
    public static void everyChunkRestoresItsPatchWithAnEvenFourSpeciesMix(GameTestHelper helper) {
        for (long seed : new long[]{0, 12345, -8712331}) {
            int[] species = new int[4];
            int selected = 0;
            for (int x = -64; x < 64; x++) for (int z = -64; z < 64; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                var plan = MushroomCaveVegetation.plan(seed, chunk);
                helper.assertTrue(plan.equals(MushroomCaveVegetation.plan(seed, chunk)), "Chunk choices must survive generation-order changes");
                if (plan.selected()) { selected++; species[plan.species()]++; }
            }
            helper.assertTrue(selected == 16384, "Suitable chunks must no longer lose three quarters of their mushroom patches: " + selected);
            for (int count : species) helper.assertTrue(count > 3800 && count < 4400,
                    "The existing four mushroom species must retain an even mixture: " + count);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1000)
    public static void restoredPatchesHaveOneOffsetGiantAndNineSmallMushrooms(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var configured = snowy.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:mushroom_caves/floor/forest_floor_full"));
        Set<Integer> covered = new HashSet<>();
        Set<Integer> localSmallColumns = new HashSet<>();
        Set<Integer> giantColumns = new HashSet<>();
        for (int x = -50; x < 50 && covered.size() < 4; x++) {
            ChunkPos chunk = new ChunkPos(x, -3);
            var plan = MushroomCaveVegetation.plan(12345, chunk);
            if (!plan.selected() || covered.contains(plan.species())) continue;
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = cave(helper, blocks, false);
            helper.assertTrue(configured.place(world, snowy.getChunkSource().getGenerator(), RandomSource.create(13), chunk.getWorldPosition()),
                    "A suitable two-floor cave must receive its full-size patch");
            long small = blocks.values().stream().filter(MushroomCaveVegetationTests::small).count();
            long stems = blocks.entrySet().stream().filter(entry -> (entry.getKey().getY() == -27 || entry.getKey().getY() == -9)
                    && MushroomCaveVegetation.isMushroom(entry.getValue()) && !small(entry.getValue())).count();
            helper.assertTrue(small == 9 && stems == 1,
                    "Restore one giant and nine small mushrooms per suitable chunk: " + plan.species() + "/" + small + "/" + stems);
            for (var entry : blocks.entrySet()) {
                helper.assertTrue(new ChunkPos(entry.getKey()).equals(chunk), "Complete giant caps and small mushrooms must stay within their patch chunk");
                if (small(entry.getValue())) localSmallColumns.add((entry.getKey().getX() & 15) * 16 + (entry.getKey().getZ() & 15));
                if ((entry.getKey().getY() == -27 || entry.getKey().getY() == -9)
                        && MushroomCaveVegetation.isMushroom(entry.getValue()) && !small(entry.getValue())) {
                    int localX = entry.getKey().getX() & 15, localZ = entry.getKey().getZ() & 15;
                    helper.assertTrue(localX >= 3 && localX <= 12 && localZ >= 3 && localZ <= 12,
                            "Giant stems must preserve their previous X/Z offset range");
                    giantColumns.add(localX * 16 + localZ);
                }
            }
            Map<BlockPos, BlockState> before = Map.copyOf(blocks);
            configured.place(world, snowy.getChunkSource().getGenerator(), RandomSource.create(984), chunk.getWorldPosition());
            helper.assertTrue(before.equals(blocks), "A repeated floor pass must not create a second patch");
            covered.add(plan.species());
        }
        helper.assertTrue(covered.size() == 4 && localSmallColumns.size() >= 24 && giantColumns.size() > 1,
                "Exercise every native giant species and visibly varying scattered columns, rather than a fixed small-mushroom grid");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void everyLowCeilingChunkGetsSmallMushroomsWithoutPartialGiants(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        for (int x = -8; x < 8; x++) {
            ChunkPos chunk = new ChunkPos(x, 5);
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = cave(helper, blocks, true);
            helper.assertTrue(MushroomCaveVegetation.place(world, snowy.getChunkSource().getGenerator(), chunk),
                    "Every suitable chunk receives mushrooms, even when the giant cannot fit");
            helper.assertTrue(blocks.values().stream().filter(MushroomCaveVegetationTests::small).count() == 9
                            && blocks.values().stream().noneMatch(state -> MushroomCaveVegetation.isMushroom(state) && !small(state)),
                    "A failed giant under a low ceiling leaves nine small mushrooms, never partial caps");
        }
        var biome = snowy.registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:mushroom_caves"));
        for (var step : biome.getGenerationSettings().features()) for (var feature : step) {
            String id = feature.unwrapKey().map(key -> key.location().toString()).orElse("");
            if (!(feature.value().feature().value().feature() instanceof net.minecraft.world.level.levelgen.feature.OreFeature)
                    && (id.contains("mushroom") || id.contains("mushgloom") || id.contains("glow_shroom"))) {
                helper.assertTrue(id.equals("aoemod:mushroom_caves/forest_floor"), "A second mushroom generation path duplicates the restored patch: " + id);
            }
        }
        helper.succeed();
    }

    private static boolean small(BlockState state) {
        return SMALL.contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());
    }

    @SuppressWarnings("unchecked")
    private static WorldGenLevel cave(GameTestHelper helper, Map<BlockPos, BlockState> blocks, boolean lowCeiling) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:mushroom_caves")));
        java.util.function.Function<BlockPos, BlockState> state = pos -> blocks.getOrDefault(pos,
                pos.getY() == -28 || pos.getY() == -10 ? Blocks.MYCELIUM.defaultBlockState()
                        : pos.getY() < -28 || lowCeiling && pos.getY() != -27 && pos.getY() != -9
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        RandomSource random = RandomSource.create(12345);
        return (WorldGenLevel) Proxy.newProxyInstance(MushroomCaveVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getSeed": return 12345L;
                        case "getRandom": return random;
                        case "getBiome": return biome;
                        case "getBlockState": return state.apply((BlockPos) args[0]);
                        case "getFluidState": return state.apply((BlockPos) args[0]).getFluidState();
                        case "getBlockEntity": return null;
                        case "getRawBrightness": case "getMaxLocalRawBrightness": return 0;
                        case "isEmptyBlock": return state.apply((BlockPos) args[0]).isAir();
                        case "isStateAtPosition": return ((Predicate<BlockState>) args[1]).test(state.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(state.apply((BlockPos) args[0]).getFluidState());
                        case "ensureCanWrite": case "hasChunk": return true;
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try { return method.invoke(level, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
