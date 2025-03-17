package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.worldgen.biome.ModBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.function.BiFunction;

public class WorldGenUtils {
    public static void tryCoralWall(int chunkX, int chunkY, int chunkZ, Level level){
        int seaLevel=getSeaLevelAtChunk(chunkX, chunkY, chunkZ);
        if(shouldCreateCoralWall(chunkX,chunkZ)&&chunkY*16<=seaLevel-4&&chunkY*16>=seaLevel-23){

        }
    }
    public static ResourceKey<Structure> genColumnStructure(int chunkX, int chunkY, int chunkZ){
        return null;//BuiltinStructures.PILLAGER_OUTPOST;
    }
    public static Vec3i genColumnStructureOffset(int chunkX, int chunkY, int chunkZ){
        return Vec3i.ZERO;
    }
    public static Holder<Biome> getBiomeAtChunk(int chunkX, int chunkY, int chunkZ){
        return Holder.direct(ModBiomes.RAINFOREST_OLD);
    }
    public static Holder<Biome> getBiomeAtPos(int x, int y, int z){
        return getBiomeAtChunk(getChunk(x), getChunk(y), getChunk(z));
    }
    public static int getSeaLevelAtChunk(int chunkX, int chunkY, int chunkZ){
        return 63;
    }
    public static int getSeaLevelAtPos(int x, int y, int z){
        return getSeaLevelAtChunk(getChunk(x), getChunk(y), getChunk(z));
    }
    public static int getHeight(int x, int z){
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        return getHeightFunction(chunkX,chunkZ).apply(x%16,z%16);
    }
    private static BiFunction<Integer,Integer,Integer> getHeightFunction(int chunkX, int chunkZ){
        return (x,z)->63;
    }
    private static int getChunk(int x){
        return SectionPos.blockToSectionCoord(x);
    }
    private static boolean shouldCreateCoralWall(int chunkX, int chunkZ){
        return false;
    }
}
