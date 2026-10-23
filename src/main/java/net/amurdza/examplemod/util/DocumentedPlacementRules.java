package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import static net.amurdza.examplemod.util.BiomePlacementRestrictions.*;

/** Placement origins from the six dimension documents; these override older annotation groups. */
public final class DocumentedPlacementRules {
    private static final String[] DIMENSIONS = {"tropical", "savanna", "plains", "snowy", "desert", "nether"};

    private DocumentedPlacementRules() { }

    public static Integer sourceMask(Block block) {
        int mask = 0;
        for (int i = 0; i < DIMENSIONS.length; i++) {
            if (block.defaultBlockState().is(BlockTags.create(new ResourceLocation("aoemod",
                    "dimension_sources/" + DIMENSIONS[i])))) mask |= 1 << i;
        }
        return mask == 0 ? null : mask;
    }

    public static boolean isPortal(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && id.getNamespace().equals("aoemod") && switch (id.getPath()) {
            case "overworld_portal", "tropical_portal", "savanna_portal", "plains_portal",
                    "snowy_portal", "desert_portal", "nether_portal" -> true;
            default -> false;
        };
    }

    /** Pure habitat check, also used to reject a whole tree before any of it is placed. */
    public static boolean isBiomeAllowed(LevelReader level, BlockPos pos, BlockState state, boolean fromGrowth) {
        if (!GameplayDimensions.isCustom(level)) return true;
        if (fromGrowth && GrowthPlacementContext.allowsCanopyExtension(level, pos, state)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return true;
        String p = id.getPath();
        if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
            if (p.equals("beetroots") || p.equals("wild_beetroots") || p.equals("mycelium") || p.equals("glow_lichen")
                    || p.equals("red_mushroom") || p.equals("brown_mushroom")
                    || p.equals("red_mushroom_block") || p.equals("brown_mushroom_block")
                    || p.equals("mushroom_stem") || p.contains("mushgloom") || p.startsWith("glow_shroom")) {
                return GameplayDimensions.biomeIs(level, p.equals("mycelium") ? pos.above() : pos, "mushroom_caves");
            }
            if (p.contains("spruce") && (p.endsWith("sapling") || p.endsWith("leaves"))
                    || p.contains("poplar") && (p.endsWith("sapling") || p.endsWith("leaves"))) {
                return GameplayDimensions.biomeIs(level, pos, "snowy_forest");
            }
        }
        if (GameplayDimensions.is(level, GameplayDimensions.DESERT) && p.equals("chorus_flower")) {
            return fromGrowth || GameplayDimensions.biomeIs(level, pos, "deep_dark");
        }
        if (!GameplayDimensions.is(level, GameplayDimensions.NETHER)) return true;
        if (p.equals("netherrack") || p.equals("nether_gold_ore") || p.equals("nether_quartz_ore")) {
            return biome(level, pos.above(), "crimson_forest", "warped_forest");
        }
        if (p.equals("crimson_nylium")) return biome(level, pos.above(), "crimson_forest");
        if (p.equals("warped_nylium")) return biome(level, pos.above(), "warped_forest");
        if (p.equals("soul_sand") || p.equals("soul_soil")
                || p.startsWith("soul_sand_") && p.endsWith("_ore")
                || p.startsWith("soul_soil_") && p.endsWith("_ore")) {
            return biome(level, pos.above(), "soul_sand_valley");
        }
        if ((p.startsWith("basalt_") || p.startsWith("blackstone_")) && p.endsWith("_ore")) {
            return biome(level, pos.above(), "basalt_deltas");
        }
        if (p.equals("crimson_fungus") || p.equals("crimson_roots") || p.startsWith("weeping_vines")
                || p.equals("red_shrub") || p.equals("nether_wart_block")
                || p.contains("crimson") && (p.endsWith("stem") || p.endsWith("hyphae"))) {
            return biome(level, pos, "crimson_forest");
        }
        if (p.equals("warped_fungus") || p.equals("warped_roots") || p.startsWith("twisting_vines")
                || p.equals("nether_sprouts") || p.equals("warped_wart_block")
                || p.contains("warped") && (p.endsWith("stem") || p.endsWith("hyphae"))) {
            return biome(level, pos, "warped_forest");
        }
        if (fromGrowth && p.contains("pale_oak")
                && (p.endsWith("log") || p.endsWith("wood") || p.endsWith("leaves"))) {
            return biome(level, pos, "soul_sand_valley");
        }
        if (p.equals("soul_berry_bush") || p.equals("soul_beetroots") || p.equals("nether_wart")
                || p.equals("pale_oak_sapling") || p.equals("pale_oak_leaves") || p.equals("pale_hanging_moss")
                || p.contains("eyeblossom") || p.equals("soul_weeds")) {
            return biome(level, pos, "soul_sand_valley");
        }
        if (id.getNamespace().equals("mynethersdelight") && p.contains("powder")) {
            return biome(level, pos, "basalt_deltas");
        }
        return true;
    }

    private static boolean biome(LevelReader level, BlockPos pos, String... names) {
        for (String name : names) if (GameplayDimensions.biomeIs(level, pos, name)) return true;
        return false;
    }
}
