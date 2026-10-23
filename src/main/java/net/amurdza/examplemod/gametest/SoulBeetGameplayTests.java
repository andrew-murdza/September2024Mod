package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Proxy;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulBeetGameplayTests {
    private static final String TEMPLATE = "plant_gameplay_empty";

    private SoulBeetGameplayTests() { }

    @GameTest(template = TEMPLATE)
    public static void soulBeetsRequireValleySoulSandOrSoilWithoutLight(GameTestHelper helper) {
        var crop = ModBlocks.SOUL_BEETROOTS.get();
        BlockState state = crop.defaultBlockState();
        BlockPos pos = new BlockPos(0, 80, 0);
        ResourceKey<Biome> modValley = ResourceKey.create(Registries.BIOME, AOEMod.makeID("soul_sand_valley"));
        for (ResourceKey<Biome> biome : List.of(modValley, Biomes.SOUL_SAND_VALLEY)) {
            for (Block support : new Block[]{Blocks.SOUL_SAND, Blocks.SOUL_SOIL}) {
                LevelReader habitat = darkHabitat(helper.getLevel(), pos, support, biome);
                helper.assertTrue(state.canSurvive(habitat, pos),
                        "Soul beets must survive in darkness on " + support + " in " + biome.location());
                helper.assertTrue(crop.isValidBonemealTarget(habitat, pos, state, false),
                        "Young soul beets must accept bonemeal in their habitat");
            }
        }
        for (Block support : new Block[]{Blocks.FARMLAND, Blocks.DIRT, Blocks.NETHERRACK}) {
            helper.assertFalse(state.canSurvive(darkHabitat(helper.getLevel(), pos, support, modValley), pos),
                    "Soul beets must reject " + support);
        }
        LevelReader wrongBiome = darkHabitat(helper.getLevel(), pos, Blocks.SOUL_SOIL, Biomes.NETHER_WASTES);
        helper.assertFalse(state.canSurvive(wrongBiome, pos), "Soul beets must require soul sand valley");
        helper.assertFalse(crop.isValidBonemealTarget(wrongBiome, pos, state, false),
                "Bonemeal must not bypass the soul beet habitat restriction");
        helper.assertTrue(crop.getCloneItemStack(helper.getLevel(), pos, state).is(ModItems.SOUL_BEETROOT_SEEDS.get()),
                "Picking a soul beet crop must return its planting seeds");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void darkSoulBeetsGrowAndIgnoreCrowding(GameTestHelper helper) {
        var crop = ModBlocks.SOUL_BEETROOTS.get();
        ServerLevel level = helper.getLevel();
        BlockPos crowded = new BlockPos(3, 2, 3);
        BlockPos isolated = new BlockPos(7, 2, 3);
        // Include a biome-sampling margin because biome lookup interpolates nearby quart positions.
        BlockPos min = helper.absolutePos(new BlockPos(-3, -3, -3));
        BlockPos max = helper.absolutePos(new BlockPos(13, 7, 9));
        String command = "fillbiome " + min.getX() + " " + min.getY() + " " + min.getZ() + " "
                + max.getX() + " " + max.getY() + " " + max.getZ() + " aoemod:soul_sand_valley";
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(), command);
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 9, 3, 5)) {
            Block block = pos.getY() == 1 ? Blocks.SOUL_SOIL
                    : pos.getY() == 3 || pos.getX() == 1 || pos.getX() == 9 || pos.getZ() == 1 || pos.getZ() == 5
                    ? Blocks.STONE : Blocks.AIR;
            helper.setBlock(pos, block);
        }
        for (BlockPos pos : BlockPos.betweenClosed(2, 2, 2, 4, 2, 4)) {
            helper.setBlock(pos, crop.defaultBlockState());
        }
        helper.setBlock(isolated, crop.defaultBlockState());
        helper.runAfterDelay(10, () -> {
            BlockPos crowdedAbsolute = helper.absolutePos(crowded);
            BlockPos isolatedAbsolute = helper.absolutePos(isolated);
            helper.assertTrue(level.getRawBrightness(crowdedAbsolute, 0) == 0
                            && level.getRawBrightness(isolatedAbsolute, 0) == 0,
                    "The growth fixture must be completely dark");
            helper.assertTrue(crop.defaultBlockState().canSurvive(level, isolatedAbsolute),
                    "The real test world must provide a valid soul beet habitat");
            RandomSource seeds = RandomSource.create(802152);
            int growths = 0;
            for (int i = 0; i < 128; i++) {
                helper.setBlock(crowded, crop.defaultBlockState());
                helper.setBlock(isolated, crop.defaultBlockState());
                long seed = seeds.nextLong();
                crop.randomTick(crop.defaultBlockState(), level, crowdedAbsolute, RandomSource.create(seed));
                crop.randomTick(crop.defaultBlockState(), level, isolatedAbsolute, RandomSource.create(seed));
                int crowdedAge = crop.getAge(helper.getBlockState(crowded));
                int isolatedAge = crop.getAge(helper.getBlockState(isolated));
                helper.assertTrue(crowdedAge == isolatedAge,
                        "Neighboring soul beets must not penalize random growth");
                growths += isolatedAge;
            }
            helper.assertTrue(growths > 0 && growths < 128, "Dark random ticks must sometimes grow soul beets");
            helper.setBlock(isolated, crop.defaultBlockState());
            RandomSource growthRandom = RandomSource.create(5913);
            for (int i = 0; i < 128 && !crop.isMaxAge(helper.getBlockState(isolated)); i++) {
                crop.randomTick(helper.getBlockState(isolated), level, isolatedAbsolute, growthRandom);
            }
            helper.assertTrue(crop.isMaxAge(helper.getBlockState(isolated)), "Random ticks must mature soul beets");
            helper.setBlock(isolated, crop.defaultBlockState());
            ItemStack bonemeal = new ItemStack(Items.BONE_MEAL, 64);
            for (int i = 0; i < 64 && !crop.isMaxAge(helper.getBlockState(isolated)); i++) {
                helper.assertTrue(BoneMealItem.growCrop(bonemeal, level, isolatedAbsolute),
                        "Bonemeal must work on soul soil in darkness");
            }
            helper.assertTrue(crop.isMaxAge(helper.getBlockState(isolated)), "Bonemeal must mature soul beets");
            helper.assertFalse(BoneMealItem.growCrop(bonemeal, level, isolatedAbsolute),
                    "Mature soul beets must stop consuming bonemeal");
            helper.assertTrue(helper.getBlockState(isolated).getLightEmission(level, isolatedAbsolute) == 0,
                    "Glowing soul beet leaves must not emit block light");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void cropLootAndSoulWeedSeedsUseLoadedLootTables(GameTestHelper helper) {
        var crop = ModBlocks.SOUL_BEETROOTS.get();
        Item beet = ModItems.SOUL_BEETROOT.get();
        Item seeds = ModItems.SOUL_BEETROOT_SEEDS.get();
        for (int age = 0; age < crop.getMaxAge(); age++) {
            var drops = drops(helper, crop.getStateForAge(age), ItemStack.EMPTY, 123);
            helper.assertTrue(count(drops, seeds) == 1 && count(drops, beet) == 0,
                    "Immature soul beets must return one seed and no beet");
        }
        ItemStack fortuneTool = new ItemStack(Items.DIAMOND_HOE);
        fortuneTool.enchant(Enchantments.BLOCK_FORTUNE, 3);
        int ordinarySeeds = 0;
        int fortuneSeeds = 0;
        RandomSource random = RandomSource.create(19238);
        for (int i = 0; i < 128; i++) {
            long seed = random.nextLong();
            BlockState mature = crop.getStateForAge(crop.getMaxAge());
            var ordinary = drops(helper, mature, ItemStack.EMPTY, seed);
            var fortunate = drops(helper, mature, fortuneTool, seed);
            helper.assertTrue(count(ordinary, beet) == 1 && count(fortunate, beet) == 1,
                    "Mature soul beets must drop one beet regardless of Fortune");
            ordinarySeeds += count(ordinary, seeds);
            fortuneSeeds += count(fortunate, seeds);
        }
        helper.assertTrue(fortuneSeeds > ordinarySeeds, "Fortune must increase mature soul beet seed yield");
        Block weeds = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("nourished_nether", "soul_weeds"));
        helper.assertTrue(weeds != null && weeds != Blocks.AIR, "Soul weeds must be registered");
        for (Item shears : new Item[]{Items.SHEARS, ModItems.BONE_SHEARS.get()}) {
            var sheared = Block.getDrops(weeds.defaultBlockState(), helper.getLevel(),
                    helper.absolutePos(new BlockPos(1, 1, 1)), null, null, new ItemStack(shears));
            helper.assertTrue(count(sheared, weeds.asItem()) == 1 && count(sheared, seeds) == 0,
                    "Shears must collect soul weeds instead of seeds");
        }
        int seedRolls = 0;
        for (int i = 0; i < 256; i++) {
            // Exercise the dependency block's getDrops override as well as its loaded loot table.
            var drops = Block.getDrops(weeds.defaultBlockState(), helper.getLevel(),
                    helper.absolutePos(new BlockPos(1, 1, 1)), null, null, ItemStack.EMPTY);
            helper.assertTrue(drops.stream().allMatch(stack -> stack.is(seeds)),
                    "Unsheared soul weeds must only drop soul beet seeds");
            seedRolls += count(drops, seeds);
        }
        helper.assertTrue(seedRolls > 0 && seedRolls < 256, "Soul weeds must sometimes drop a seed and sometimes nothing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sixSoulBeetsCraftSoupAndReturnTheBowl(GameTestHelper helper) {
        var menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

            @Override
            public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        for (int slot : new int[]{0, 1, 2, 3, 5, 7}) {
            grid.setItem(slot, new ItemStack(ModItems.SOUL_BEETROOT.get()));
        }
        grid.setItem(8, new ItemStack(Items.BOWL));
        ServerLevel level = helper.getLevel();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(recipe.isPresent(), "Six soul beets and a bowl must have a loaded crafting recipe");
        ItemStack soup = recipe.orElseThrow().assemble(grid, level.registryAccess());
        helper.assertTrue(soup.is(ModItems.SOUL_BEETROOT_SOUP.get()) && soup.getCount() == 1,
                "Soul beet soup recipe must produce one serving");
        grid.setItem(0, ItemStack.EMPTY);
        helper.assertFalse(recipe.orElseThrow().matches(grid, level), "Soup must require all six soul beets");
        grid.setItem(0, new ItemStack(Items.BEETROOT));
        helper.assertFalse(recipe.orElseThrow().matches(grid, level), "Ordinary beets must not replace soul beets");
        ItemStack bowl = soup.finishUsingItem(level, helper.makeMockSurvivalPlayer());
        helper.assertTrue(bowl.is(Items.BOWL), "Eating soul beet soup must return its bowl");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stackedSoupPreservesServingsAndReturnsOneBowlEach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack servings = new ItemStack(ModItems.SOUL_BEETROOT_SOUP.get(), 3);
        int originalBowls = player.getInventory().countItem(Items.BOWL);
        ItemStack remaining = servings.finishUsingItem(level, player);
        helper.assertTrue(remaining.is(ModItems.SOUL_BEETROOT_SOUP.get()) && remaining.getCount() == 2,
                "Eating stacked soup must preserve the other two servings");
        helper.assertTrue(player.getInventory().countItem(Items.BOWL) == originalBowls + 1,
                "Eating one serving from a stack must add exactly one bowl");
        remaining = remaining.finishUsingItem(level, player);
        helper.assertTrue(remaining.is(ModItems.SOUL_BEETROOT_SOUP.get()) && remaining.getCount() == 1,
                "Eating the second serving must preserve the final serving");
        helper.assertTrue(player.getInventory().countItem(Items.BOWL) == originalBowls + 2,
                "Each consumed stacked serving must add exactly one bowl");
        ItemStack finalBowl = remaining.finishUsingItem(level, player);
        helper.assertTrue(finalBowl.is(Items.BOWL) && finalBowl.getCount() == 1,
                "The final serving must return a single bowl in place of the soup");
        helper.assertTrue(player.getInventory().countItem(Items.BOWL) == originalBowls + 2,
                "The final serving must not also duplicate its bowl into the inventory");

        Player creative = helper.makeMockSurvivalPlayer();
        creative.getAbilities().instabuild = true;
        ItemStack creativeServings = new ItemStack(ModItems.SOUL_BEETROOT_SOUP.get(), 3);
        int creativeBowls = creative.getInventory().countItem(Items.BOWL);
        ItemStack creativeResult = creativeServings.finishUsingItem(level, creative);
        helper.assertTrue(creativeResult.is(ModItems.SOUL_BEETROOT_SOUP.get()) && creativeResult.getCount() == 3,
                "Creative players must keep all their soup servings");
        helper.assertTrue(creative.getInventory().countItem(Items.BOWL) == creativeBowls,
                "Creative players must not receive bowls for unconsumed soup");
        helper.succeed();
    }

    private static List<ItemStack> drops(GameTestHelper helper, BlockState state, ItemStack tool, long seed) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        return helper.getLevel().getServer().getLootData().getLootTable(state.getBlock().getLootTable())
                .getRandomItems(params, seed);
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static LevelReader darkHabitat(ServerLevel level, BlockPos cropPos, Block support, ResourceKey<Biome> biomeKey) {
        Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biomeKey);
        return (LevelReader) Proxy.newProxyInstance(SoulBeetGameplayTests.class.getClassLoader(),
                new Class<?>[]{LevelReader.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getBiome" -> biome;
                    case "getBlockState" -> cropPos.below().equals(args[0]) ? support.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    case "getRawBrightness", "getBrightness" -> 0;
                    case "canSeeSky" -> false;
                    case "toString" -> "Dark soul beet habitat";
                    default -> throw new UnsupportedOperationException("Unexpected habitat access: " + method.getName());
                });
    }
}
