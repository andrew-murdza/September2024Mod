package net.amurdza.examplemod.util;

import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Helper {

    private static final Map<DyeColor, ItemLike> ITEM_BY_DYE = Util.make(Maps.newEnumMap(DyeColor.class), (map) -> {
        map.put(DyeColor.WHITE, Blocks.WHITE_WOOL);
        map.put(DyeColor.ORANGE, Blocks.ORANGE_WOOL);
        map.put(DyeColor.MAGENTA, Blocks.MAGENTA_WOOL);
        map.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
        map.put(DyeColor.YELLOW, Blocks.YELLOW_WOOL);
        map.put(DyeColor.LIME, Blocks.LIME_WOOL);
        map.put(DyeColor.PINK, Blocks.PINK_WOOL);
        map.put(DyeColor.GRAY, Blocks.GRAY_WOOL);
        map.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
        map.put(DyeColor.CYAN, Blocks.CYAN_WOOL);
        map.put(DyeColor.PURPLE, Blocks.PURPLE_WOOL);
        map.put(DyeColor.BLUE, Blocks.BLUE_WOOL);
        map.put(DyeColor.BROWN, Blocks.BROWN_WOOL);
        map.put(DyeColor.GREEN, Blocks.GREEN_WOOL);
        map.put(DyeColor.RED, Blocks.RED_WOOL);
        map.put(DyeColor.BLACK, Blocks.BLACK_WOOL);
    });

    public static Item getWool(Sheep sheep){
        return ITEM_BY_DYE.get(sheep.getColor()).asItem();
    }

    public static int computeIncrements(RandomSource rand, float mult) {
        if(mult <= 0.0f){
            return 0;
        }
        if (mult < 1.0f) {
            return (rand.nextFloat() < mult) ? 1 : 0;
        }
        int base = 1 + Mth.floor(mult - 1.0f);
        float frac = (mult - 1.0f) - Mth.floor(mult - 1.0f);
        if (frac > 0.0f && rand.nextFloat() < frac) base += 1;
        return base;
    }

    public static boolean isBlock(Block block, Block... blocks){
        return Arrays.asList(blocks).contains(block);
    }

    public static boolean isBlock(BlockState block, Block... blocks){
        return Arrays.asList(blocks).contains(block.getBlock());
    }

    public static boolean isBlockTag(BlockState state, TagKey<Block>... tags){
        for(TagKey<Block> tag: tags){
            if(state.is(tag)){
                return true;
            }
        }
        return false;
    }

    public static boolean withChance(LevelAccessor level, double f){
        return level.getRandom().nextFloat()<f;
    }

    public static <T> T select(LevelAccessor level, List<T> objects){
        return objects.get(level.getRandom().nextInt(objects.size()));
    }

    public static <T> T select(LevelAccessor level, T... objects){
        return select(level,List.of(objects));
    }

    public static List<Direction> HORIZONTAL_DIRECTIONS = List.of(Direction.EAST,Direction.SOUTH,Direction.NORTH,Direction.WEST);

    public static int nextIntCropsGrow(Level level, BlockPos pos, BlockState state, RandomSource random, int n){
        return ForgeHooks.onCropsGrowPre(level,pos,state,random.nextInt(n)==0)?0:1;
    }

    public static int nextFloatCropsGrow(Level level, BlockPos pos, BlockState state, RandomSource random, double d){
        return ForgeHooks.onCropsGrowPre(level,pos,state,random.nextFloat()<d)?0:1;
    }

    public static boolean isSpecialBiome(LevelReader level, BlockPos pos){
        return level.getBiome(pos).is(ModTags.Biomes.tropicalBiomes);
    }


    public static <T> T getBiomeValue(LevelReader level, BlockPos pos, Map<TagKey<Biome>,T> map) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        for (var entry : map.entrySet()) {
            TagKey<Biome> tag = entry.getKey();
            if (biomeHolder.is(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
