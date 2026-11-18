package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.event_handlers.CustomBlockDrops;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeedPaletteTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void bothForestGroundCoversUseTheirOwnSeedsWithoutChangingRates(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = level.getChunkSource().getGenerator();
        for (String biome : new String[]{"dappled_forest", "snowy_forest"}) {
            BlockPos pos = null;
            // Cross the bands along Z. Along-ridge X sampling can now stay in
            // one habitat indefinitely because the bends are deliberately slow.
            for (int i = 0; i < 1024; i++) {
                BlockPos candidate = new BlockPos(96000, 300, 96000 + i * 16);
                var baseBiome = generator.getBiomeSource().getNoiseBiome(candidate.getX() >> 2, 75,
                        candidate.getZ() >> 2, level.getChunkSource().randomState().sampler());
                if (baseBiome.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                        new ResourceLocation("aoemod", biome))) && GameplayDimensions.biomeIs(level, candidate, biome)) {
                    pos = candidate;
                    break;
                }
            }
            helper.assertTrue(pos != null, "Could not find a real " + biome + " seed-loot fixture");
            Item expected = biome.equals("dappled_forest") ? Items.WHEAT_SEEDS : Items.BEETROOT_SEEDS;
            for (var block : new net.minecraft.world.level.block.Block[]{Blocks.GRASS, Blocks.TALL_GRASS,
                    Blocks.FERN, Blocks.LARGE_FERN}) {
                var state = block.defaultBlockState();
                double chance = block instanceof net.minecraft.world.level.block.DoublePlantBlock ? 0.10 : 0.05;
                RandomSource random = RandomSource.create(871293);
                int successes = 0;
                for (int i = 0; i < 12000; i++) {
                    var drops = CustomBlockDrops.configuredDrops(level, pos, state, ItemStack.EMPTY, random);
                    helper.assertTrue(drops != null && drops.size() <= 1, "Ground cover has one seed lottery");
                    for (var stack : drops) {
                        helper.assertTrue(stack.is(expected) && stack.getCount() == 1, "Wrong forest seed: " + stack);
                        successes++;
                    }
                }
                helper.assertTrue(Math.abs(successes / 12000.0 - chance) < 0.012,
                        "Forest seed frequency changed for " + biome + ": " + successes);
                helper.assertTrue(CustomBlockDrops.configuredDrops(level, pos, state, new ItemStack(Items.SHEARS), random) == null,
                        "Shears must keep the original grass/fern item");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void plainsAndSavannaUseOneSharedSeedLotteryWithTheRequestedPalettes(GameTestHelper helper) {
        Item corn = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hearthandharvest:corn_kernels"));
        helper.assertTrue(corn != null && corn != Items.AIR, "Corn kernels must be registered");
        var palettes = Map.of(
                GameplayDimensions.PLAINS, Set.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, ModItems.POTATO_SEEDS.get()),
                GameplayDimensions.SAVANNA, Set.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, ModItems.POTATO_SEEDS.get(), corn));
        for (var region : palettes.entrySet()) {
            var level = helper.getLevel().getServer().getLevel(region.getKey());
            for (var block : new net.minecraft.world.level.block.Block[]{Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN}) {
                var state = block.defaultBlockState();
                var entries = BlockConfig.blockDropEntries(level, BlockPos.ZERO, state);
                Set<Item> actual = entries.stream().map(BlockConfig.BlockLootEntry::item).collect(Collectors.toSet());
                double chance = block instanceof net.minecraft.world.level.block.DoublePlantBlock ? 0.25 : 0.125;
                helper.assertTrue(actual.equals(region.getValue()) && entries.size() == actual.size(),
                        "Incorrect or duplicated grass seed palette for " + region.getKey().location() + ": " + actual);
                helper.assertTrue(entries.stream().allMatch(BlockConfig.BlockLootEntry::groupedSeedRoll)
                                && Math.abs(entries.stream().mapToDouble(e -> BlockConfig.blockDropAmount(state, e)).sum() - chance) < 1e-7,
                        "Short grass must share one 1/8 lottery; tall grass one 1/4 lottery");
                RandomSource random = RandomSource.create(718231L);
                Set<Item> observed = new HashSet<>();
                Map<Item, Integer> seedCounts = new java.util.HashMap<>();
                int total = 0;
                for (int i = 0; i < 24000; i++) {
                    var drops = CustomBlockDrops.configuredDrops(level, BlockPos.ZERO, state, ItemStack.EMPTY, random);
                    helper.assertTrue(drops != null && drops.size() <= 1
                                    && drops.stream().mapToInt(ItemStack::getCount).sum() <= 1,
                            "A single grass harvest cannot return multiple seeds");
                    for (ItemStack stack : drops) {
                        helper.assertTrue(region.getValue().contains(stack.getItem())
                                        && !stack.is(Items.POTATO) && !stack.is(Items.CARROT) && !stack.is(Items.PUMPKIN_SEEDS),
                                "Grass dropped produce, pumpkin seed, or an unrequested seed: " + stack);
                        observed.add(stack.getItem());
                        seedCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                        total += stack.getCount();
                    }
                }
                helper.assertTrue(observed.equals(region.getValue()), "Every requested seed must be reachable by actual loot rolls");
                double perSeed = total / (double) region.getValue().size();
                helper.assertTrue(seedCounts.values().stream().allMatch(n -> Math.abs(n - perSeed) <= perSeed * .15),
                        "Every seed must be equally common in actual harvesting: " + seedCounts);
                helper.assertTrue(Math.abs(total / 24000.0 - chance) < 0.015,
                        "Observed seed frequency differs from the shared lottery: " + total + "/24000");
                ItemStack shears = new ItemStack(Items.SHEARS);
                helper.assertTrue(CustomBlockDrops.configuredDrops(level, BlockPos.ZERO, state, shears, random) == null
                                && !CustomBlockDrops.usesConfiguredDrops(level, BlockPos.ZERO, state, shears),
                        "Shears must delegate to the original grass block drop, without rolling seeds");
                AOEMod.LOGGER.info("Grass seed audit {} {}: {} of 24000 drops, {} distinct seeds",
                        region.getKey().location(), block, total, observed.size());
            }
        }
        helper.succeed();
    }
}
