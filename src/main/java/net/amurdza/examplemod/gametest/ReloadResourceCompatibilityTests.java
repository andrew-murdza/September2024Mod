package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import vectorwing.farmersdelight.common.world.modifier.AddFeaturesByFilterBiomeModifier;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReloadResourceCompatibilityTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void wildPeanutsGenerateOnlyInTemperateOverworldBiomes(GameTestHelper helper) {
        var loaded = helper.getLevel().registryAccess().registryOrThrow(ForgeRegistries.Keys.BIOME_MODIFIERS)
                .get(new ResourceLocation("hearthandharvest:wild_peanuts"));
        helper.assertTrue(loaded instanceof AddFeaturesByFilterBiomeModifier,
                "The actual peanut biome modifier must load its feature filter");
        var modifier = (AddFeaturesByFilterBiomeModifier) loaded;
        helper.assertTrue(modifier.step() == GenerationStep.Decoration.VEGETAL_DECORATION,
                "Peanuts must retain their vegetation generation step");
        checkPeanutBiome(helper, modifier, Biomes.PLAINS, true);
        checkPeanutBiome(helper, modifier, Biomes.FOREST, true);
        checkPeanutBiome(helper, modifier, Biomes.DESERT, false);
        checkPeanutBiome(helper, modifier, Biomes.SNOWY_PLAINS, false);
        checkPeanutBiome(helper, modifier, Biomes.NETHER_WASTES, false);
        checkPeanutBiome(helper, modifier, Biomes.LUSH_CAVES, false);
        checkPeanutBiome(helper, modifier, Biomes.MUSHROOM_FIELDS, false);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void floralFlairBlockLootPreservesFlowersPotsAndTallHalves(GameTestHelper helper) {
        int checked = 0;
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if (!id.getNamespace().equals("floral_flair")) continue;
            Block block = BuiltInRegistries.BLOCK.get(id);
            var table = helper.getLevel().getServer().getLootData().getLootTable(block.getLootTable());
            helper.assertTrue(table.getParamSet() == LootContextParamSets.BLOCK,
                    "Floral Flair block loot must use the block context: " + id);
            checked++;
        }
        helper.assertTrue(checked == 45, "All 45 installed Floral Flair block tables must be exercised");
        Item foxnip = item("floral_flair:foxnip");
        List<ItemStack> flower = drops(helper, block("floral_flair:foxnip"), ItemStack.EMPTY, 1);
        helper.assertTrue(count(flower, foxnip) == 1 && total(flower) == 1,
                "A single flower must still drop exactly one matching item");
        List<ItemStack> potted = drops(helper, block("floral_flair:potted_foxnip"), ItemStack.EMPTY, 1);
        helper.assertTrue(count(potted, foxnip) == 1 && count(potted, Items.FLOWER_POT) == 1 && total(potted) == 2,
                "Breaking a potted flower must preserve both its flower and pot");
        BlockState tall = block("floral_flair:fairy_blossom");
        helper.assertTrue(count(drops(helper, tall.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                        DoubleBlockHalf.LOWER), ItemStack.EMPTY, 1), item("floral_flair:fairy_blossom")) == 1,
                "The lower tall-flower half must supply its one drop");
        helper.assertTrue(drops(helper, tall.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                        DoubleBlockHalf.UPPER), ItemStack.EMPTY, 1).isEmpty(),
                "The upper tall-flower half must not duplicate its drop");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void repairedWildCropLootPreservesToolsAndFortune(GameTestHelper helper) {
        BlockState peanuts = block("hearthandharvest:wild_peanuts");
        List<ItemStack> harvested = drops(helper, peanuts, ItemStack.EMPTY, 1);
        helper.assertTrue(count(harvested, item("hearthandharvest:peanut")) == 1 && total(harvested) == 1,
                "Ordinary wild peanut harvesting must still yield one peanut");
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        for (ItemStack tool : List.of(new ItemStack(Items.SHEARS), silk)) {
            List<ItemStack> picked = drops(helper, peanuts, tool, 1);
            helper.assertTrue(count(picked, item("hearthandharvest:wild_peanuts")) == 1 && total(picked) == 1,
                    "Shears and silk touch must preserve the wild peanut plant");
        }
        BlockState pineapple = block("pineapple_delight:pineapple_wild_crop");
        Item crop = item("pineapple_delight:pineapple_crop");
        List<ItemStack> basic = drops(helper, pineapple, ItemStack.EMPTY, 1);
        helper.assertTrue(count(basic, crop) == 1 && total(basic) == 1,
                "Removing an unreachable alternative must retain one pineapple crop without fortune");
        ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        boolean sawBonus = false;
        for (long seed = 1; seed <= 32; seed++) {
            List<ItemStack> enriched = drops(helper, pineapple, fortune, seed);
            int amount = count(enriched, crop);
            helper.assertTrue(amount >= 1 && amount <= 7 && total(enriched) == amount,
                    "Fortune III must retain the original one-to-seven pineapple-crop yield");
            sawBonus |= amount > 1;
        }
        helper.assertTrue(sawBonus, "Fortune must still increase at least one tested crop harvest");
        helper.succeed();
    }

    private static void checkPeanutBiome(GameTestHelper helper, AddFeaturesByFilterBiomeModifier modifier,
                                         ResourceKey<Biome> biomeKey, boolean expected) {
        Holder<Biome> biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(biomeKey);
        boolean selected = modifier.allowedBiomes().contains(biome)
                && modifier.deniedBiomes().map(denied -> !denied.contains(biome)).orElse(true);
        helper.assertTrue(selected == expected, "Loaded peanut filter selected the wrong habitat: " + biomeKey.location());
        boolean hasFeature = biome.value().getGenerationSettings().features().stream()
                .flatMap(features -> features.stream())
                .anyMatch(feature -> feature.is(new ResourceLocation("hearthandharvest:patch_wild_peanuts")));
        helper.assertTrue(hasFeature == expected,
                "Actual biome generation settings must reflect the peanut filter: " + biomeKey.location());
    }

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(id)).defaultBlockState();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(new ResourceLocation(id));
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

    private static int total(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(ItemStack::getCount).sum();
    }
}
