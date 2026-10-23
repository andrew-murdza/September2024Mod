package net.amurdza.examplemod.future;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.Set;

/** Keeps the requested backports from introducing a second copy of existing AOE content. */
public final class FutureContentPolicy {
    private static final Set<String> EXCLUDED_ENTITIES = Set.of(
            "creaking", "nautilus", "zombie_nautilus", "happy_ghast");
    private static final Set<String> COPPER_EQUIPMENT = Set.of(
            "copper_sword", "copper_shovel", "copper_pickaxe", "copper_axe", "copper_hoe",
            "copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots");
    private static final Set<String> SPEARS = Set.of(
            "wooden_spear", "stone_spear", "copper_spear", "iron_spear", "golden_spear",
            "diamond_spear", "netherite_spear");
    private static final Map<String, String> REPLACED_MOB_EGGS = Map.of(
            "parched_spawn_egg", "parched_spawn_egg",
            "camel_husk_spawn_egg", "camel_husk_spawn_egg",
            "camel_husk_jockey_spawn_egg", "camel_husk_spawn_egg");

    private FutureContentPolicy() {
    }

    public static boolean enabled() {
        return ModList.get().isLoaded("vanillabackport");
    }

    public static boolean isExcludedItem(ResourceLocation id) {
        if (!enabled()) return false;
        String path = id.getPath();
        if (id.getNamespace().equals(AOEMod.MOD_ID)) {
            return SPEARS.contains(path) || path.equals("copper_chain") || REPLACED_MOB_EGGS.containsKey(path);
        }
        if (!id.getNamespace().equals("minecraft")) return false;
        return COPPER_EQUIPMENT.contains(path)
                || path.equals("dried_ghast") || path.endsWith("_harness")
                || path.equals("happy_ghast_spawn_egg")
                || path.equals("nautilus_spawn_egg") || path.equals("zombie_nautilus_spawn_egg")
                || path.endsWith("_nautilus_armor") || path.equals("creaking_spawn_egg")
                || path.equals("creaking_heart") || path.contains("resin")
                || (path.contains("pale_oak") && !path.equals("pale_oak_shelf"))
                || path.startsWith("pale_moss") || path.equals("pale_hanging_moss")
                || path.endsWith("eyeblossom")
                || path.equals("music_disc_tears") || path.equals("music_disc_lava_chicken");
    }

    /** Recipe ingredients use existing AOE copper equipment; old spears use the complete backport. */
    public static ResourceLocation canonicalItem(ResourceLocation id) {
        if (!enabled()) return id;
        if (id.getNamespace().equals(AOEMod.MOD_ID) && REPLACED_MOB_EGGS.containsKey(id.getPath())) {
            return new ResourceLocation("minecraft", REPLACED_MOB_EGGS.get(id.getPath()));
        }
        if (id.getNamespace().equals("minecraft") && COPPER_EQUIPMENT.contains(id.getPath())) {
            return new ResourceLocation(AOEMod.MOD_ID, id.getPath());
        }
        if (id.getNamespace().equals(AOEMod.MOD_ID)
                && (SPEARS.contains(id.getPath()) || id.getPath().equals("copper_chain"))) {
            return new ResourceLocation("minecraft", id.getPath());
        }
        return id;
    }

    public static ItemStack canonicalStack(ItemStack original) {
        if (original.isEmpty()) return original;
        ResourceLocation oldId = BuiltInRegistries.ITEM.getKey(original.getItem());
        ResourceLocation newId = canonicalItem(oldId);
        if (oldId.equals(newId) || !BuiltInRegistries.ITEM.containsKey(newId)) return original;
        Item replacement = BuiltInRegistries.ITEM.get(newId);
        ItemStack converted = new ItemStack(replacement, original.getCount());
        if (original.hasTag()) converted.setTag(original.getTag().copy());
        return converted;
    }

    public static void normalizeRecipe(JsonElement value) {
        if (!enabled()) return;
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(FutureContentPolicy::normalizeRecipe);
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement child = entry.getValue();
                if ((entry.getKey().equals("item") || entry.getKey().equals("result"))
                        && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    ResourceLocation id = ResourceLocation.tryParse(child.getAsString());
                    if (id != null) entry.setValue(new com.google.gson.JsonPrimitive(canonicalItem(id).toString()));
                } else {
                    normalizeRecipe(child);
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class EntityEvents {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void preventExcludedEntities(EntityJoinLevelEvent event) {
            if (!enabled()) return;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            if (id.getNamespace().equals("minecraft") && EXCLUDED_ENTITIES.contains(id.getPath())) {
                event.setCanceled(true);
                return;
            }
            if (event.getEntity() instanceof ItemEntity itemEntity) {
                itemEntity.setItem(canonicalStack(itemEntity.getItem()));
            }
            if (event.getEntity() instanceof LivingEntity livingEntity) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack previous = livingEntity.getItemBySlot(slot);
                    ItemStack replacement = canonicalStack(previous);
                    if (previous != replacement) livingEntity.setItemSlot(slot, replacement);
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class CreativeEvents {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void hideExcludedItems(BuildCreativeModeTabContentsEvent event) {
            var entries = event.getEntries().iterator();
            while (entries.hasNext()) {
                if (isExcludedItem(BuiltInRegistries.ITEM.getKey(entries.next().getKey().getItem()))) {
                    entries.remove();
                }
            }
        }
    }
}
