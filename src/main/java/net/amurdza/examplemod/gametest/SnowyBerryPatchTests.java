package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.snow.CoreModule;
import snownee.snow.block.entity.SnowBlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyBerryPatchTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void completePatchesContainSixMatureSnowloggedBushes(GameTestHelper helper) {
        var fixture = fixture(helper, "snowy_forest");
        for (String species : new String[]{"minecraft:sweet_berry_bush", "aoemod:blue_berry_bush"}) {
            reset(fixture, 36);
            var patch = new SnowyVegetation.BerryPatch(fixture.origin().getX(), fixture.origin().getZ(), species, 489231L);
            helper.assertTrue(SnowyVegetation.placeBerryPatch(fixture.world(), patch) == 6,
                    "The real snow block-entity placement must complete six bushes for " + species);
            Set<BlockPos> planted = positions(fixture);
            helper.assertTrue(planted.size() == 6, "Six distinct roots must occupy the 6x6 patch: " + planted);
            var expected = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
            for (BlockPos pos : planted) {
                helper.assertTrue(fixture.level().getBlockState(pos).is(CoreModule.TILE_BLOCK.get()),
                        "The bush must generate in a Snow! Real Magic container at " + pos);
                helper.assertTrue(fixture.level().getBlockState(pos.below()).getValue(BlockStateProperties.SNOWY),
                        "Snowlogged berry patches must also use snowy grass-side textures");
                var snow = (SnowBlockEntity) fixture.level().getBlockEntity(pos);
                helper.assertTrue(snow != null && snow.getContainedState().is(expected)
                                && snow.getContainedState().getValue(BlockStateProperties.AGE_3) == 3,
                        "Snowlogging must preserve the mature " + species + " plant at " + pos);
                var saved = snow.saveWithFullMetadata();
                var restored = new SnowBlockEntity(pos, fixture.level().getBlockState(pos));
                restored.load(saved);
                helper.assertTrue(restored.getContainedState().equals(snow.getContainedState()),
                        "The snowlogged plant must survive a block-entity save/reload");
            }
            reset(fixture, 36);
            SnowyVegetation.placeBerryPatch(fixture.world(), patch);
            helper.assertTrue(positions(fixture).equals(planted), "The same seed and terrain must recreate the same patch");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void wrongBiomeAndInsufficientSpaceLeaveNoPartialPatch(GameTestHelper helper) {
        var fixture = fixture(helper, "snowy_forest");
        var patch = new SnowyVegetation.BerryPatch(fixture.origin().getX(), fixture.origin().getZ(), "aoemod:blue_berry_bush", 915L);
        reset(fixture, 5);
        helper.assertTrue(SnowyVegetation.placeBerryPatch(fixture.world(), patch) == 0,
                "Five valid roots cannot satisfy a six-bush patch");
        helper.assertTrue(positions(fixture).isEmpty(), "Failed preflight must not leave any berry bushes behind");
        reset(fixture, 36);
        for (String forbidden : new String[]{"dappled_forest", "mushroom_caves"}) {
            var wrongBiome = world(fixture.level(), forbidden);
            helper.assertTrue(SnowyVegetation.placeBerryPatch(wrongBiome, patch) == 0,
                    "Berries must not generate in " + forbidden);
            helper.assertTrue(positions(fixture).isEmpty(), "A rejected biome must remain unchanged");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void berryTilesScatterOnePatchOfEachSpeciesAcross120Squares(GameTestHelper helper) {
        Set<Integer> occupiedCells = new HashSet<>();
        for (long seed : new long[]{0L, 97132L, -4857392L}) {
            for (int tx = -2; tx <= 2; tx++) for (int tz = -2; tz <= 2; tz++) {
                var patches = SnowyVegetation.berryTilePatches(seed, tx, tz, (x, z) -> true);
                helper.assertTrue(patches.size() == 2 && patches.equals(SnowyVegetation.berryTilePatches(seed, tx, tz, (x, z) -> true)),
                        "Each suitable tile needs one deterministic patch of each berry");
                Set<String> species = new HashSet<>();
                for (var patch : patches) {
                    helper.assertTrue(species.add(patch.species()), "Only one patch of each species may occupy a tile");
                    int cx = Math.floorDiv(patch.x(), 16), cz = Math.floorDiv(patch.z(), 16);
                    helper.assertTrue(patch.x() >= tx * 120 && patch.x() + 5 < tx * 120 + 120
                                    && patch.z() >= tz * 120 && patch.z() + 5 < tz * 120 + 120,
                            "The complete six-square footprint must remain within its 120-square tile");
                    helper.assertTrue(SnowyVegetation.berryPatches(seed, cx, cz).contains(patch),
                            "Non-chunk-aligned tile seams must retain their owner chunk");
                    occupiedCells.add((Math.floorMod(patch.x(), 120) / 40) * 3 + Math.floorMod(patch.z(), 120) / 40);
                }
            }
        }
        helper.assertTrue(occupiedCells.size() == 9, "Patches must spread over entire tiles, including edges and corners");
        helper.assertTrue(SnowyVegetation.berryTilePatches(50881, 0, 0, (x, z) -> false).isEmpty(),
                "Unsuitable tiles must not force berries into lakes or another biome");
        helper.assertTrue(SnowyVegetation.berryTilePatches(50881, 0, 0,
                        (x, z) -> Math.floorMod(Math.floorDiv(x, 16), 2) == 0).size() == 2,
                "Separated suitable land may independently host the two berry patches");
        helper.succeed();
    }

    private record Fixture(ServerLevel level, WorldGenLevel world, BlockPos origin) { }

    private static Fixture fixture(GameTestHelper helper, String biome) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 0, 1));
        BlockPos origin = new BlockPos(anchor.getX(), level.getMaxBuildHeight() - 12, anchor.getZ());
        return new Fixture(level, world(level, biome), origin);
    }

    private static WorldGenLevel world(ServerLevel level, String name) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
        // Keep real chunks, heightmaps and block entities. Override only the biome
        // so the fixture cannot depend on the random terrain underneath the test.
        return (WorldGenLevel) Proxy.newProxyInstance(SnowyBerryPatchTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBiome")) return biome;
                    try {
                        return method.invoke(level, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void reset(Fixture fixture, int openRoots) {
        for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) {
            BlockPos pos = fixture.origin().offset(x, 0, z);
            fixture.level().setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
            for (int y = 0; y < 10; y++) fixture.level().setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 18);
            if (x * 6 + z >= openRoots) fixture.level().setBlock(pos, Blocks.STONE.defaultBlockState(), 18);
        }
    }

    private static Set<BlockPos> positions(Fixture fixture) {
        Set<BlockPos> positions = new HashSet<>();
        for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) {
            BlockPos pos = fixture.origin().offset(x, 0, z);
            if (fixture.level().getBlockEntity(pos) instanceof SnowBlockEntity snow
                    && !snow.getContainedState().isAir()) positions.add(pos);
        }
        return positions;
    }
}
