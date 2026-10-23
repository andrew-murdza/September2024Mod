package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Shared by item placement, plant spreading, and runtime block changes. */
public final class BiomePlacementRestrictions {
    public static final int TROPICAL = 1, SAVANNA = 2, PLAINS = 4, SNOWY = 8, DESERT = 16, NETHER = 32;
    public static final int ALL = 63;
    private static final Map<Block, Integer> MASK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> GROUPS = Map.ofEntries(
            Map.entry("aoe_jungle_only", TROPICAL), Map.entry("crimson_only", NETHER),
            Map.entry("deep_dark_only", DESERT), Map.entry("desert_only", DESERT),
            Map.entry("grove_only", SNOWY), Map.entry("nether_only", NETHER),
            Map.entry("overworld_only", ALL & ~NETHER), Map.entry("plains_only", PLAINS),
            Map.entry("rainforest_desert_only", TROPICAL | DESERT),
            Map.entry("rainforest_grove_only", TROPICAL | SNOWY), Map.entry("rainforest_only", TROPICAL),
            Map.entry("rainforest_plains_only", TROPICAL | PLAINS),
            Map.entry("rainforest_savanna_grove_only", TROPICAL | SAVANNA | SNOWY),
            Map.entry("rainforest_savanna_plains_grove_only", TROPICAL | SAVANNA | PLAINS | SNOWY),
            Map.entry("rainforest_savanna_plains_only", TROPICAL | SAVANNA | PLAINS),
            Map.entry("savanna_only", SAVANNA), Map.entry("soul_sand_valley_only", NETHER),
            Map.entry("warped_crimson_only", NETHER), Map.entry("warped_only", NETHER));

    private BiomePlacementRestrictions() { }

    public static boolean isAllowed(LevelReader level, BlockPos pos, BlockState state) {
        if (!GameplayDimensions.isCustom(level) || state.isAir()) return true;
        int dimension = dimensionMask(level);
        if (dimension == NETHER && state.getFluidState().is(FluidTags.WATER)) return false;
        if ((dimension & (SNOWY | DESERT | NETHER)) == 0 && state.getFluidState().is(FluidTags.LAVA)) return false;
        if ((allowedMask(state.getBlock()) & dimension) == 0) return false;
        if (state.is(Blocks.NOTE_BLOCK)) return true;
        if (DocumentedPlacementRules.isPortal(state.getBlock())) return true;
        if (!DocumentedPlacementRules.isBiomeAllowed(level, pos, state, GrowthPlacementContext.isGrowing())) return false;
        Item item = materialItem(state.getBlock());
        return DimensionMaterialAvailability.isAvailable(level, item, dimension);
    }

    public static Optional<Component> placementFailureMessage(Level level, BlockPos pos, BlockState state) {
        if (isAllowed(level, pos, state)) return Optional.empty();
        return Optional.of(Component.literal(state.getBlock().getName().getString() + " is not available in "
                + level.dimension().location().getPath().replace('_', ' ') + " at this biome."));
    }

    public static int dimensionMask(LevelReader level) {
        var dimension = GameplayDimensions.dimension(level);
        if (GameplayDimensions.TROPICAL.equals(dimension)) return TROPICAL;
        if (GameplayDimensions.SAVANNA.equals(dimension)) return SAVANNA;
        if (GameplayDimensions.PLAINS.equals(dimension)) return PLAINS;
        if (GameplayDimensions.SNOWY.equals(dimension)) return SNOWY;
        if (GameplayDimensions.DESERT.equals(dimension)) return DESERT;
        if (GameplayDimensions.NETHER.equals(dimension)) return NETHER;
        return ALL;
    }

    public static int allowedMask(Block block) {
        return MASK_CACHE.computeIfAbsent(block, BiomePlacementRestrictions::computeAllowedMask);
    }

    public static void clearCache() { MASK_CACHE.clear(); }

    /** Technical plant states share the item/material origin of their placeable form. */
    public static Item materialItem(Block block) {
        if (block instanceof FlowerPotBlock) return Items.FLOWER_POT;
        String path = ForgeRegistries.BLOCKS.getKey(block).getPath();
        return switch (path) {
            case "cave_vines_plant" -> Items.GLOW_BERRIES;
            case "twisting_vines_plant" -> Items.TWISTING_VINES;
            case "weeping_vines_plant" -> Items.WEEPING_VINES;
            case "kelp_plant" -> Items.KELP;
            case "attached_melon_stem" -> Items.MELON_SEEDS;
            case "attached_pumpkin_stem" -> Items.PUMPKIN_SEEDS;
            default -> block.asItem();
        };
    }

    private static int computeAllowedMask(Block block) {
        if (block instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR) {
            return computeAllowedMask(pot.getContent()) & computeAllowedMask(Blocks.FLOWER_POT);
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) return ALL;
        if (id.getNamespace().equals("snowrealmagic")) return SNOWY;
        String p = id.getPath();
        BlockState state = block.defaultBlockState();
        if (DocumentedPlacementRules.isPortal(block)) return ALL;
        Integer documented = DocumentedPlacementRules.sourceMask(block);
        if (documented != null) return documented;
        Integer explicit = explicitMask(p);
        if (explicit != null) return explicit;
        int tree = treeMask(p);
        if (tree != ALL) return tree;
        int mask = 0;
        for (var group : GROUPS.entrySet()) {
            if (state.is(BlockTags.create(new ResourceLocation("aoemod", "biome_restrictions/" + group.getKey())))) mask |= group.getValue();
        }
        if (mask != 0) return mask;
        if (!state.is(BlockTags.create(new ResourceLocation("aoemod", "biome_restrictions/not_rainforest_exceptions")))) {
            int unrestricted = ALL;
            if (state.is(BlockTags.create(new ResourceLocation("aoemod", "biome_restrictions/not_rainforest")))) unrestricted &= ~TROPICAL;
            if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) unrestricted &= ~SAVANNA;
            return unrestricted;
        }
        return ALL;
    }

    /** Explicit requests take priority over old broad tag groups and recipe alternatives. */
    public static Integer explicitMask(String p) {
        if (p.equals("note_block")) return ALL;
        if (p.equals("water") || p.equals("water_cauldron")) return ALL & ~NETHER;
        if (p.equals("grass_block")) return SAVANNA | PLAINS | SNOWY;
        if (p.equals("mycelium")) return SNOWY;
        if (p.equals("grass") || p.equals("short_grass")) return TROPICAL | SAVANNA | PLAINS | SNOWY;
        if (p.equals("farmland")) return SAVANNA | PLAINS | SNOWY;
        if (p.equals("rich_soil_farmland")) return TROPICAL;
        if (p.equals("fire") || p.equals("soul_fire")) return ALL & ~TROPICAL;
        if (p.equals("lava") || p.equals("lava_cauldron")) return DESERT | NETHER;
        if (p.equals("beetroots")) return TROPICAL | SAVANNA | PLAINS | SNOWY;
        if (p.equals("wheat") || p.equals("carrots") || p.equals("potatoes") || p.equals("sunflower")) return TROPICAL | SAVANNA | PLAINS;
        if (p.equals("frogspawn") || p.equals("firefly_bush")) return TROPICAL;
        if (p.equals("red_shrub")) return NETHER;
        if (p.startsWith("twisting_vines") || p.startsWith("weeping_vines")) return NETHER;
        if (p.startsWith("cave_vines") || p.equals("attached_melon_stem")) return TROPICAL;
        if (p.equals("budding_tomatoes") || p.equals("rice_panicles")
                || p.equals("torchflower_crop") || p.equals("pitcher_crop")) return TROPICAL;
        if (p.equals("attached_pumpkin_stem")) return TROPICAL | SAVANNA | PLAINS;
        if (p.equals("kelp_plant")) return TROPICAL | SAVANNA | SNOWY;
        if (p.startsWith("pale_moss") || p.equals("soul_weeds")) return NETHER;
        if (p.equals("bee_nest") || p.equals("beehive")) return TROPICAL | SAVANNA | PLAINS;
        if (p.equals("bell")) return PLAINS | SNOWY | DESERT;
        if (p.equals("ancient_debris")) return DESERT | NETHER;
        if (p.contains("terracotta") || p.contains("cinnabar")) return DESERT;
        if (p.contains("sulfur")) return NETHER;
        if (p.contains("amethyst") || p.equals("snow") || p.equals("snow_block") || p.equals("powder_snow") || p.equals("powder_snow_cauldron")
                || p.startsWith("snow_brick") || p.startsWith("ice_brick")
                || p.equals("ice") || p.endsWith("_ice") || p.contains("prismarine") || p.equals("sea_lantern")
                || p.contains("dripstone") || p.equals("calcite")) return SNOWY;
        if (p.equals("polished_basalt")) return SNOWY | DESERT | NETHER;
        if (p.equals("clay")) return PLAINS;
        if (p.equals("mud") || p.equals("muddy_mangrove_roots")) return SAVANNA | SNOWY;
        if (p.equals("red_sand") || p.contains("red_sandstone")) return DESERT | NETHER;
        if (p.equals("sand") || p.equals("suspicious_sand") || p.equals("suspicious_gravel")) return PLAINS | SNOWY | DESERT;
        if (p.equals("coarse_dirt")) return PLAINS | SNOWY;
        if (p.equals("podzol")) return SNOWY;
        if (p.equals("stone") || p.equals("cobblestone") || p.equals("andesite") || p.equals("diorite") || p.equals("granite") || p.equals("gravel")) return PLAINS | SNOWY | DESERT;
        if (p.equals("deepslate") || p.equals("cobbled_deepslate") || p.equals("reinforced_deepslate") || p.equals("infested_deepslate") || p.equals("tuff")) return SNOWY | DESERT;
        if (p.startsWith("infested_")) return PLAINS | SNOWY | DESERT;
        if (p.equals("netherrack") || p.equals("blackstone") || p.equals("basalt") || p.equals("smooth_basalt")
                || p.equals("gilded_blackstone") || p.equals("crying_obsidian") || p.equals("obsidian") || p.equals("magma_block")
                || p.equals("soul_sand") || p.equals("soul_soil") || p.endsWith("nylium")) return NETHER;
        if (p.contains("quartz") && p.endsWith("_ore")) return NETHER;
        if (p.contains("quartz") || p.equals("glowstone")) return DESERT | NETHER;
        if (p.endsWith("_ore")) {
            if (p.contains("lapis") || p.contains("emerald") || p.contains("diamond")) return SNOWY;
            if (p.contains("redstone")) return DESERT;
            if (p.contains("iron")) return PLAINS | SNOWY;
            if (p.startsWith("nether_") || p.startsWith("basalt_") || p.startsWith("blackstone_") || p.startsWith("soul_")) return NETHER;
            if (p.contains("gold") || p.contains("copper") || p.contains("coal")) return PLAINS | SNOWY | DESERT;
        }
        if (p.equals("lapis_block") || p.equals("emerald_block") || p.equals("diamond_block")) return SNOWY;
        if (p.equals("redstone_block") || p.equals("redstone_wire")) return DESERT | NETHER;
        if (p.startsWith("end_") || p.equals("ender_chest") || p.startsWith("purpur") || p.startsWith("chorus")
                || p.endsWith("shulker_box") || p.equals("dragon_egg") || p.equals("dragon_head") || p.equals("dragon_wall_head")) return DESERT;
        if (p.equals("mushroom_stem") || p.equals("red_mushroom_block") || p.equals("brown_mushroom_block")) return TROPICAL | SNOWY;
        if (p.contains("mushgloom") || p.startsWith("glow_shroom") || p.equals("red_bush")) return SNOWY;
        if (p.equals("nether_wart_block") || p.equals("warped_wart_block") || p.equals("shroomlight")) return NETHER;
        return null;
    }

    public static int treeMask(String p) {
        if (containsFamily(p, "pale_oak")) {
            return p.endsWith("leaves") || p.endsWith("sapling") ? NETHER : DESERT | NETHER;
        }
        if (containsFamily(p, "crimson") || containsFamily(p, "warped")) return NETHER;
        if (containsFamily(p, "spruce") || containsFamily(p, "poplar") || p.endsWith("_poplar_leaves")) return SNOWY;
        if (containsFamily(p, "acacia")) return SAVANNA;
        if (containsFamily(p, "birch")) return PLAINS;
        if (containsFamily(p, "dark_oak") || containsFamily(p, "jungle") || containsFamily(p, "mangrove")
                || containsFamily(p, "cherry") || containsFamily(p, "azalea") || containsFamily(p, "bamboo")) return TROPICAL;
        if (containsFamily(p, "oak")) return TROPICAL | PLAINS;
        return ALL;
    }

    private static boolean containsFamily(String p, String family) {
        // Compatibility blocks add prefixes such as hollow_, vertical_, or flowering_.
        // Match complete underscore-delimited components, including compound wood names.
        return ("_" + p + "_").contains("_" + family + "_");
    }

    private static boolean biomeHereOrAbove(LevelReader level, BlockPos pos, String biome) {
        return GameplayDimensions.biomeIs(level, pos, biome) || GameplayDimensions.biomeIs(level, pos.above(), biome);
    }

    private static boolean isLivingNetherPlant(String p) {
        return p.equals("crimson_fungus") || p.equals("crimson_roots") || p.startsWith("weeping_vines")
                || p.equals("warped_fungus") || p.equals("warped_roots") || p.startsWith("twisting_vines")
                || p.equals("nether_sprouts") || p.equals("nether_wart") || p.equals("soul_berry_bush") || p.equals("soul_beetroots")
                || p.equals("pale_oak_sapling") || p.contains("eyeblossom") || p.equals("pale_hanging_moss")
                || p.equals("soul_weeds") || p.equals("red_shrub");
    }
}
