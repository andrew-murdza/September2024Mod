package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Exercises full chunk decoration, including third-party biome modifiers and direct ore writes. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GeneratedDimensionTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void decoratedNetherRampsKeepSafeWalkingPassages(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.NETHER);
        var desert = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.DESERT);
        var generator = nether.getChunkSource().getGenerator();
        var random = nether.getChunkSource().randomState();
        helper.assertTrue(net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.terrain(random).isPresent(),
                "Ramp protection needs the seeded terrain fields in the Nether router");
        // A new cycle on every run prevents already decorated test chunks from hiding a regression.
        int cycle = 4000 + (int) ((System.currentTimeMillis() / 1000) % 10000);
        int startX = cycle * 616;
        double centerZ = (cycle + 11.5) * net.amurdza.examplemod.worldgen.DimensionTerrain.STAIR_BAND_SPACING;
        AOEMod.LOGGER.info("Decorated Nether ramp regression starts at {},{} with seed {}", startX, centerZ, nether.getSeed());
        int previousFeet = -1, previousZ = (int) Math.round(centerZ);
        boolean[] bands = new boolean[6];
        BlockPos lastFeet = null;
        for (int x = startX; x <= startX + 616; x++) {
            int z = previousZ;
            for (int step = 0; step < 6; step++) z = (int) Math.round(centerZ - random.router().ridges().compute(
                    new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, 0, z)) * 18);
            var base = stackColumn(nether, desert, x, z);
            int feet = 2;
            while (feet < 186 && !base.getBlock(feet).isAir()) feet++;
            var current = feet < 128 ? nether : desert;
            BlockPos pos = new BlockPos(x, feet < 128 ? feet : feet - 160, z);
            current.getChunk(pos); // FULL status: includes columns, deltas, ores, fungi and flora.
            var support = stackBlock(nether, desert, x, feet - 1, z);
            helper.assertTrue(feet < 186 && !support.getCollisionShape(current, pos.below()).isEmpty()
                            && support.getFluidState().isEmpty()
                            && !net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.hazardous(support),
                    "Decoration removed or damaged ramp support at " + pos);
            for (int dy = 0; dy < 8; dy++) {
                BlockPos air = pos.above(dy);
                var state = stackBlock(nether, desert, x, feet + dy, z);
                helper.assertTrue(state.getCollisionShape(current, air).isEmpty() && state.getFluidState().isEmpty()
                                && !net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.hazardous(state),
                        "Decoration obstructed the ramp at " + air + " with " + state);
            }
            if (previousFeet >= 0) helper.assertTrue(Math.abs(feet - previousFeet) <= 1 && Math.abs(z - previousZ) <= 1,
                    "Decorated ramp has an unclimbable adjacent step at " + pos);
            previousFeet = feet; previousZ = z;
            if (current == nether && feet < 120) lastFeet = pos;
            bands[Math.min(5, feet / 32)] = true;
        }
        for (int band = 0; band < 6; band++) helper.assertTrue(bands[band], "Decorated ramp missed band " + band);
        var region = new net.minecraft.server.level.WorldGenRegion(nether, java.util.List.of(nether.getChunk(lastFeet)),
                net.minecraft.world.level.chunk.ChunkStatus.FEATURES, 0);
        helper.assertFalse(region.setBlock(lastFeet, Blocks.BASALT.defaultBlockState(), 18), "A column can overwrite reserved air");
        helper.assertFalse(region.setBlock(lastFeet, Blocks.SOUL_FIRE.defaultBlockState(), 18), "Fire can occupy reserved air");
        helper.assertFalse(region.setBlock(lastFeet, Blocks.SWEET_BERRY_BUSH.defaultBlockState(), 18),
                "A damaging berry bush can occupy the reserved walkway");
        helper.assertFalse(region.setBlock(lastFeet.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 18), "Magma can replace the walkway");
        helper.assertFalse(region.setBlock(lastFeet.below(), Blocks.AIR.defaultBlockState(), 18), "Decoration can remove walkway support");
        helper.assertFalse(net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.allows(region, lastFeet.below(),
                Blocks.STONE_SLAB.defaultBlockState()), "Decoration can lower the supported step into a slab");
        helper.assertTrue(net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.allows(nether, lastFeet, Blocks.BASALT.defaultBlockState()),
                "The reservation must not restrict runtime player building");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void netherRampsHaveDrySupportedClimbableFloors(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.NETHER);
        var desert = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.DESERT);
        var generator = nether.getChunkSource().getGenerator();
        var random = nether.getChunkSource().randomState();
        int previousFloor = -1;
        double centerZ = net.amurdza.examplemod.worldgen.DimensionTerrain.STAIR_BAND_SPACING / 2;
        int previousZ = (int) Math.round(centerZ);
        boolean[] crossedBands = new boolean[6];
        for (int x = 0; x <= 616; x++) {
            int z = previousZ;
            for (int step = 0; step < 4; step++) {
                double detail = random.router().ridges().compute(
                        new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, 0, z));
                z = (int) Math.round(centerZ - detail * 18);
            }
            var column = stackColumn(nether, desert, x, z);
            int feet = 2;
            while (feet < 186 && !column.getBlock(feet).isAir()) feet++;
            helper.assertTrue(feet < 186 && column.getBlock(feet - 1).isSolid()
                            && column.getBlock(feet).isAir() && column.getBlock(feet + 1).isAir(),
                    "Ramp lacks a supported two-block-clearance passage at " + x + "," + z + " near Y" + feet);
            for (int y = 2; y < 127; y++) helper.assertFalse(column.getBlock(y).is(Blocks.LAVA),
                    "Upper lava pool flooded the ramp at " + x + "," + y + "," + z);
            if (previousFloor >= 0) helper.assertTrue(Math.abs(feet - previousFloor) <= 1 && Math.abs(z - previousZ) <= 1,
                    "Adjacent ramp floors have an unclimbable step at X" + x + ": " + previousFloor + " to " + feet);
            previousFloor = feet;
            previousZ = z;
            crossedBands[Math.min(5, feet / 32)] = true;
        }
        for (int band = 0; band < 6; band++) helper.assertTrue(crossedBands[band], "Ramp never reached Nether band " + band);
        helper.succeed();
    }

    private static net.minecraft.world.level.NoiseColumn stackColumn(net.minecraft.server.level.ServerLevel nether,
                                                                       net.minecraft.server.level.ServerLevel desert, int x, int z) {
        var lower = nether.getChunkSource().getGenerator().getBaseColumn(x, z, nether, nether.getChunkSource().randomState());
        var upper = desert.getChunkSource().getGenerator().getBaseColumn(x, z, desert, desert.getChunkSource().randomState());
        var states = new net.minecraft.world.level.block.state.BlockState[192];
        for (int y = 0; y < states.length; y++) states[y] = y < 128 ? lower.getBlock(y) : upper.getBlock(y - 160);
        return new net.minecraft.world.level.NoiseColumn(0, states);
    }

    private static net.minecraft.world.level.block.state.BlockState stackBlock(net.minecraft.server.level.ServerLevel nether,
                                                                              net.minecraft.server.level.ServerLevel desert,
                                                                              int x, int y, int z) {
        return y < 128 ? nether.getBlockState(new BlockPos(x, y, z))
                : desert.getBlockState(new BlockPos(x, y - 160, z));
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void lavaLakesRemainOnlyInBasaltDeltas(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.NETHER);
        var generator = nether.getChunkSource().getGenerator();
        var random = nether.getChunkSource().randomState();
        boolean basaltPool = false;
        boolean materialsVaryVertically = false;
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            var column = generator.getBaseColumn(x * 131, z * 167, nether, random);
            int consecutive = 0;
            for (int y = 0; y < 128; y++) {
                boolean lava = column.getBlock(y).is(Blocks.LAVA);
                helper.assertFalse(y >= 32 && lava,
                        "Lava lakes must not generate above the basalt deltas at " + (x * 131) + "," + y + "," + (z * 167));
                consecutive = lava ? consecutive + 1 : 0;
                basaltPool |= consecutive >= 5;
            }
            var low = new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x * 131, 100, z * 167);
            var high = new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x * 131, 120, z * 167);
            materialsVaryVertically |= Math.abs(random.router().vegetation().compute(low)
                    - random.router().vegetation().compute(high)) > 0.01;
        }
        helper.assertTrue(basaltPool, "The basalt deltas must retain deep lava lakes");
        helper.assertTrue(materialsVaryVertically, "Nether material patches must use three-dimensional noise");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void decoratedChunksKeepRegionalMaterials(GameTestHelper helper) {
        String[] names = {"tropical", "savanna", "plains", "snowy", "desert", "nether"};
        int cycle = 22000 + (int) ((System.currentTimeMillis() / 1000) % 10000);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            var level = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation("aoemod", name + "_dimension")));
            // A new cycle avoids passing or failing against already-saved chunks
            // from previous material/terrain rules. Existing saves retain those
            // blocks deliberately; this test is about newly generated terrain.
            var chunk = level.getChunk(cycle + i, cycle);
            Map<String, Integer> materials = new TreeMap<>();
            Set<String> forbidden = switch (name) {
                case "tropical", "savanna" -> Set.of("lava", "coal_ore", "iron_ore", "copper_ore", "gold_ore",
                        "diamond_ore", "emerald_ore", "lapis_ore", "redstone_ore", "deepslate");
                case "plains" -> Set.of("lava", "deepslate", "diamond_ore", "emerald_ore", "lapis_ore", "redstone_ore");
                case "desert" -> Set.of("bedrock", "iron_ore", "deepslate_iron_ore", "diamond_ore", "deepslate_diamond_ore",
                        "emerald_ore", "deepslate_emerald_ore", "lapis_ore", "deepslate_lapis_ore");
                case "snowy" -> Set.of("redstone_ore", "deepslate_redstone_ore");
                default -> Set.of("water");
            };
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    cursor.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    var state = chunk.getBlockState(cursor);
                    var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (!state.isAir()) materials.merge(id.toString(), 1, Integer::sum);
                    helper.assertFalse(id.getNamespace().equals("minecraft") && forbidden.contains(id.getPath()),
                            "Forbidden " + id + " generated in " + name + " at " + cursor);
                    if (!name.equals("desert") && y == level.getMinBuildHeight()) {
                        helper.assertTrue(state.is(Blocks.BEDROCK), "Missing bottom seal in " + name);
                    }
                }
            }
            if (name.equals("tropical")) helper.assertTrue(materials.getOrDefault("minecraft:moss_block", 0) > 100
                            && materials.getOrDefault("minecraft:mud", 0) == 0,
                    "Tropical bulk material should be moss without underground mud");
            if (name.equals("savanna")) helper.assertTrue(materials.getOrDefault("minecraft:mud", 0) > 100
                            && materials.getOrDefault("minecraft:moss_block", 0) == 0,
                    "Savanna bulk material should be mud beneath its soil, with no moss");
            AOEMod.LOGGER.info("Generated dimension material sample {}: {}", name, materials);
        }
        helper.succeed();
    }
}
