package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.function.Predicate;

public class CaveVineColumn extends Feature<CaveVineConfig> {
    public CaveVineColumn(Codec<CaveVineConfig> p_66619_) {
        super(p_66619_);
    }

    public boolean place(FeaturePlaceContext<CaveVineConfig> context) {
        CaveVineConfig config = context.config();
        float chance=config.chance;
        IntProvider heightProvider=config.heightProvider;
        RandomSource random = context.random();
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
                    boolean shouldRun=random.nextFloat()<chance;
                    BlockPredicate pred=BlockPredicate.anyOf(BlockPredicate.matchesBlocks(Blocks.MOSS_BLOCK));
                    //,BlockPredicate.matchesTag(BlockTags.LEAVES)
                    //BlockPredicate.matchesBlocks(Blocks.MOSS_BLOCK) is temporary
                    boolean flag1=helper(level,pos,context,pred, heightProvider,shouldRun);
                    flag=flag||flag1;
                    pos.move(0,-1,0);
                }
            }
        }
        return flag;
    }
    private final Predicate<BlockState> tester= state->!state.is(Blocks.WATER)&&!state.is(Blocks.AIR)&&!state.is(Blocks.CAVE_AIR)
            &&!state.is(Blocks.VOID_AIR);
    private boolean helper(WorldGenLevel level, BlockPos pos, FeaturePlaceContext<CaveVineConfig> context,
                           BlockPredicate predicate, IntProvider heightProvider, boolean shouldRun){
        BlockState state=level.getBlockState(pos);
        if(!tester.test(state)&&predicate.test(level,pos.above())&&shouldRun){
            return placeHelper(level,context.chunkGenerator(),context.random(),pos,
                    heightProvider.sample(context.random()));
        }
        return false;
    }
    private boolean placeHelper(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos,
                                int h){
        BlockPos.MutableBlockPos pos1=pos.mutable();
        boolean flag=false;
        h=1;
        for(int i=0;i<h;i++){
            BlockState state=level.getBlockState(pos1);
//            for(int j=1;j<4;j++){
//                if(tester.test(level.getBlockState(pos1.below(j)))){
//                    break;
//                }
//            }
            level.setBlock(pos1,ModBlocks.CAVE_VINES.get().defaultBlockState().setValue(BlockStateProperties.BERRIES,true)
                    .setValue(BlockStateProperties.WATERLOGGED,state.is(Blocks.WATER)).setValue(BlockStateProperties.AGE_25,25),2);
            pos1.move(0,-1,0);
            flag=true;
        }
        return flag;
    }
}
