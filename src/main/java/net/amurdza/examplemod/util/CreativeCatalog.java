package net.amurdza.examplemod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** One source-mod catalog per category, with search and vanilla categories kept separate. */
public final class CreativeCatalog {
    private CreativeCatalog() { }

    private static final class VanillaItems {
        private static final Set<Item> ITEMS = load();

        private static Set<Item> load() {
            Set<Item> items = Collections.newSetFromMap(new IdentityHashMap<>());
            // The pinned Minecraft Items class declares the original vanilla items.
            // Backports additionally register minecraft: IDs through their own registries.
            for (var field : Items.class.getFields()) {
                if (field.getType() != Item.class || !Modifier.isStatic(field.getModifiers())) continue;
                try {
                    items.add((Item) field.get(null));
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot identify original vanilla item " + field.getName(), exception);
                }
            }
            return items;
        }
    }

    public static String owner(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return "";
        if (id.getNamespace().equals("minecraft")) return VanillaItems.ITEMS.contains(stack.getItem()) ? "minecraft" : "vanillabackport";
        if (id.getNamespace().equals("betterend")) return "aoemod";
        if (id.getNamespace().equals("aoemod") && id.getPath().startsWith("spawn_egg_orcz_")) return "orcz";
        if (id.toString().equals("aoemod:spawn_egg_goblin_knight")) return "twilightforest";
        if (id.toString().equals("aoemod:bucket_cubozoa") || id.toString().equals("aoemod:spawn_egg_cubozoa")) return "netherdepthsupgrade";
        return id.getNamespace();
    }

    public static Stream<ItemStack> items(String namespace) {
        return ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> namespace.equals(owner(new ItemStack(item))))
                .flatMap(item -> item instanceof org.violetmoon.zeta.registry.CreativeTabManager.AppendsUniquely variants
                        ? variants.appendItemsToCreativeTab().stream() : Stream.of(new ItemStack(item)))
                .filter(RetainedContent::isKeptItem);
    }

    public static List<CreativeModeTab> visibleTabs(List<CreativeModeTab> sorted) {
        return sorted.stream().filter(CreativeModeTab::shouldDisplay).toList();
    }
}
