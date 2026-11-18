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
    public static void compatibilityLootPreservesRetainedShearingDrops(GameTestHelper h) {
        for (String blockId : new String[]{
                "hearthandharvest:wild_cotton", "hearthandharvest:wild_red_grapes",
                "hearthandharvest:wild_green_grapes", "hearthandharvest:wild_peanuts",
                "hearthandharvest:wild_hops"}) {
            var key = id(blockId);
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(key), "Required pack block missing: " + blockId);
            var block = BuiltInRegistries.BLOCK.get(key);
            var drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), h.absolutePos(BlockPos.ZERO),
                    null, null, new ItemStack(Items.SHEARS));
            var item = BuiltInRegistries.ITEM.get(key);
            h.assertTrue(drops.stream().anyMatch(stack -> stack.is(item) && stack.getCount() == 1),
                    "Shearing must preserve its block drop: " + blockId + " got " + drops);
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void compatibleAdvancementsAndReplacementStewsLoad(GameTestHelper h) {
        var server = h.getLevel().getServer();
        for (String advancement : new String[]{"nourished_nether:strange_elixir_advancement"}) {
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
                    || namespace.equals("vanillabackport") || namespace.equals("tiny_takeover_backport") || namespace.equals("orcz")
                    || net.amurdza.examplemod.util.RetainedContent.isSelectedItem(recipe.getResultItem(h.getLevel().registryAccess()));
        }), "Native recipes may only be restored for explicitly retained outputs");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void retiredDependenciesLeaveNoDataPacksOrBiomeSpawns(GameTestHelper h) {
        var retired = java.util.Set.of("born_in_chaos_v1", "untamedwilds", "naturalist",
                "bountiful_critters", "dropthemeat", "dtmeat", "ferdinandsflowers", "culturaldelights", "iceandfire");
        var server = h.getLevel().getServer();
        h.assertTrue(java.util.Collections.disjoint(server.getResourceManager().getNamespaces(), retired),
                "Retired dependencies must not leave a data namespace in the active pack");
        for (var biome : server.registryAccess().registryOrThrow(Registries.BIOME)) {
            for (var category : net.minecraft.world.entity.MobCategory.values()) {
                for (var spawn : biome.getMobSettings().getMobs(category).unwrap()) {
                    h.assertFalse(retired.contains(BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type).getNamespace()),
                            "A loaded biome still spawns a retired dependency entity");
                }
            }
        }
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
