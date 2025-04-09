package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class WorldGenUtils {
    public static int getSeaLevelWorldGen(BlockPos pos, LevelAccessor level){
        return getSeaLevelWorldGen(level.getBiome(pos));
    }
    public static int getSeaLevelWorldGen(Holder<Biome> biome){
        return biome.is(ModTags.Biomes.netherBiomes)?-64:biome.is(ModTags.Biomes.desertBiomes)?170
                :biome.is(ModTags.Biomes.mountainBiomes)?120:63;
    }
    public static int getTotalWaterAbove(BlockPos pos, BlockGetter level){
        return getTotalWaterAbove(pos,level,-pos.getY()+level.getMaxBuildHeight()-1);
    }
    private static boolean isWater(BlockPos pos, BlockGetter level){
        BlockState state=level.getBlockState(pos);
        return state.getBlock()==Blocks.WATER&&state.getValue(BlockStateProperties.LEVEL)==8;

    }
    public static int getTotalWaterAbove(BlockPos pos, BlockGetter level, int max){
        int sum=0;
        for(int i=0;i<max;i++){
            if(isWater(pos.above(i),level)){
                sum++;
            }
            else{
                break;
            }
        }
        return sum;
    }
    public static int getTotalWaterInColumn(BlockPos pos, int radius, BlockGetter level){
        return getTotalWaterInColumn(pos,radius,level,-pos.getY()+level.getMinBuildHeight()+1,
                -pos.getY()+level.getMaxBuildHeight()-1);
    }
    public static int getTotalWaterInColumn(BlockPos pos, int radius, BlockGetter level, int min, int max){
        int sum=0;
        outer:for(int k=min;k<0;k++){
            for(int i=-radius;i<=radius;i++){
                for(int j=-radius;j<radius;j++){
                    if(!isWater(pos.offset(i,k,j),level)){
                        break outer;
                    }
                    sum++;
                }
            }
        }
        outer1:for(int k=0;k<=max;k++){
            for(int i=-radius;i<=radius;i++){
                for(int j=-radius;j<radius;j++){
                    if(!isWater(pos.offset(i,k,j),level)){
                        break outer1;
                    }
                    sum++;
                }
            }
        }
        return sum;
    }
//    public static int getSeaLevelAtPos(int x, int y, int z){
//        return getSeaLevelAtChunk(getChunk(x), getChunk(y), getChunk(z));
//    }
//    public static void tryCoralWall(int chunkX, int chunkY, int chunkZ, Level level){
//        int seaLevel=getSeaLevelAtChunk(chunkX, chunkY, chunkZ);
//        if(shouldCreateCoralWall(chunkX,chunkZ)&&chunkY*16<=seaLevel-4&&chunkY*16>=seaLevel-23){
//
//        }
//    }
//    public static int getSeaLevelAtChunk(int chunkX, int chunkY, int chunkZ){
//        return 63;
//    }
//    public static int getHeight(int x, int z){
//        int chunkX = SectionPos.blockToSectionCoord(x);
//        int chunkZ = SectionPos.blockToSectionCoord(z);
//        return getHeightFunction(chunkX,chunkZ).apply(x%16,z%16);
//    }
//    public static Biome getBiomeAtChunk(int chunkX, int chunkY, int chunkZ){
//        return ModBiomes.RAINFOREST;
//    }
//    public static Biome getBiomeAtPos(int x, int y, int z){
//        return getBiomeAtChunk(getChunk(x), getChunk(y), getChunk(z));
//    }
//    public static Biome getBiomeAtPos(BlockPos pos){
//        return getBiomeAtPos(pos.getX(),pos.getY(),pos.getZ());
//    }
//    public static void placeStructure(int x, int y, int z, ServerLevel level, Structure structure){
//
//    }
//    public static void tryColumn(int chunkX, int chunkY, int chunkZ, ServerLevel level){
//        ResourceKey<Structure> columnKey=genColumnStructure(chunkX, chunkY, chunkZ);
//
//        if(columnKey!=null){
//            Structure column=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(columnKey);
//            Vec3i offset=genColumnStructureOffset(chunkX, chunkY, chunkZ);
//            placeStructure(chunkX*16+offset.getX(),chunkY+offset.getY(),chunkZ+offset.getZ(),level,column);
//        }
//    }
//    public static ResourceKey<Structure> genColumnStructure(int chunkX, int chunkY, int chunkZ){
//        return null;//BuiltinStructures.PILLAGER_OUTPOST;
//    }
//    public static Vec3i genColumnStructureOffset(int chunkX, int chunkY, int chunkZ){
//        return Vec3i.ZERO;
//    }
//    public static ResourceKey<Biome> getBiomeKeyAtChunk(int chunkX, int chunkY, int chunkZ){
//        return ModBiomes.RAINFOREST_KEY;
//    }
//    public static ResourceKey<Biome> getBiomeKeyAtPos(int x, int y, int z){
//        return getBiomeKeyAtChunk(getChunk(x), getChunk(y), getChunk(z));
//    }
//    public static ResourceKey<Biome> getBiomeKeyAtPos(BlockPos pos){
//        return getBiomeKeyAtChunk(pos.getX(),pos.getY(),pos.getZ());
//    }
//    private static BiFunction<Integer,Integer,Integer> getHeightFunction(int chunkX, int chunkZ){
//        return (x,z)->63;
//    }
//    private static int getChunk(int x){
//        return SectionPos.blockToSectionCoord(x);
//    }
//    private static boolean shouldCreateCoralWall(int chunkX, int chunkZ){
//        return false;
//    }
}
