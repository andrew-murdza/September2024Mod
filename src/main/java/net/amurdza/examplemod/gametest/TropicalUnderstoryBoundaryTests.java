package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalUnderstoryBoundaryTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void mangroveGrassCoversColumnsOutsideTheOldChunkCentreGate(GameTestHelper helper)
            throws ReflectiveOperationException {
        var grass = TropicalVegetation.class.getDeclaredMethod("placeMoundGrass", WorldGenLevel.class,
                int.class, int.class, RandomSource.class);
        grass.setAccessible(true);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", 0, true);
        var layout = DimensionTerrain.tropicalLayout(world.getLevel().getChunkSource().randomState().router().temperature());
        var island = layout.island(0, 0);
        int minX = Math.floorDiv((int) island.x(), 16) * 16;
        int minZ = Math.floorDiv((int) island.z(), 16) * 16;
        helper.assertFalse(layout.mangroveLandChunk(minX >> 4, minZ >> 4),
                "The fixture must exercise a chunk rejected by the former centre-only grass gate");
        int placed = (int) grass.invoke(null, world, minX, minZ, RandomSource.create(7719));
        helper.assertTrue(placed >= 80 && blocks.size() == placed,
                "Every dry mangrove column must receive the scattered and expanded forest understory; actual=" + placed);
        helper.assertTrue(blocks.values().stream().anyMatch(state -> state.is(Blocks.GRASS))
                        && blocks.values().stream().anyMatch(state -> state.is(Blocks.FERN))
                        && blocks.values().stream().allMatch(state -> state.is(Blocks.GRASS) || state.is(Blocks.FERN)),
                "Mangrove uses rainforest grass and regular ferns, with every large fern replaced by a regular fern");
        int ferns = 0, grasses = 0;
        for (int seed = 0; seed < 16; seed++) {
            blocks.clear();
            grass.invoke(null, world, minX, minZ, RandomSource.create(seed));
            ferns += (int) blocks.values().stream().filter(state -> state.is(Blocks.FERN)).count();
            grasses += (int) blocks.values().stream().filter(state -> state.is(Blocks.GRASS)).count();
        }
        // Roughly 57 distinct scattered sites in 64 draws from 256. The two
        // fern passes previously averaged 57 roots (5/8 + 3/8 per site).
        helper.assertTrue(ferns >= 16 * 24 && ferns <= 16 * 33 && grasses >= 16 * 71,
                "Halve the small ferns and keep the original grass expansion: ferns=" + ferns + ", grass=" + grasses);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalFlowerAndCropPatchesKeepEveryBlockInTheirHabitat(GameTestHelper helper) {
        for (String habitat : new String[]{"mangrove_swamp", "rainforest"}) {
            String[] species = habitat.equals("mangrove_swamp")
                    ? new String[]{"minecraft:blue_orchid", "minecraft:pitcher_plant", "hearthandharvest:wild_cotton",
                    "farmersdelight:wild_cabbages"}
                    : new String[]{"minecraft:torchflower", "minecraft:rose_bush", "farmersdelight:wild_tomatoes",
                    "pineapple_delight:pineapple_wild_crop"};
            String neighbour = habitat.equals("mangrove_swamp") ? "rainforest" : "mangrove_swamp";
            for (String plant : species) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                Function<BlockPos, String> biome = pos -> pos.getX() < 8 ? habitat : neighbour;
                WorldGenLevel world = fixture(helper, blocks, biome, 0);
                int count = TropicalVegetation.placePatch(world, new BlockPos(8, 101, 8), habitat, plant,
                        RandomSource.create(781));
                helper.assertTrue(count > 0, "The legal half of a boundary patch must remain usable: " + plant);
                helper.assertTrue(blocks.keySet().stream().allMatch(pos -> biome.apply(pos).equals(habitat)),
                        "Every flower/crop block, including its upper half, must stay in " + habitat + ": " + plant);
            }
        }
        // A root check alone would allow these upper halves into the neighbouring
        // biome. Reject the whole plant without leaving a lower-half fragment.
        for (String habitat : new String[]{"mangrove_swamp", "rainforest"}) {
            String neighbour = habitat.equals("mangrove_swamp") ? "rainforest" : "mangrove_swamp";
            String plant = habitat.equals("mangrove_swamp") ? "minecraft:pitcher_plant" : "minecraft:rose_bush";
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, pos -> pos.getY() == 101 ? habitat : neighbour, 0);
            helper.assertTrue(TropicalVegetation.placePatch(world, new BlockPos(8, 101, 8), habitat, plant,
                    RandomSource.create(781)) == 0 && blocks.isEmpty(),
                    "A tall plant crossing the biome boundary must be rejected atomically: " + plant);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void wetSwampCropsAlsoKeepTheirUpperPartsInMangrove(GameTestHelper helper) {
        Map<BlockPos, BlockState> control = new HashMap<>();
        WorldGenLevel allowed = fixture(helper, control, pos -> "mangrove_swamp", 1);
        helper.assertTrue(TropicalVegetation.placeWetPatch(allowed, new BlockPos(8, 101, 8),
                "farmersdelight:wild_rice", RandomSource.create(51)) == 5 && control.size() == 10,
                "Wild rice must retain all five complete plants when both halves are in mangrove swamp");
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks,
                pos -> pos.getY() == 101 ? "mangrove_swamp" : "rainforest", 1);
        helper.assertTrue(TropicalVegetation.placeWetPatch(world, new BlockPos(8, 101, 8),
                "farmersdelight:wild_rice", RandomSource.create(51)) == 0 && blocks.isEmpty(),
                "A wild rice patch must not place upper crop blocks outside mangrove swamp");
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         Function<BlockPos, String> biomeAt, int waterDepth) {
        WorldGenLevel base = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", waterDepth, true);
        var biomes = base.registryAccess().registryOrThrow(Registries.BIOME);
        return (WorldGenLevel) Proxy.newProxyInstance(TropicalUnderstoryBoundaryTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBiome")) {
                        return biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME,
                                AOEMod.makeID(biomeAt.apply((BlockPos) args[0]))));
                    }
                    try {
                        return method.invoke(base, args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }
}
