package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DimensionMaterialAvailability;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionLootTests {
    private static final ResourceLocation REGIONAL_TABLE = AOEMod.makeID("chests/illager_mansion");
    @GameTest(template = "plant_gameplay_empty")
    public static void mansionLootContainsOnlyPlainsMaterials(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var table = level.getServer().getLootData().getLootTable(REGIONAL_TABLE);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        var seen = new HashSet<net.minecraft.world.item.Item>();
        for (int seed = 1; seed <= 1024; seed++) {
            var drops = table.getRandomItems(params, seed);
            helper.assertTrue(!drops.isEmpty(), "Mansion loot table must load and generate supplies");
            for (var stack : drops) {
                seen.add(stack.getItem());
                helper.assertFalse(stack.is(Items.ROTTEN_FLESH), "Mansion supplies must use cooked livestock meat instead of rotten flesh");
                helper.assertTrue(DimensionMaterialAvailability.isAvailable(level, stack.getItem(), BiomePlacementRestrictions.PLAINS),
                        "Mansion treasure contains material unavailable in Plains: " + ForgeRegistries.ITEMS.getKey(stack.getItem()));
            }
        }
        helper.assertTrue(seen.containsAll(java.util.Set.of(Items.BREAD, Items.BAKED_POTATO,
                        Items.CROSSBOW, Items.IRON_AXE, Items.COAL, Items.GOLD_INGOT, Items.COPPER_INGOT)),
                "Upper rooms need prepared food and regional equipment and metals");
        helper.assertTrue(seen.containsAll(java.util.Set.of(Items.AZURE_BLUET, Items.WHITE_WOOL,
                        Items.LIGHT_GRAY_WOOL, Items.GRAY_WOOL, Items.BLACK_WOOL)),
                "The upstairs decoration pool must include Plains flowers and all four colors on the mansion banners");
        var pantry = level.getServer().getLootData().getLootTable(AOEMod.makeID("chests/illager_mansion_pantry"));
        var staples = new HashSet<net.minecraft.world.item.Item>();
        for (int seed = 1; seed <= 256; seed++) pantry.getRandomItems(params, seed).forEach(stack -> staples.add(stack.getItem()));
        helper.assertTrue(staples.containsAll(java.util.Set.of(Items.EGG, Items.CHICKEN, Items.PUMPKIN,
                        Items.BEETROOT, Items.SUGAR_CANE, Items.WHEAT_SEEDS, Items.MUSHROOM_STEW)),
                "The granary must stock the crops and livestock kept in the basement");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void illagerLordAwardsIronGoldAndOneAxe(GameTestHelper helper) {
        var level = helper.getLevel();
        var table = level.getServer().getLootData().getLootTable(AOEMod.makeID("chests/bosses/illager_lord"));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        for (int seed = 1; seed <= 128; seed++) {
            var totals = new java.util.HashMap<net.minecraft.world.item.Item, Integer>();
            for (var stack : table.getRandomItems(params, seed)) totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
            helper.assertTrue(totals.size() == 3 && totals.getOrDefault(Items.GOLD_INGOT, 0) >= 16
                    && totals.getOrDefault(Items.IRON_INGOT, 0) >= 24
                    && totals.getOrDefault(net.amurdza.examplemod.registry.ModItems.BOSS_AXE.get(), 0) == 1,
                    "The Lord's reward must contain both metals and exactly one unique axe");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void unopenedSavedMansionChestsUseRegionalLootWithoutChangingOpenedChests(GameTestHelper helper) {
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        for (ServerLevel level : java.util.List.of(plains, helper.getLevel())) {
            for (long seed = 1; seed <= 12; seed++) {
                ChestBlockEntity saved = chest(level);
                CompoundTag tag = new CompoundTag();
                tag.putString("LootTable", BuiltInLootTables.WOODLAND_MANSION.toString());
                tag.putLong("LootTableSeed", seed);
                saved.load(tag);
                ChestBlockEntity expected = chest(level);
                var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.ZERO))
                        .create(LootContextParamSets.CHEST);
                level.getServer().getLootData().getLootTable(level == plains ? REGIONAL_TABLE : BuiltInLootTables.WOODLAND_MANSION)
                        .fill(expected, params, seed);
                for (int slot = 0; slot < saved.getContainerSize(); slot++) {
                    helper.assertTrue(ItemStack.matches(saved.getItem(slot), expected.getItem(slot)),
                            "Saved native mansion loot must switch only in Plains and preserve deterministic seed/slot rolls");
                }
            }
        }
        ChestBlockEntity opened = chest(plains);
        opened.setItem(0, new ItemStack(Items.DIAMOND, 3));
        opened.unpackLootTable(null);
        helper.assertTrue(opened.getItem(0).is(Items.DIAMOND) && opened.getItem(0).getCount() == 3,
                "Existing inventories must not be rewritten");
        helper.succeed();
    }

    private static ChestBlockEntity chest(ServerLevel level) {
        var chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        chest.setLevel(level);
        return chest;
    }
}
