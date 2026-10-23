package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Real optional API integration: include the paired VillagerRealms build to exercise this test. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillagerRealmsLootCompatibilityTests {
    private VillagerRealmsLootCompatibilityTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void pairedWorkerLootUsesRegionalBaseForProfessionExtras(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("villager_realms")) {
            AOEMod.LOGGER.info("Skipping paired worker-loot integration: VillagerRealms is not loaded");
            helper.succeed();
            return;
        }
        ServerLevel savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockState potatoes = Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, 7);
        Supplier<List<ItemStack>> provider = post(savanna, potatoes, ItemStack.EMPTY);
        helper.assertTrue(provider != null, "The real AoE listener did not attach regional worker loot");
        helper.assertTrue(count(provider.get(), Items.POTATO) == 5, "Savanna mature potato base yield must be five");

        ResourceLocation id = new ResourceLocation(AOEMod.MOD_ID, "test/paired_worker_loot");
        LootTable original = LootTable.lootTable().setParamSet(LootContextParamSets.EMPTY)
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.POTATO)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))))
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))).build();
        Class<?> matchType = type("loot.LootEntryMatch");
        Class<?> hillType = type("work.model.HillParameters");
        Class<?> curveType = type("work.model.StatisticCurve");
        Class<?> biomeType = type("loot.BiomeMultipliers");
        Class<?> toolType = type("loot.ToolMultipliers");
        Class<?> entryType = type("loot.LootBonusEntry");
        Class<?> profileType = type("loot.LootBonusProfile");
        Class<?> inputType = type("loot.LootBonusInputs");
        Object match = matchType.getConstructor(ResourceLocation.class, String.class, Integer.class)
                .newInstance(new ResourceLocation("minecraft", "potato"), null, null);
        Object hill = hillType.getConstructor(double.class, double.class).newInstance(20D, 1D);
        Object entry = entryType.getConstructor(String.class, matchType, double.class, double.class,
                        hillType, curveType, Map.class, biomeType, toolType, Map.class, Map.class)
                .newInstance("potato", match, 1D, 1D, hill, curveType.getField("NEUTRAL").get(null), Map.of(),
                        biomeType.getField("NEUTRAL").get(null), toolType.getField("NEUTRAL").get(null), Map.of(), Map.of());
        Object profile = profileType.getConstructor(ResourceLocation.class, ResourceLocation.class, List.class)
                .newInstance(id, id, List.of(entry));
        Object inputs = inputType.getMethod("levelOnly", double.class).invoke(null, 0D);
        LootParams params = new LootParams.Builder(savanna).create(LootContextParamSets.EMPTY);
        var getDrops = type("loot.ProfessionLootService").getMethod("getTableDrops", LootTable.class,
                LootParams.class, profileType, inputType, Supplier.class);
        @SuppressWarnings("unchecked")
        List<ItemStack> combined = (List<ItemStack>) getDrops.invoke(null, original, params, profile, inputs, provider);
        helper.assertTrue(count(combined, Items.POTATO) == 10,
                "Real regional base five plus one profession roll must yield ten, before one GLM pass: " + combined);
        @SuppressWarnings("unchecked")
        List<ItemStack> nativeDrops = (List<ItemStack>) getDrops.invoke(null, original, params, profile, inputs, null);
        helper.assertTrue(count(nativeDrops, Items.POTATO) == 2, "No adapter changed native base one plus extra one");
        helper.assertTrue(post(helper.getLevel(), potatoes, ItemStack.EMPTY) == null,
                "AoE attached a provider to an unconfigured ordinary Overworld crop");
        ServerLevel tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        helper.assertTrue(post(tropical, Blocks.OAK_LEAVES.defaultBlockState(), new ItemStack(Items.SHEARS)) == null,
                "AoE replaced the native shears leaf harvest instead of delegating");
        AOEMod.LOGGER.info("Paired AoE/VillagerRealms loot verified: regional base 5 + profession extra 5 = 10; native fallback 1 + 1 = 2");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static Supplier<List<ItemStack>> post(ServerLevel level, BlockState state, ItemStack tool)
            throws ReflectiveOperationException {
        Class<?> eventType = type("loot.WorkerBlockLootEvent");
        Object event = eventType.getConstructor(ServerLevel.class, BlockPos.class, BlockState.class,
                        type("entity.VillagerRealmsVillager"), ResourceLocation.class, ItemStack.class)
                .newInstance(level, BlockPos.ZERO, state, null,
                        new ResourceLocation(AOEMod.MOD_ID, "test/paired_worker_harvest"), tool);
        MinecraftForge.EVENT_BUS.post((Event) event);
        return (Supplier<List<ItemStack>>) eventType.getMethod("getBaseRoll").invoke(event);
    }

    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName("com.villagerrealms." + name);
    }

    private static int count(List<ItemStack> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
