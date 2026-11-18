package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

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
        Habitat habitat = habitat(level, state, fromGrowth);
        if (habitat == null) return true;
        BlockPos target = habitat.above ? pos.above() : pos;
        for (String biome : habitat.biomes) if (GameplayDimensions.biomeIs(level, target, biome)) return true;
        return false;
    }

    /** Uses the same habitat definition as placement so feedback names the actual restriction. */
    public static Optional<String> requiredBiomes(LevelReader level, BlockState state, boolean fromGrowth) {
        Habitat habitat = habitat(level, state, fromGrowth);
        return habitat == null ? Optional.empty() : Optional.of(java.util.Arrays.stream(habitat.biomes)
                .map(biome -> biome.equals("mountain_lake") ? "cold_ocean" : biome)
                .distinct().collect(java.util.stream.Collectors.joining(" or "))
                .replace("snowy_forest", "snowy_taiga").replace('_', ' '));
    }

    private static Habitat habitat(LevelReader level, BlockState state, boolean fromGrowth) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return null;
        String p = id.getPath();
        if (p.equals("red_shrub")) return new Habitat(false, "dappled_forest");
        if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
            if (p.equals("wheat") || p.equals("oat_crop") || p.equals("pumpkin") || p.equals("carved_pumpkin")
                    || p.equals("pumpkin_stem") || p.equals("attached_pumpkin_stem")) {
                return new Habitat(false, "dappled_forest");
            }
            if (p.equals("sugar_cane")) return new Habitat(false, "dappled_forest", "cold_ocean", "mountain_lake");
            if (p.equals("beetroots") || p.equals("wild_beetroots") || p.equals("mycelium") || p.equals("glow_lichen")
                    || p.equals("red_mushroom") || p.equals("brown_mushroom")
                    || p.equals("red_mushroom_block") || p.equals("brown_mushroom_block")
                    || p.equals("mushroom_stem") || p.contains("mushgloom") || p.startsWith("glow_shroom")) {
                return new Habitat(p.equals("mycelium"), "mushroom_caves");
            }
            if (p.contains("poplar") && (p.endsWith("sapling") || p.endsWith("leaves"))) {
                return new Habitat(false, "dappled_forest");
            }
            if (p.contains("spruce") && (p.endsWith("sapling") || p.endsWith("leaves"))) {
                return new Habitat(false, "snowy_forest", "dappled_forest", "cold_ocean", "mountain_lake");
            }
            if (p.equals("sweet_berry_bush") || p.equals("blue_berry_bush")) {
                return new Habitat(false, "snowy_forest");
            }
        }
        if (GameplayDimensions.is(level, GameplayDimensions.DESERT) && p.equals("chorus_flower")) {
            return fromGrowth ? null : new Habitat(false, "deep_dark");
        }
        if (!GameplayDimensions.is(level, GameplayDimensions.NETHER)) return null;
        if (p.equals("netherrack") || p.equals("nether_gold_ore") || p.equals("nether_quartz_ore")) {
            return new Habitat(true, "crimson_forest", "warped_forest");
        }
        if (p.equals("crimson_nylium")) return new Habitat(true, "crimson_forest");
        if (p.equals("warped_nylium")) return new Habitat(true, "warped_forest");
        if (p.equals("soul_sand") || p.equals("soul_soil")
                || p.startsWith("soul_sand_") && p.endsWith("_ore")
                || p.startsWith("soul_soil_") && p.endsWith("_ore")) {
            return new Habitat(true, "soul_sand_valley");
        }
        if ((p.startsWith("basalt_") || p.startsWith("blackstone_")) && p.endsWith("_ore")) {
            return new Habitat(true, "basalt_deltas");
        }
        if (p.equals("crimson_fungus") || p.equals("crimson_roots") || p.startsWith("weeping_vines")
                || p.equals("nether_wart_block")
                || p.contains("crimson") && (p.endsWith("stem") || p.endsWith("hyphae"))) {
            return new Habitat(false, "crimson_forest");
        }
        if (p.equals("warped_fungus") || p.equals("warped_roots") || p.startsWith("twisting_vines")
                || p.equals("nether_sprouts") || p.equals("warped_wart_block")
                || p.contains("warped") && (p.endsWith("stem") || p.endsWith("hyphae"))) {
            return new Habitat(false, "warped_forest");
        }
        if (fromGrowth && p.contains("pale_oak")
                && (p.endsWith("log") || p.endsWith("wood") || p.endsWith("leaves"))) {
            return new Habitat(false, "soul_sand_valley");
        }
        if (p.equals("soul_beetroots") || p.equals("nether_wart")
                || p.equals("pale_oak_sapling") || p.equals("pale_oak_leaves") || p.equals("pale_hanging_moss")
                || p.contains("eyeblossom") || p.equals("soul_weeds") || p.equals("ghoulflower")) {
            return new Habitat(false, "soul_sand_valley");
        }
        if (id.getNamespace().equals("mynethersdelight") && p.contains("powder")) {
            return new Habitat(false, "basalt_deltas");
        }
        return null;
    }

    private record Habitat(boolean above, String... biomes) { }
}
