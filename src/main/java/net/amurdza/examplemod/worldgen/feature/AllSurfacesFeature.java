package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class AllSurfacesFeature extends Feature<AllSurfacesFeatureConfig> {
    public AllSurfacesFeature(Codec<AllSurfacesFeatureConfig> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AllSurfacesFeatureConfig> context) {
        PlacedFeature feature = context.config().feature.value();
        boolean water = context.config().water;
        BlockPredicate predicate = context.config().predicate;
        boolean allLayers=context.config().allLayers;
        WorldGenLevel level=context.level();
        ChunkAccess chunk=level.getChunk(context.origin());
        ChunkPos chunkPos=chunk.getPos();
        int minX=chunkPos.getMinBlockX();
        int minZ=chunkPos.getMinBlockZ();
        boolean flag=false;
        for(int i=0;i<16;i++){
            int x=minX+i;
            for(int j=0;j<16;j++){
                int z=minZ+j;
                int maxY=chunk.getMaxBuildHeight();//chunk.getHeight(water? Heightmap.Types.OCEAN_FLOOR_WG: Heightmap.Types.WORLD_SURFACE_WG,x,z)+1;
                BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos(x,maxY,z);
                for(int k=maxY;k>=chunk.getMinBuildHeight()+1;k--){
                    boolean flag1=helper(level,pos,context,water,predicate,feature);
                    flag=flag||flag1;
                    if(flag1&&!allLayers){
                        break;
                    }
                    pos.move(0,-1,0);
                }
            }
        }
        return flag;
    }
    private boolean helper(WorldGenLevel level, BlockPos pos, FeaturePlaceContext<AllSurfacesFeatureConfig> context,
                           boolean water, BlockPredicate predicate, PlacedFeature feature){
        BlockState state=level.getBlockState(pos);
        BlockState state1=level.getBlockState(pos.above());
        if(water?state.is(Blocks.WATER)&&state1.is(Blocks.WATER):state.is(Blocks.AIR)||
                state.is(Blocks.CAVE_AIR)||state.is(Blocks.VOID_AIR)){
            if(predicate.test(level,pos.below())){
                feature.place(level,context.chunkGenerator(),context.random(),pos);
                return true;
            }
        }
        return false;
    }
}
