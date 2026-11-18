package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.RetainedMobSpawns;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InventoryRemovalTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void bygBlocksKimonoAndLaviathanLoseTheirGameplaySources(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        for (String path : List.of("allium_petal_block", "pink_allium_petal_block", "white_allium_petal_block",
                "mossy_stone", "mossy_stone_slab", "mossy_stone_stairs", "mossy_stone_wall")) {
            var id = new ResourceLocation("biomeswevegone", path);
            var block = ForgeRegistries.BLOCKS.getValue(id);
            helper.assertTrue(block != null && block != net.minecraft.world.level.block.Blocks.AIR
                    && !RetainedContent.isKeptItem(block.asItem().getDefaultInstance())
                    && java.util.Objects.equals(RetainedContent.placementMask(id), 0),
                    "Restoring the dependency must preserve the prior allium/mossy-stone exclusion: " + id);
        }
        for (String path : List.of("allium", "pink_allium", "white_allium", "allium_petal_block",
                "mossy_stone_from_vine", "mossy_stone_slab_from_mossy_stone_stonecutting")) {
            helper.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("biomeswevegone", path)).isEmpty(),
                    "Removed crafting ingredient or result remains available in " + path);
        }
        helper.assertTrue(RetainedContent.isKeptItem(Items.ALLIUM.getDefaultInstance()), "Ordinary allium flower is hidden");

        var kimono = ForgeRegistries.ITEMS.getValue(new ResourceLocation("alexsmobs", "unsettling_kimono"));
        helper.assertTrue(kimono != null && kimono != Items.AIR
                && !RetainedContent.isKeptItem(kimono.getDefaultInstance()), "Kimono is still available");
        var murmur = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexsmobs", "murmur")).create(level);
        var murmurLoot = level.getServer().getLootData().getLootTable(new ResourceLocation("alexsmobs", "entities/murmur"));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, murmur)
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .withParameter(LootContextParams.ORIGIN, murmur.position()).create(LootContextParamSets.ENTITY);
        boolean wool = false;
        for (int seed = 1; seed <= 128; seed++) {
            for (var drop : murmurLoot.getRandomItems(params, seed)) {
                helper.assertFalse(drop.is(kimono), "Native Murmur loot still includes a kimono");
                wool |= drop.is(Items.RED_WOOL);
            }
        }
        helper.assertTrue(wool, "Murmur lost its red wool drop");

        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexsmobs", "laviathan"));
        var egg = ForgeRegistries.ITEMS.getValue(new ResourceLocation("alexsmobs", "spawn_egg_laviathan"));
        helper.assertTrue(type != null && egg instanceof SpawnEggItem spawnEgg && spawnEgg.getType(null) == type
                        && !RetainedContent.isKeptItem(egg.getDefaultInstance()),
                "Laviathan egg is still available");
        var join = new EntityJoinLevelEvent(type.create(level), level);
        RetainedMobSpawns.joined(join);
        helper.assertTrue(join.isCanceled(), "Laviathan can still enter a world");
        helper.succeed();
    }
}
