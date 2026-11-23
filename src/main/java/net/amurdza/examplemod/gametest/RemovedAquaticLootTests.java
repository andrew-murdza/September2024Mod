package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RemovedAquaticLootTests {
    private RemovedAquaticLootTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void beachArchaeologyCannotRestoreRemovedCrabClaws(GameTestHelper helper) {
        verifyLoadedTable(helper, "beach_archaeology", 1, Set.of(
                "minecraft:stick", "hybrid_aquatic:shark_tooth", "hybrid_aquatic:sea_urchin_spine",
                "hybrid_aquatic:bull_kelp", "hybrid_aquatic:delesseria", "hybrid_aquatic:sargassum",
                "hybrid_aquatic:cuttlebone", "hybrid_aquatic:coral_chunk", "minecraft:prismarine_shard",
                "minecraft:prismarine_crystals", "hybrid_aquatic:ghost_crab_claw", "hybrid_aquatic:dungeness_crab_claw",
                "hybrid_aquatic:hybrid_crate", "hybrid_aquatic:oak_crate", "hybrid_aquatic:dark_oak_crate",
                "hybrid_aquatic:spruce_crate", "hybrid_aquatic:birch_crate", "hybrid_aquatic:acacia_crate",
                "hybrid_aquatic:jungle_crate", "hybrid_aquatic:mangrove_crate", "hybrid_aquatic:cherry_crate",
                "hybrid_aquatic:bamboo_crate", "minecraft:nautilus_shell", "minecraft:scute",
                "minecraft:turtle_egg", "minecraft:heart_of_the_sea", "hybrid_aquatic:diving_armor_upgrade_template"));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void existingCrabPotsCannotRestoreRemovedCrabClaws(GameTestHelper helper) {
        verifyLoadedTable(helper, "crab_pot_treasure", 3, Set.of(
                "hybrid_aquatic:ghost_crab_claw", "hybrid_aquatic:yeti_crab_claw", "hybrid_aquatic:spider_crab_claw",
                "hybrid_aquatic:vampire_crab_claw", "hybrid_aquatic:dungeness_crab_claw", "hybrid_aquatic:fiddler_crab_claw",
                "hybrid_aquatic:raw_shrimp", "hybrid_aquatic:lobster_claw", "hybrid_aquatic:raw_lobster_tail"));
        helper.succeed();
    }

    private static void verifyLoadedTable(GameTestHelper helper, String name, int maximumCount, Set<String> expected) {
        var level = helper.getLevel();
        var id = new ResourceLocation("hybrid_aquatic", "gameplay/" + name);
        var table = level.getServer().getLootData().getLootTable(id);
        helper.assertTrue(table != LootTable.EMPTY, "Loot override failed to load: " + id);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        Set<String> observed = new HashSet<>();
        for (long roll = 1; roll <= 4096; roll++) {
            // Widely separated fixed seeds exercise the loaded loot pipeline reproducibly.
            var drops = table.getRandomItems(params, roll * 0x9E3779B97F4A7C15L);
            helper.assertTrue(drops.size() == 1, "The native single-reward pool must remain intact: " + id);
            var stack = drops.get(0);
            String item = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            helper.assertTrue(expected.contains(item), "Removed or unexpected reward leaked from " + id + ": " + item);
            helper.assertTrue(stack.getCount() >= 1 && stack.getCount() <= maximumCount,
                    "Native reward count changed for " + id + ": " + stack);
            observed.add(item);
        }
        helper.assertTrue(observed.equals(expected), "Unchanged native rewards must still be obtainable from " + id + ": " + observed);
    }
}
