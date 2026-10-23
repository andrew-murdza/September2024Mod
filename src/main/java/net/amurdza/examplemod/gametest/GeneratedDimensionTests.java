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
        var generator = nether.getChunkSource().getGenerator();
        var random = nether.getChunkSource().randomState();
        helper.assertTrue(net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.terrain(random).isPresent(),
                "Ramp protection needs the seeded terrain fields in the Nether router");
        // A new cycle on every run prevents already decorated test chunks from hiding a regression.
        int cycle = 4000 + (int) ((System.currentTimeMillis() / 1000) % 10000);
        int startX = cycle * 368, centerZ = (cycle + 11) * 192 + 96;
        AOEMod.LOGGER.info("Decorated Nether ramp regression starts at {},{} with seed {}", startX, centerZ, nether.getSeed());
        int previousFeet = -1, previousZ = centerZ;
        boolean[] bands = new boolean[4];
        BlockPos lastFeet = null;
        for (int x = startX; x <= startX + 368; x++) {
            int z = previousZ;
            for (int step = 0; step < 6; step++) z = (int) Math.round(centerZ - random.router().ridges().compute(
                    new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, 0, z)) * 18);
            var base = generator.getBaseColumn(x, z, nether, random);
            int feet = 2;
            while (feet < 126 && !base.getBlock(feet).isAir()) feet++;
            BlockPos pos = new BlockPos(x, feet, z);
            nether.getChunk(pos); // FULL status: includes columns, deltas, ores, fungi and flora.
            var support = nether.getBlockState(pos.below());
            helper.assertTrue(feet < 126 && !support.getCollisionShape(nether, pos.below()).isEmpty()
                            && support.getFluidState().isEmpty()
                            && !net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.hazardous(support),
                    "Decoration removed or damaged ramp support at " + pos);
            for (int dy = 0; dy < 3; dy++) {
                BlockPos air = pos.above(dy);
                var state = nether.getBlockState(air);
                helper.assertTrue(state.getCollisionShape(nether, air).isEmpty() && state.getFluidState().isEmpty()
                                && !net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.hazardous(state),
                        "Decoration obstructed the ramp at " + air + " with " + state);
            }
            if (previousFeet >= 0) helper.assertTrue(Math.abs(feet - previousFeet) <= 1 && Math.abs(z - previousZ) <= 1,
                    "Decorated ramp has an unclimbable adjacent step at " + pos);
            previousFeet = feet; previousZ = z; lastFeet = pos;
            bands[Math.min(3, feet / 32)] = true;
        }
        for (int band = 0; band < 4; band++) helper.assertTrue(bands[band], "Decorated ramp missed band " + band);
        var region = new net.minecraft.server.level.WorldGenRegion(nether, java.util.List.of(nether.getChunk(lastFeet)),
                net.minecraft.world.level.chunk.ChunkStatus.FEATURES, 0);
        helper.assertFalse(region.setBlock(lastFeet, Blocks.BASALT.defaultBlockState(), 18), "A column can overwrite reserved air");
        helper.assertFalse(region.setBlock(lastFeet, Blocks.SOUL_FIRE.defaultBlockState(), 18), "Fire can occupy reserved air");
        helper.assertFalse(region.setBlock(lastFeet, net.amurdza.examplemod.registry.ModBlocks.SOUL_BERRY_BUSH.get().defaultBlockState(), 18),
                "A damaging soul-berry bush can occupy the reserved walkway");
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
        var generator = nether.getChunkSource().getGenerator();
        var random = nether.getChunkSource().randomState();
        int previousFloor = -1;
        int previousZ = 96;
        boolean[] crossedBands = new boolean[4];
        for (int x = 0; x <= 368; x++) {
            int z = previousZ;
            for (int step = 0; step < 4; step++) {
                double detail = random.router().ridges().compute(
                        new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, 0, z));
                z = (int) Math.round(96 - detail * 18);
            }
            var column = generator.getBaseColumn(x, z, nether, random);
            int feet = 2;
            while (feet < 126 && !column.getBlock(feet).isAir()) feet++;
            helper.assertTrue(feet < 126 && column.getBlock(feet - 1).isSolid()
                            && column.getBlock(feet).isAir() && column.getBlock(feet + 1).isAir(),
                    "Ramp lacks a supported two-block-clearance passage at " + x + "," + z + " near Y" + feet);
            for (int y = 2; y < 127; y++) helper.assertFalse(column.getBlock(y).is(Blocks.LAVA),
                    "Upper lava pool flooded the ramp at " + x + "," + y + "," + z);
            if (previousFloor >= 0) helper.assertTrue(Math.abs(feet - previousFloor) <= 1 && Math.abs(z - previousZ) <= 1,
                    "Adjacent ramp floors have an unclimbable step at X" + x + ": " + previousFloor + " to " + feet);
            previousFloor = feet;
            previousZ = z;
            crossedBands[Math.min(3, feet / 32)] = true;
        }
        for (int band = 0; band < 4; band++) helper.assertTrue(crossedBands[band], "Ramp never reached Nether band " + band);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void upperNetherBandsHaveFishableLavaPools(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.NETHER);
        var generator = nether.getChunkSource().getGenerator();
        var random = nether.getChunkSource().randomState();
        boolean[] pools = new boolean[4];
        boolean materialsVaryVertically = false;
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            var column = generator.getBaseColumn(x * 131, z * 167, nether, random);
            for (int band = 0; band < 4; band++) {
                int consecutive = 0;
                for (int y = band * 32; y < band * 32 + 32; y++) {
                    consecutive = column.getBlock(y).is(Blocks.LAVA) ? consecutive + 1 : 0;
                    pools[band] |= consecutive >= 5;
                }
            }
            var low = new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x * 131, 100, z * 167);
            var high = new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x * 131, 120, z * 167);
            materialsVaryVertically |= Math.abs(random.router().vegetation().compute(low)
                    - random.router().vegetation().compute(high)) > 0.01;
        }
        for (int band = 0; band < 4; band++) helper.assertTrue(pools[band],
                "Nether band " + band + " needs five-deep pools for its documented large lava fish");
        helper.assertTrue(materialsVaryVertically, "Nether material patches must use three-dimensional noise");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void decoratedChunksKeepRegionalMaterials(GameTestHelper helper) {
        String[] names = {"tropical", "savanna", "plains", "snowy", "desert", "nether"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            var level = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation("aoemod", name + "_dimension")));
            // Remote test-only chunks avoid structures placed by other tests or old sample worlds.
            var chunk = level.getChunk(1900 + i, 1900);
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
            if (name.equals("tropical")) helper.assertTrue(materials.getOrDefault("minecraft:mud", 0) > 100,
                    "Tropical bulk material should be mud beneath its moss cover");
            if (name.equals("savanna")) helper.assertTrue(materials.getOrDefault("minecraft:moss_block", 0) > 100,
                    "Savanna bulk material should be moss beneath its soil");
            AOEMod.LOGGER.info("Generated dimension material sample {}: {}", name, materials);
        }
        helper.succeed();
    }
}
