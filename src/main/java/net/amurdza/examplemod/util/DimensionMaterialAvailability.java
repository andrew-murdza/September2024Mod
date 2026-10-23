package net.amurdza.examplemod.util;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static net.amurdza.examplemod.util.BiomePlacementRestrictions.*;

/** Computes recipe reachability once per recipe reload, not once per block placement. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class DimensionMaterialAvailability {
    private static final Map<RecipeManager, Map<Item, Integer>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<String> NATURAL_BLOCKS = Set.of(
            "stone", "cobblestone", "andesite", "diorite", "granite", "deepslate", "cobbled_deepslate", "tuff", "calcite", "polished_basalt", "terracotta",
            "dirt", "coarse_dirt", "rooted_dirt", "grass_block", "mycelium", "moss_block", "rich_soil",
            "sand", "red_sand", "gravel", "clay", "mud", "netherrack", "soul_sand", "soul_soil",
            "blackstone", "basalt", "smooth_basalt", "obsidian", "crying_obsidian", "magma_block", "glowstone",
            "snow", "snow_block", "ice", "packed_ice", "blue_ice", "powder_snow", "ancient_debris",
            "amethyst_block", "budding_amethyst", "dripstone_block", "pointed_dripstone", "end_stone",
            "red_mushroom_block", "brown_mushroom_block", "mushroom_stem", "warped_wart_block", "nether_wart_block",
            "shroomlight", "bee_nest", "sponge", "wet_sponge", "sculk", "sculk_catalyst", "sculk_shrieker", "sculk_sensor");

    private DimensionMaterialAvailability() { }

    public static boolean isAvailable(LevelReader reader, Item item, int dimension) {
        if (item == Items.AIR) return true;
        Level level = reader instanceof Level world ? world : reader instanceof ServerLevelAccessor accessor ? accessor.getLevel() : null;
        if (level == null) return true;
        Map<Item, Integer> masks = CACHE.computeIfAbsent(level.getRecipeManager(), manager -> calculate(level));
        return (masks.getOrDefault(item, ALL) & dimension) != 0;
    }

    @SubscribeEvent
    public static void onTagsReload(TagsUpdatedEvent event) {
        BiomePlacementRestrictions.clearCache();
        CACHE.clear();
    }

    @SubscribeEvent
    public static void onRecipesReload(OnDatapackSyncEvent event) { CACHE.clear(); }

    public static void clearCache() { CACHE.clear(); }

    private static Map<Item, Integer> calculate(Level level) {
        List<Recipe<?>> recipes = new ArrayList<>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            // A trim recipe's displayed iron chestplate is a preview, not a manufactured output.
            if (!(recipe instanceof SmithingTrimRecipe)) recipes.add(recipe);
        }
        Set<Item> outputs = new HashSet<>();
        for (Recipe<?> recipe : recipes) {
            Item output = recipe.getResultItem(level.registryAccess()).getItem();
            if (output != Items.AIR && !recipe.getIngredients().isEmpty()) outputs.add(output);
        }
        Map<Item, Integer> masks = new HashMap<>();
        Map<Item, Integer> caps = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            String path = ForgeRegistries.ITEMS.getKey(item).getPath();
            int cap = item instanceof BlockItem block ? allowedMask(block.getBlock()) : treeMask(path);
            Integer material = materialMask(path);
            if (material != null && !path.equals("stick")) cap &= material;
            caps.put(item, cap);
            boolean natural = !outputs.contains(item) || NATURAL_BLOCKS.contains(path) || path.endsWith("_ore")
                    || path.endsWith("_log") || path.endsWith("_stem") || path.endsWith("_leaves")
                    || path.endsWith("_sapling") || path.endsWith("_propagule") || path.endsWith("nylium")
                    || material != null || path.equals("note_block") || isHarvestedPlant(item)
                    || item instanceof BlockItem block && DocumentedPlacementRules.sourceMask(block.getBlock()) != null
                    || path.equals("crimson_kelp") || path.equals("warped_kelp")
                    || (ForgeRegistries.ITEMS.getKey(item).getNamespace().equals("quark")
                    && path.endsWith("_corundum") && !path.startsWith("waxed_"));
            masks.put(item, path.equals("stick") ? DESERT : natural ? cap : 0);
        }
        // A fixed point handles chains, ingredient alternatives, stonecutting and smelting,
        // without allowing an unseeded compression/decompression cycle to create a resource.
        boolean changed;
        do {
            changed = false;
            for (Recipe<?> recipe : recipes) {
                Item output = recipe.getResultItem(level.registryAccess()).getItem();
                if (output == Items.AIR || recipe.getIngredients().isEmpty()) continue;
                int available = ALL;
                boolean nonempty = false;
                for (var ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    nonempty = true;
                    int alternatives = 0;
                    for (var stack : ingredient.getItems()) alternatives |= masks.getOrDefault(stack.getItem(), 0);
                    available &= alternatives;
                }
                if (!nonempty) continue;
                if ((available & NETHER) != 0 && isNonlivingNetherProduct(output)) available |= DESERT;
                int previous = masks.getOrDefault(output, 0);
                int next = previous | (available & caps.getOrDefault(output, ALL));
                if (next != previous) {
                    masks.put(output, next);
                    changed = true;
                }
            }
        } while (changed);
        return Map.copyOf(masks);
    }

    private static boolean isNonlivingNetherProduct(Item item) {
        String path = ForgeRegistries.ITEMS.getKey(item).getPath();
        if (treeMask(path) != ALL || item.getDefaultInstance().is(ItemTags.PLANKS)
                || item.getDefaultInstance().is(ItemTags.LOGS) || path.equals("stick") || path.equals("bowl")) return false;
        // Transfer mineral building products; wood-dependent furniture keeps its recipe requirements.
        return item instanceof BlockItem block && (block.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                || path.contains("brick") || path.contains("glass"));
    }

    private static boolean isHarvestedPlant(Item item) {
        if (!(item instanceof BlockItem blockItem)) return false;
        var block = blockItem.getBlock();
        return block instanceof BushBlock || block instanceof GrowingPlantBlock
                || block instanceof CactusBlock || block instanceof SugarCaneBlock || block instanceof BambooStalkBlock;
    }

    private static Integer materialMask(String p) {
        // Harvested ingredients and the explicitly allowed transfers seed recipe derivation.
        return switch (p) {
            case "raw_iron", "iron_ingot", "iron_nugget" -> PLAINS | SNOWY;
            case "raw_copper", "copper_ingot", "copper_nugget",
                 "raw_gold", "gold_ingot", "gold_nugget", "coal" -> PLAINS | SNOWY | DESERT | NETHER;
            case "diamond", "emerald", "lapis_lazuli" -> SNOWY;
            case "redstone", "quartz", "glowstone_dust", "netherite_scrap", "netherite_ingot" -> DESERT | NETHER;
            case "charcoal" -> TROPICAL | SAVANNA | PLAINS | SNOWY | DESERT | NETHER;
            case "stick" -> DESERT;
            case "flint" -> PLAINS | SNOWY | DESERT;
            case "honeycomb", "honey_bottle" -> TROPICAL | SAVANNA | PLAINS;
            case "blaze_rod", "blaze_powder" -> DESERT | NETHER;
            case "string", "slime_ball", "white_wool" -> ALL;
            case "leather", "rabbit_hide", "rabbit_foot", "gunpowder", "bone", "apple", "feather", "egg",
                 "ink_sac", "glow_ink_sac", "rotten_flesh", "cod", "salmon", "pufferfish", "tropical_fish",
                 "rabbit", "beef", "mutton", "porkchop", "chicken" -> ALL;
            case "clay_ball" -> PLAINS | SNOWY;
            case "cocoa_beans" -> TROPICAL;
            case "amethyst_shard", "prismarine_shard", "prismarine_crystals", "snowball" -> SNOWY;
            case "wheat", "wheat_seeds", "carrot", "potato" -> TROPICAL | SAVANNA | PLAINS;
            case "beetroot", "beetroot_seeds" -> SAVANNA | PLAINS | SNOWY;
            case "soul_beetroot", "soul_beetroot_seeds" -> NETHER;
            case "chorus_fruit", "popped_chorus_fruit", "shulker_shell", "dragon_breath", "ender_pearl" -> DESERT;
            default -> null;
        };
    }
}
