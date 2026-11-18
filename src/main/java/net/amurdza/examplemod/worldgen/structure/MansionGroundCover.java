package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.PlainsFlowers;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** Restore the normal short-grass lottery when /place grades already decorated chunks. */
public final class MansionGroundCover {
    private MansionGroundCover() { }

    public static void place(WorldGenLevel level, BoundingBox clip, MansionRampTerrain.Site site) {
        var forest = new MansionForestFloor.Forest(site.house(), site.groundY() + 1, site.layoutVersion());
        for (int x = clip.minX(); x <= clip.maxX(); x++) for (int z = clip.minZ(); z <= clip.maxZ(); z++) {
            Integer rootY = forest.rootY(level, x, z);
            if (rootY == null || rootY < clip.minY() || rootY >= clip.maxY()) continue;
            var root = new BlockPos(x, rootY, z);
            var previous = level.getBlockState(root);
            var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(previous.getBlock());
            if (forest.contains(x, z) && (previous.is(Blocks.PUMPKIN) || id != null
                    && (id.getPath().startsWith("wild_") || id.getPath().endsWith("_wild_crop")))) {
                if (previous.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF))
                    level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(root, Blocks.AIR.defaultBlockState(), 18);
            }
            if (!level.getBlockState(root.below()).is(Blocks.GRASS_BLOCK) || !level.isEmptyBlock(root)) continue;
            long seed = TropicalLayout.mix(level.getSeed() ^ net.minecraft.util.Mth.getSeed(x, rootY, z));
            // Existing configured grass: 30% choice * 74/80 short-grass share.
            if (Math.floorMod(seed, 800) < 222)
                DocumentedFloraFeature.placePlant(level, root, "minecraft:grass", RandomSource.create(seed));
        }
        PlainsFlowers.placeChunk(level, clip.minX() >> 4, clip.minZ() >> 4, List.of(forest));
    }
}
