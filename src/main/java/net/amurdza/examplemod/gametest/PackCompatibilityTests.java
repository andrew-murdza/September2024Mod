package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PackCompatibilityTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void foodAndVerticalSlabTagsRetainPackMembers(GameTestHelper h) {
        itemInTag(h, "vintagedelight:gearo_berry", "forge:fruits");
        itemInTag(h, "fruitsdelight:pear", "forge:fruits");
        itemInTag(h, "minecraft:apple", "forge:fruits");
        itemInTag(h, "vintagedelight:peanut", "forge:protein");
        var vertical = TagKey.create(Registries.BLOCK, id("quark:vertical_slab"));
        var alias = TagKey.create(Registries.BLOCK, id("quark:vertical_slabs"));
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.defaultBlockState().is(vertical)) {
                h.assertTrue(block.defaultBlockState().is(alias), "Plural Quark tag lost " + BuiltInRegistries.BLOCK.getKey(block));
                ++checked;
            }
        }
        h.assertTrue(checked > 0, "The real Quark vertical-slab tag must be populated");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void compatibilityLootPreservesShearingAndServingDrops(GameTestHelper h) {
        for (String blockId : new String[]{
                "hearthandharvest:wild_cotton", "hearthandharvest:wild_red_grapes",
                "hearthandharvest:wild_green_grapes", "hearthandharvest:wild_peanuts",
                "hearthandharvest:wild_hops", "culturaldelights:avocado_leaves",
                "culturaldelights:fruiting_avocado_leaves", "culturaldelights:exotic_roll_medley"}) {
            var key = id(blockId);
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(key), "Required pack block missing: " + blockId);
            var block = BuiltInRegistries.BLOCK.get(key);
            var drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), h.absolutePos(BlockPos.ZERO),
                    null, null, new ItemStack(Items.SHEARS));
            var item = BuiltInRegistries.ITEM.get(key);
            h.assertTrue(drops.stream().anyMatch(stack -> stack.is(item) && stack.getCount() == 1),
                    "Shearing/full serving must preserve its block drop: " + blockId + " got " + drops);
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void compatibleAdvancementsAndReplacementStewsLoad(GameTestHelper h) {
        var server = h.getLevel().getServer();
        for (String advancement : new String[]{"outer_end:end/find_catacombs", "nourished_nether:strange_elixir_advancement"}) {
            h.assertTrue(server.getAdvancements().getAdvancement(id(advancement)) != null,
                    "Missing functional advancement " + advancement);
        }
        for (String recipe : new String[]{"aoemod:food/mushroom_stew", "aoemod:food/rabbit_stew"}) {
            h.assertTrue(server.getRecipeManager().byKey(id(recipe)).isPresent(), "AoE replacement recipe missing " + recipe);
        }
        for (String recipe : new String[]{"minecraft:mushroom_stew", "minecraft:rabbit_stew_from_brown_mushroom", "minecraft:rabbit_stew_from_red_mushroom"}) {
            h.assertTrue(server.getRecipeManager().byKey(id(recipe)).isEmpty(), "Disabled vanilla duplicate unexpectedly restored " + recipe);
        }
        h.assertTrue(server.getRecipeManager().getRecipes().stream().allMatch(recipe -> {
            String namespace = recipe.getId().getNamespace();
            return namespace.equals("minecraft") || namespace.equals("aoemod") || namespace.equals("quark")
                    || namespace.equals("vanillabackport") || namespace.equals("tiny_takeover_backport");
        }), "Existing recipe namespace policy must remain enforced");
        h.succeed();
    }

    private static void itemInTag(GameTestHelper h, String item, String tag) {
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id(item)), "Required pack item missing: " + item);
        h.assertTrue(new ItemStack(BuiltInRegistries.ITEM.get(id(item))).is(TagKey.create(Registries.ITEM, id(tag))),
                item + " missing from " + tag);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
