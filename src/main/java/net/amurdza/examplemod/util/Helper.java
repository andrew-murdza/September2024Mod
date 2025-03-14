package net.amurdza.examplemod.util;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class Helper {

    public static int getLevel(Item item){
        return 2;
    }

    public static boolean isBoss(Entity entity){
        return false;
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
    public static <T> T select(RandomSource source, T... elements){
        return elements[source.nextInt(elements.length)];
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
    public static int withChanceToInt(LevelAccessor level, double f){
        return withChance(level,f)?0:1;
    }
    public static boolean withChance(LevelAccessor level, double f){
        return level.getRandom().nextFloat()<f;
    }
    public static ResourceKey<Biome> getBiomeName(Holder<Biome> biome) {
        return biome.unwrapKey().get();
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
