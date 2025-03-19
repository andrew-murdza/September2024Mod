package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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
    public static void placeStructure(int x, int y, int z, ServerLevel level, Structure structure){

    }
    public static void tryColumn(int chunkX, int chunkY, int chunkZ, ServerLevel level){
        ResourceKey<Structure> columnKey=genColumnStructure(chunkX, chunkY, chunkZ);

        if(columnKey!=null){
            Structure column=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(columnKey);
            Vec3i offset=genColumnStructureOffset(chunkX, chunkY, chunkZ);
            placeStructure(chunkX*16+offset.getX(),chunkY+offset.getY(),chunkZ+offset.getZ(),level,column);
        }
    }
    public static ResourceKey<Structure> genColumnStructure(int chunkX, int chunkY, int chunkZ){
        return null;//BuiltinStructures.PILLAGER_OUTPOST;
    }
    public static Vec3i genColumnStructureOffset(int chunkX, int chunkY, int chunkZ){
        return Vec3i.ZERO;
    }
    public static ResourceKey<Biome> getBiomeAtChunk(int chunkX, int chunkY, int chunkZ){
        return ModBiomes.RAINFOREST;
    }
    public static ResourceKey<Biome> getBiomeAtPos(int x, int y, int z){
        return getBiomeAtChunk(getChunk(x), getChunk(y), getChunk(z));
    }
    public static ResourceKey<Biome> getBiomeAtPos(BlockPos pos){
        return getBiomeAtChunk(pos.getX(),pos.getY(),pos.getZ());
    }
    public static int getSeaLevelAtChunk(int chunkX, int chunkY, int chunkZ){
        return 63;
    }
    public static int getSeaLevelAtPos(int x, int y, int z){
        return getSeaLevelAtChunk(getChunk(x), getChunk(y), getChunk(z));
    }
    public static int getSeaLevelAtPos(BlockPos pos){
        return getSeaLevelAtChunk(pos.getX(),pos.getY(),pos.getZ());
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
