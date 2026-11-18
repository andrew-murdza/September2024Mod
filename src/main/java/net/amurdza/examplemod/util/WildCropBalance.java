package net.amurdza.examplemod.util;

import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/** Wild sources favor exploration; cultivation remains the efficient use of bone meal. */
public final class WildCropBalance {
    public static final int SEED_YIELD = 9;
    // At most .09 seeds/use. Even Plains carrots yield 2.5 net carrots in at most
    // four bone meal uses (.625/use), so this is at least 6.9 times less efficient.
    // One roll per consumed item deliberately bypasses regional repeat multipliers.
    public static final float SPREAD_CHANCE = 0.01F;
    public static final Set<String> CROPS = Set.of("farmersdelight:wild_carrots", "farmersdelight:wild_cabbages",
            "farmersdelight:wild_tomatoes", "farmersdelight:wild_rice", "legumedelight:wild_beans",
            "hearthandharvest:wild_cotton", "pineapple_delight:pineapple_wild_crop");

    private WildCropBalance() { }

    public static boolean isWildCrop(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && CROPS.contains(id.toString());
    }

    public static boolean spread(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockState source = level.getBlockState(origin);
        if (!isWildCrop(source) || random.nextFloat() >= SPREAD_CHANCE) return false;
        if (source.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && source.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) origin = origin.below();
        String species = ForgeRegistries.BLOCKS.getKey(source.getBlock()).toString();
        int[] offsets = new int[147];
        for (int i = 0; i < offsets.length; i++) offsets[i] = i;
        for (int i = offsets.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), previous = offsets[i];
            offsets[i] = offsets[j]; offsets[j] = previous;
        }
        for (int offset : offsets) {
            BlockPos target = origin.offset(offset % 7 - 3, offset / 49 - 1, offset / 7 % 7 - 3);
            BlockState previous = level.getBlockState(target);
            if (!previous.isAir() && !previous.is(Blocks.WATER)) continue;
            if (DocumentedFloraFeature.placePlant(level, target, species, random)) return true;
        }
        return false;
    }
}
