package net.amurdza.examplemod.util;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.config.MobConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/** Keeps harvested materials and every reachable loaded recipe in the source catalogs. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class ObtainableContent {
    private static volatile Set<Item> discovered = Set.of();
    private static volatile boolean initialized;

    private ObtainableContent() { }

    public static boolean contains(Item item) {
        return discovered.contains(item);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static synchronized void rebuild(RecipeManager recipes, RegistryAccess registries) {
        Set<Item> reachable = new HashSet<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (RetainedContent.isSelectedItem(new ItemStack(item))
                    && (!(item instanceof BlockItem) || id.getNamespace().equals("minecraft")
                    || id.getNamespace().equals("vanillabackport")
                    // Native Overworld camp materials have no custom-dimension source tag.
                    || id.getNamespace().equals("orcz"))) reachable.add(item);
        }
        // These tags describe the actual dimension sources, including technical crop
        // states whose asItem() is their seed rather than a same-named block item.
        for (var block : ForgeRegistries.BLOCKS.getValues()) {
            if (DocumentedPlacementRules.sourceMask(block) != null) add(reachable, block.asItem());
        }
        // Wild bean and spider cave loot tables supply this planting-only BlockItem directly.
        add(reachable, net.amurdza.examplemod.registry.ModItems.BEAN_SEEDS.get());
        // Adult cassowaries lay the native hatchable egg directly, outside recipes.
        add(reachable, ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("biodiverse:cassowary_egg")));
        // Tilling sand is an acquisition path outside the recipe graph.
        add(reachable, ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("biomeswevegone:sandy_farmland")));
        MobConfig.MOB_INFO_BY_MOB.forEach((entity, info) -> {
            if (RetainedContent.isKeptEntity(entity)) info.drops().forEach(drop -> {
                add(reachable, drop.item());
                add(reachable, drop.cookedItem());
            });
        });
        // An unseeded recipe cycle cannot manufacture availability. Ingredient tags
        // are alternatives, whereas every ingredient slot must have a reachable choice.
        boolean changed;
        do {
            changed = false;
            // A configured crop yield is reachable only once its seed/plant is.
            // Otherwise a removed crop could bootstrap itself from its own harvest.
            for (var entry : BlockConfig.BLOCK_INFO_BY_BLOCK.entrySet()) {
                var block = entry.getKey();
                if (!reachable.contains(block.asItem()) && DocumentedPlacementRules.sourceMask(block) == null) continue;
                for (var drop : entry.getValue().drops()) changed |= add(reachable, drop.item());
            }
            // Stripping uses an axe rather than a recipe. Keep that acquisition
            // path for the native azalea and custom pale oak wood families.
            for (Item item : Set.copyOf(reachable)) {
                var id = ForgeRegistries.ITEMS.getKey(item);
                if (!id.getPath().startsWith("stripped_") && (id.getPath().endsWith("_log") || id.getPath().endsWith("_wood"))) {
                    var stripped = new net.minecraft.resources.ResourceLocation(id.getNamespace(), "stripped_" + id.getPath());
                    if (ForgeRegistries.ITEMS.containsKey(stripped)) changed |= add(reachable, ForgeRegistries.ITEMS.getValue(stripped));
                }
                if (id.getPath().contains("coral") && !id.getPath().startsWith("dead_")) {
                    var dead = new net.minecraft.resources.ResourceLocation(id.getNamespace(), "dead_" + id.getPath());
                    if (ForgeRegistries.ITEMS.containsKey(dead)) changed |= add(reachable, ForgeRegistries.ITEMS.getValue(dead));
                }
            }
            for (var recipe : recipes.getRecipes()) {
                if (recipe instanceof SmithingTrimRecipe || recipe.getIngredients().isEmpty()) continue;
                boolean hasIngredient = false;
                boolean available = true;
                for (var ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    hasIngredient = true;
                    boolean alternative = false;
                    for (ItemStack stack : ingredient.getItems()) alternative |= reachable.contains(stack.getItem());
                    if (!alternative) {
                        available = false;
                        break;
                    }
                }
                if (available && hasIngredient) changed |= add(reachable, recipe.getResultItem(registries).getItem());
            }
        } while (changed);
        discovered = Set.copyOf(reachable);
        initialized = true;
        net.amurdza.examplemod.mixins.accessor.CreativeTabContentsCache.aoemod$setCachedParameters(null);
    }

    private static boolean add(Set<Item> items, Item item) {
        if (item == null || item == Items.AIR || item instanceof SpawnEggItem) return false;
        var id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && !RetainedContent.isExplicitlyRemovedItem(id)
                && !net.amurdza.examplemod.future.FutureContentPolicy.isExcludedItem(id) && items.add(item);
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        rebuild(event.getServer().getRecipeManager(), event.getServer().registryAccess());
    }

    @SubscribeEvent
    public static void recipesReloaded(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null && initialized) return;
        var server = event.getPlayerList().getServer();
        rebuild(server.getRecipeManager(), server.registryAccess());
    }
}
