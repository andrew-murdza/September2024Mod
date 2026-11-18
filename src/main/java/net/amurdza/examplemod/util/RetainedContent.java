package net.amurdza.examplemod.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.amurdza.examplemod.future.FutureContentPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** The requested creative selection; dependencies stay registered for saved-world compatibility. */
public final class RetainedContent {
    private static final Map<String, List<String>> ITEMS;
    private static final Map<String, List<String>> ENTITIES;

    static {
        try (var stream = RetainedContent.class.getResourceAsStream("/aoemod-retained-content.json")) {
            if (stream == null) throw new IllegalStateException("Missing retained-content selection");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            ITEMS = readGroups(json.getAsJsonObject("items"));
            ENTITIES = readGroups(json.getAsJsonObject("entities"));
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private RetainedContent() { }

    private static Map<String, List<String>> readGroups(JsonObject object) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), java.util.stream.StreamSupport
                .stream(entry.getValue().getAsJsonArray().spliterator(), false).map(value -> value.getAsString()).toList()));
        return Map.copyOf(result);
    }

    public static boolean isKeptEntity(EntityType<?> type) {
        return isKeptEntity(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    public static boolean isKeptEntity(ResourceLocation id) {
        return id != null && !Set.of("aoemod:end_fish", "aoemod:sea_serpent").contains(id.toString())
                && matches(ENTITIES.get(id.getNamespace()), id.getPath());
    }

    public static boolean isKeptItem(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || stack.isEmpty()) return false;
        if (FutureContentPolicy.isExcludedItem(id)) return false;
        if (isExplicitlyRemovedItem(id)) return false;
        if (isRetiredSulfurEgg(stack, id)) return false;
        if (stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantments(stack).keySet().stream()
                .anyMatch(enchantment -> !"minecraft".equals(ForgeRegistries.ENCHANTMENTS.getKey(enchantment).getNamespace()))) return false;
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.TIPPED_ARROW)) {
            ResourceLocation potion = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(stack));
            if (potion == null || !potion.getNamespace().equals("minecraft")) return false;
            if (PotionUtils.getCustomEffects(stack).stream().anyMatch(effect ->
                    !"minecraft".equals(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).getNamespace()))) return false;
        }
        if (stack.is(Items.PAINTING) && stack.hasTag()) {
            ResourceLocation variant = ResourceLocation.tryParse(stack.getTag().getCompound("EntityTag").getString("variant"));
            if (variant != null && !variant.getNamespace().equals("minecraft")) return false;
        }
        if (id.toString().equals("quark:ancient_tome")) {
            var enchantment = org.violetmoon.quark.content.tools.module.AncientTomesModule.getTomeEnchantment(stack);
            return enchantment != null && "minecraft".equals(ForgeRegistries.ENCHANTMENTS.getKey(enchantment).getNamespace());
        }
        if (id.toString().equals("quark:seed_pouch")) {
            ItemStack contents = org.violetmoon.quark.content.tools.item.SeedPouchItem.getContents(stack).getContents();
            // Keep the useful empty pouch, without filling the catalog with preloaded variants.
            return contents.isEmpty();
        }
        if (stack.getItem() instanceof SpawnEggItem egg) return isKeptEntity(egg.getType(stack.getTag()));
        if (ObtainableContent.isInitialized() && stack.getItem() instanceof net.minecraft.world.item.BlockItem
                && !id.getNamespace().equals("minecraft") && !id.getNamespace().equals("vanillabackport")) {
            return ObtainableContent.contains(stack.getItem());
        }
        if (ObtainableContent.contains(stack.getItem())) return true;
        return isSelectedItem(stack);
    }

    /** The explicit non-block tools/foods are acquisition seeds, never the whole block catalog. */
    public static boolean isSelectedItem(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || stack.isEmpty() || FutureContentPolicy.isExcludedItem(id) || isExplicitlyRemovedItem(id)) return false;
        if (isRetiredSulfurEgg(stack, id)) return false;
        if (stack.getItem() instanceof SpawnEggItem egg) return isKeptEntity(egg.getType(stack.getTag()));
        if (id.getNamespace().equals("minecraft") || id.getNamespace().equals("aoemod")) return true;
        if (matches(ITEMS.get(id.getNamespace()), id.getPath())) return true;
        // Native bucket items consistently use one of these two conventions.
        String creature = id.getPath().endsWith("_bucket") ? id.getPath().substring(0, id.getPath().length() - 7)
                : id.getPath().startsWith("bucket_") ? id.getPath().substring(7) : null;
        return creature != null && isKeptEntity(new ResourceLocation(id.getNamespace(), creature));
    }

    /** Explicit removals also suppress recipes and recipe-derived creative entries. */
    public static boolean isExplicitlyRemovedItem(ResourceLocation id) {
        if (id == null) return false;
        String path = id.getPath();
        return id.toString().equals("ecologics:azalea_flower")
                || id.toString().equals("aoemod:mansion_key")
                || id.toString().equals("alexsmobs:unsettling_kimono")
                || isRemovedBygBlock(id)
                || id.getNamespace().equals("farmersdelight") && path.contains("onion")
                || isRemovedOrcItem(id)
                || isRemovedOuterEndItem(id)
                || isRemovedAquaticItem(id)
                || id.getNamespace().equals("culturaldelights") && !Set.of("squid", "glow_squid", "cooked_squid").contains(path)
                || id.getNamespace().equals("aoemod") && (path.contains("end_fish")
                || Set.of("raw_squid", "raw_glow_squid", "cooked_squid", "cooked_glow_squid", "raw_mahi_mahi").contains(path))
                || id.toString().equals("pineapple_delight:huge_pineapple")
                || id.getNamespace().equals("farmersdelight") && (path.equals("cutting_board")
                || path.equals("cooking_pot") || path.equals("flint_knife"))
                || id.getNamespace().equals("mynethersdelight") && path.endsWith("_cabinet")
                || id.getNamespace().equals("aoemod") && (path.contains("spider_crab_leg") || path.contains("sulfur_end_fish"))
                || id.toString().equals("hybrid_aquatic:cooked_crab")
                || id.getNamespace().equals("angling") && path.contains("anglerfish")
                || id.toString().equals("hybrid_aquatic:anglerfish")
                || id.getNamespace().equals("hapi") && path.contains("net")
                || id.getNamespace().equals("hybrid_aquatic") && path.contains("net")
                || path.contains("vertical_slab") || path.equals("trumpet_hedge") || path.equals("ashen_hedge")
                || id.getNamespace().equals("quark") && (path.endsWith("_blossom_hedge") || path.equals("ancient_hedge"))
                || id.getNamespace().equals("environmental") && path.contains("wisteria")
                || id.getNamespace().equals("environmental") && path.endsWith("_path")
                || id.toString().equals("vintagedelight:wild_peanuts") || id.toString().equals("farmersdelight:rice_panicle")
                || isRemovedLavaFishFood(id)
                || path.contains("crab_claw") || path.equals("ghoulflower")
                || path.contains("tiger_prawn") || path.contains("giant_river_prawn")
                || id.toString().equals("wildernature:flamingo_spawn_egg")
                || path.contains("peanut") && (path.contains("honey") || path.contains("honeyed"))
                || (path.contains("pie") || path.contains("cake")) && (path.contains("slice") || path.endsWith("_side"));
    }

    public static boolean isRemovedBygBlock(ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("biomeswevegone")) return false;
        return switch (id.getPath()) {
            case "allium_petal_block", "pink_allium_petal_block", "white_allium_petal_block",
                    "mossy_stone", "mossy_stone_slab", "mossy_stone_stairs", "mossy_stone_wall" -> true;
            default -> false;
        };
    }

    private static boolean isRemovedAquaticItem(ResourceLocation id) {
        String path = id.getPath();
        return switch (id.getNamespace()) {
            case "aoemod" -> Set.of("raw_mahi_mahi", "cooked_mahi_mahi", "raw_rubber_belly_glider",
                    "cooked_rubber_belly_glider", "cooked_carp").contains(path);
            case "angling" -> path.contains("mahi_mahi");
            case "finsandtails" -> path.contains("rubber_belly_glider");
            case "upgrade_aquatic" -> path.contains("lionfish");
            case "hybrid_aquatic" -> path.equals("mahi") || path.startsWith("mahi_") || path.equals("cooked_mahi")
                    || path.equals("carp") || path.startsWith("carp_") || path.equals("cooked_carp");
            default -> false;
        };
    }

    /** Restoring the dependency must not restore its unused blocks and equipment. */
    public static boolean isRemovedOuterEndItem(ResourceLocation id) {
        return id != null && id.getNamespace().equals("outer_end")
                && !Set.of("stalker_meat", "cooked_stalker_meat", "stalker_spawn_egg").contains(id.getPath());
    }

    public static boolean isRemovedOrcItem(ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("orcz")) return false;
        String path = id.getPath();
        if (path.endsWith("_spawn_egg")) {
            return !isKeptEntity(new ResourceLocation("orcz", path.substring(0, path.length() - "_spawn_egg".length())));
        }
        return !matches(ITEMS.get("orcz"), path);
    }

    private static boolean isRetiredSulfurEgg(ItemStack stack, ResourceLocation id) {
        if (!id.toString().equals("aoemod:spawn_egg_end_fish") || !stack.hasTag()) return false;
        CompoundTag entityTag = stack.getTag().getCompound("EntityTag");
        int variant = entityTag.getInt("Variant");
        return variant >= 5 && variant <= 7;
    }

    public static boolean isRemovedLavaFishFood(ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("netherdepthsupgrade")) return false;
        String path = id.getPath();
        return !path.endsWith("_bucket") && !path.endsWith("_spawn_egg")
                && (path.contains("blazefish") || path.contains("glowdine")
                || path.contains("magmacubefish") || path.contains("wither_bonefish"));
    }

    public static Set<String> namespaces() {
        Set<String> namespaces = new TreeSet<>(ITEMS.keySet());
        namespaces.addAll(ENTITIES.keySet());
        return Set.copyOf(namespaces);
    }

    private static boolean matches(List<String> patterns, String path) {
        if (patterns == null) return false;
        for (String pattern : patterns) {
            if (pattern.equals("*")) return true;
            boolean start = pattern.startsWith("*"), end = pattern.endsWith("*");
            String text = pattern.substring(start ? 1 : 0, pattern.length() - (end ? 1 : 0));
            if (start && end ? path.contains(text) : start ? path.endsWith(text) : end ? path.startsWith(text) : path.equals(text)) return true;
        }
        return false;
    }

    /** Explicit new placement requests override old tags and recipe-derived restrictions. */
    public static Integer placementMask(ResourceLocation id) {
        if (id == null) return null;
        String namespace = id.getNamespace(), path = id.getPath();
        return switch (namespace) {
            case "upgrade_aquatic" -> path.equals("mulberry_vine") ? BiomePlacementRestrictions.TROPICAL : null;
            case "orcz" -> isRemovedOrcItem(id) ? 0 : null;
            case "outer_end" -> isRemovedOuterEndItem(id) ? 0 : null;
            case "collectorsreap" -> path.contains("dragon") ? BiomePlacementRestrictions.DESERT : null;
            case "pineapple_delight" -> path.equals("pineapple_crop") || path.equals("pineapple_wild_crop") || path.equals("pineapple_pie")
                    ? BiomePlacementRestrictions.TROPICAL : null;
            case "hearthandharvest" -> {
                if (path.equals("corn_stalk")) yield BiomePlacementRestrictions.TROPICAL | BiomePlacementRestrictions.SAVANNA;
                if (path.equals("cotton") || path.equals("wild_cotton") || path.equals("cotton_bale")) yield BiomePlacementRestrictions.TROPICAL;
                yield null;
            }
            case "legumedelight" -> path.equals("wild_beans") || path.equals("beans") || path.equals("budding_beans") || path.equals("beans_on_rope")
                    ? BiomePlacementRestrictions.TROPICAL : null;
            case "vintagedelight" -> path.contains("peanut") ? BiomePlacementRestrictions.DESERT : null;
            case "ecologics" -> {
                if (path.contains("azalea")) yield BiomePlacementRestrictions.TROPICAL;
                if (path.contains("prickly_pear")) yield BiomePlacementRestrictions.DESERT;
                yield null;
            }
            case "hybrid_aquatic" -> path.startsWith("bull_kelp") ? BiomePlacementRestrictions.DESERT : null;
            case "alexscaves" -> {
                if (path.equals("smooth_bone") || path.equals("smooth_bone_slab")
                        || path.equals("smooth_bone_stairs") || path.equals("smooth_bone_wall")) {
                    yield BiomePlacementRestrictions.ALL;
                }
                yield path.equals("ping_pong_sponge") || path.equals("bone_worms") || path.startsWith("bioluminescent_")
                        ? BiomePlacementRestrictions.SNOWY : null;
            }
            case "biomesoplenty" -> path.contains("pink_daffodil") ? BiomePlacementRestrictions.TROPICAL : null;
            case "biomeswevegone" -> {
                if (isRemovedBygBlock(id)) yield 0;
                if (path.equals("sandy_farmland")) yield BiomePlacementRestrictions.DESERT;
                yield path.contains("japanese_orchid") ? BiomePlacementRestrictions.TROPICAL : null;
            }
            case "environmental" -> {
                if (path.contains("wisteria")) yield BiomePlacementRestrictions.TROPICAL;
                if (path.equals("mycelium_path") || path.equals("podzol_path")) yield BiomePlacementRestrictions.SNOWY;
                if (path.equals("dirt_path") || path.equals("grass_path")) yield BiomePlacementRestrictions.SAVANNA
                        | BiomePlacementRestrictions.PLAINS | BiomePlacementRestrictions.SNOWY;
                yield null;
            }
            case "betterend" -> Set.of("charnia_light_blue", "charnia_cyan").contains(path) ? BiomePlacementRestrictions.SNOWY : null;
            case "mynethersdelight" -> {
                if (path.equals("blackstone_bricks_cabinet")) yield BiomePlacementRestrictions.DESERT | BiomePlacementRestrictions.NETHER;
                if (path.startsWith("powdery_") || path.equals("powder_cannon")) yield BiomePlacementRestrictions.NETHER;
                yield null;
            }
            case "farmersdelight" -> path.contains("onion") ? 0 : null;
            case "nourished_nether" -> {
                if (path.startsWith("smooth_basalt")) yield BiomePlacementRestrictions.SNOWY | BiomePlacementRestrictions.DESERT | BiomePlacementRestrictions.NETHER;
                if (path.contains("soul_quartz") || path.equals("nether_brick_fence_gate")) yield BiomePlacementRestrictions.DESERT | BiomePlacementRestrictions.NETHER;
                if (path.equals("soul_weeds") || path.equals("ghoulflower") || path.startsWith("netherrack_")) yield BiomePlacementRestrictions.NETHER;
                yield null;
            }
            case "netherdepthsupgrade" -> {
                if (path.equals("lava_glass")) yield BiomePlacementRestrictions.SNOWY | BiomePlacementRestrictions.DESERT | BiomePlacementRestrictions.NETHER;
                if (path.contains("kelp") || path.contains("seagrass")) yield BiomePlacementRestrictions.NETHER;
                yield null;
            }
            case "quark" -> quarkMask(path);
            default -> null;
        };
    }

    private static Integer quarkMask(String path) {
        if (path.equals("charcoal_block")) return BiomePlacementRestrictions.ALL;
        if (path.startsWith("glow_shroom") || path.contains("calcite") || path.contains("dripstone")) return BiomePlacementRestrictions.SNOWY;
        if (path.contains("andesite") || path.contains("diorite") || path.contains("granite"))
            return BiomePlacementRestrictions.PLAINS | BiomePlacementRestrictions.SNOWY | BiomePlacementRestrictions.DESERT;
        if (path.contains("soul_sandstone") || path.contains("red_sandstone")) return BiomePlacementRestrictions.DESERT | BiomePlacementRestrictions.NETHER;
        if (path.contains("sandstone")) return BiomePlacementRestrictions.PLAINS | BiomePlacementRestrictions.SNOWY | BiomePlacementRestrictions.DESERT;
        if (path.equals("gold_bars")) return BiomePlacementRestrictions.PLAINS | BiomePlacementRestrictions.SNOWY | BiomePlacementRestrictions.DESERT | BiomePlacementRestrictions.NETHER;
        if (path.startsWith("paper_")) return BiomePlacementRestrictions.TROPICAL | BiomePlacementRestrictions.SAVANNA | BiomePlacementRestrictions.PLAINS | BiomePlacementRestrictions.SNOWY;
        if (path.endsWith("_hedge")) return BiomePlacementRestrictions.treeMask(path);
        return null;
    }
}
