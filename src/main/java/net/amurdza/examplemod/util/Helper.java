package net.amurdza.examplemod.util;

import com.google.common.collect.Maps;
import net.amurdza.examplemod.Config;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
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

import javax.annotation.Nullable;
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

    public static boolean isBlackListed(Block block){
        return Config.BLACKLISTED_USE_ITEMS.contains(block.asItem());
    }
    public static boolean isBlackListed(BlockState state){
        return isBlackListed(state.getBlock());
    }
    public static boolean isBlackListed(Mob mob){
        return mob.getEncodeId()!=null&&Config.BLACKLISTED_MOBS.contains(mob.getEncodeId());
    }
    public static boolean isOkMob(Level level, BlockPos pos, Mob mob){
        return !Helper.isSpecialBiome(level, pos) || !isBlackListed(mob);
    }
    public static boolean isOkToPlace(Level level, BlockPos pos, BlockState state){
        return !Helper.isSpecialBiome(level,pos)||!Helper.isBlackListed(state);
    }
    public static boolean isOkToPlace(Level level, BlockPos pos, Block block){
        return !Helper.isSpecialBiome(level,pos)||!Helper.isBlackListed(block);
    }
    public static int getDiffLevel(Holder<Biome> biome){
        if(biome.unwrapKey().isPresent()){
            ResourceKey<Biome> key=biome.unwrapKey().get();
            if(Config.DIFF_MAP.containsKey(key)){
                return Config.DIFF_MAP.get(key);
            }
        }
        return 1;
    }
    public static Direction reverseDirection(Direction direction){
        if(direction==Direction.UP){
            return Direction.DOWN;
        }
        if(direction==Direction.DOWN){
            return Direction.UP;
        }
        if(direction==Direction.NORTH){
            return Direction.SOUTH;
        }
        if(direction==Direction.SOUTH){
            return Direction.NORTH;
        }
        if(direction==Direction.EAST){
            return Direction.WEST;
        }
        if(direction==Direction.WEST){
            return Direction.EAST;
        }
        return Direction.EAST;
    }
    public static int getDiffLevel(Level level, BlockPos pos){
        return getDiffLevel(level.getBiome(pos));
    }
    public static int getDiffLevel(Entity entity){
        return getDiffLevel(entity.level().getBiome(entity.getOnPos()));
    }

    public static boolean isFromPlayer(@Nullable Entity entity){
        return entity instanceof Player||entity instanceof TamableAnimal animal && animal.getOwner() instanceof Player;
    }
    public static boolean isFromAnimal(@Nullable Entity entity){
        return entity instanceof Animal;
    }
    public static boolean isFromAnimal(DamageSource source){
        return isFromAnimal(source.getEntity());
    }

    public static boolean isFromPlayer(DamageSource source){
        return isFromPlayer(source.getEntity());
    }

    public static int getLevel(Entity entity){
        Player player=null;
        if(entity instanceof Player player1){
            player=player1;
        }
        else if(entity instanceof TamableAnimal animal&&animal.getOwner() instanceof Player player1){
            player=player1;
        }
        return player!=null?player.experienceLevel:Helper.getDiffLevel(entity);
    }
    public static int getLevel(DamageSource source, Entity entityDefending){
        return source.getEntity()!=null?getLevel(source.getEntity()):getLevel(entityDefending);
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

    public static void sendMessage(Player player,String message){
        player.displayClientMessage(MutableComponent.create(new LiteralContents(message)),true);
    }

    public static int nextIntCropsGrow(Level level, BlockPos pos, BlockState state, RandomSource random, int n){
        return ForgeHooks.onCropsGrowPre(level,pos,state,random.nextInt(n)==0)?0:1;
    }

    public static int nextFloatCropsGrow(Level level, BlockPos pos, BlockState state, RandomSource random, double d){
        return ForgeHooks.onCropsGrowPre(level,pos,state,random.nextFloat()<d)?0:1;
    }

    public static boolean isSpecialBiome(LevelReader level, BlockPos pos){
        return level.getBiome(pos).is(ModTags.Biomes.tropicalBiomes);//isBiomeNameAtPos(level,pos,Config.SPECIAL_BIOME);
    }
    public static boolean isSpecialBiome(Entity entity){
        return entity!=null&&isSpecialBiome(entity.level(),entity.getOnPos());
    }
}
