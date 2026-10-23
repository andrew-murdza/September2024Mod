package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Native planting origins remain strict; only the generated upper canopy may cross a layer boundary. */
public final class TreeGrowthPlacementRules {
    public static final int CANOPY_EXTENSION = 16;

    private TreeGrowthPlacementRules() { }

    public static boolean isAllowed(LevelReader level, BlockPos origin, BlockPos pos, BlockState state) {
        String habitat = habitat(level, state);
        if (habitat == null) return true;
        if (level.isOutsideBuildHeight(origin) || level.isOutsideBuildHeight(pos)) return false;
        if (!GameplayDimensions.biomeIs(level, origin, habitat)) return false;
        return GameplayDimensions.biomeIs(level, pos, habitat) || allowsExtension(level, origin, pos, state);
    }

    public static boolean allowsExtension(LevelReader level, BlockPos origin, BlockPos pos, BlockState state) {
        String habitat = habitat(level, state);
        if (habitat == null || pos.getY() <= origin.getY() || level.isOutsideBuildHeight(pos)
                || !GameplayDimensions.biomeIs(level, origin, habitat)
                || GameplayDimensions.biomeIs(level, pos, habitat)) return false;
        // Project the canopy column back to the planting height. A neighboring horizontal biome
        // cannot borrow the tree's vertical allowance, even if another native patch lies below it.
        if (!GameplayDimensions.biomeIs(level, new BlockPos(pos.getX(), origin.getY(), pos.getZ()), habitat)) return false;
        var upperBiome = level.getBiome(pos);
        for (int below = 1; below <= CANOPY_EXTENSION && pos.getY() - below >= origin.getY(); below++) {
            BlockPos lower = pos.below(below);
            if (GameplayDimensions.biomeIs(level, lower, habitat)) return true;
            if (!level.getBiome(lower).equals(upperBiome)) return false;
        }
        return false;
    }

    private static String habitat(LevelReader level, BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return null;
        String path = id.getPath();
        if (GameplayDimensions.is(level, GameplayDimensions.NETHER)) {
            if (path.equals("nether_wart_block") || path.startsWith("weeping_vines")
                    || path.contains("crimson") && (path.endsWith("stem") || path.endsWith("hyphae"))) return "crimson_forest";
            if (path.equals("warped_wart_block")
                    || path.contains("warped") && (path.endsWith("stem") || path.endsWith("hyphae"))) return "warped_forest";
            if (path.equals("pale_hanging_moss") || path.contains("pale_oak")
                    && (path.endsWith("log") || path.endsWith("wood") || path.endsWith("leaves"))) return "soul_sand_valley";
        }
        if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
            if (path.equals("mushroom_stem") || path.equals("red_mushroom_block")
                    || path.equals("brown_mushroom_block") || path.contains("mushgloom")
                    || path.startsWith("glow_shroom")) return "mushroom_caves";
            if ((path.contains("spruce") || path.contains("poplar"))
                    && (path.endsWith("log") || path.endsWith("wood") || path.endsWith("leaves"))) return "snowy_forest";
        }
        return null;
    }
}
