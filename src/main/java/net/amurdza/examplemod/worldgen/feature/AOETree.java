package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.QuintConsumer;
import net.amurdza.examplemod.SixConsumer;
import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class AOETree extends Feature<AOETreeConfiguration> {

    public AOETree(Codec<AOETreeConfiguration> codec){
        super(codec);
    }
    @Override
    public boolean place(FeaturePlaceContext<AOETreeConfiguration> pContext) {
        WorldGenLevel level=pContext.level();
        BlockPos pos = pContext.origin();
        RandomSource random = pContext.random();
        Function<Integer,Integer> getCenter=x->((int)((double)x)/16)*16+8;
        pos=new BlockPos(getCenter.apply(pos.getX()),pos.getY(),getCenter.apply(pos.getZ()));
        AOETreeConfiguration config = pContext.config();
        BlockPos.MutableBlockPos pos1=pos.mutable();
        BiConsumer<BlockPos,Direction.Axis> placeTrunk=(x, axis)->{
            BlockState state=config.trunkProvider.getState(random,x).setValue(BlockStateProperties.AXIS,axis);
            level.setBlock(x,state,19);
        };
        Consumer<BlockPos> placeLeaves=x->level.setBlock(x,config.leafProvider.getState(random,x)
                .setValue(BlockStateProperties.PERSISTENT,true),19);
        BiConsumer<BlockPos,Direction> placeCocoa=(pos2, dir)->{
            BlockState cocoa=Blocks.COCOA.defaultBlockState().setValue(BlockStateProperties.AGE_2,2);
            cocoa=cocoa.setValue(BlockStateProperties.HORIZONTAL_FACING,dir);
            level.setBlock(pos2,cocoa,19);
        };
        QuintConsumer<Integer,Integer,BlockPos,Integer,Integer> placeTrunkCross=(minDist,maxDist,pos2,j,k)->{
            for(int i=minDist;i<maxDist+1;i++){
                placeTrunk.accept(pos2.offset(j+(j==0?-i:i),0,k), Direction.Axis.X);
                placeTrunk.accept(pos2.offset(j,0,k+(k==0?-i:i)), Direction.Axis.Z);
            }
        };
        SixConsumer<BlockPos,Integer,Integer,Boolean,Boolean,Boolean> placeVines=(pos2, j, k, xdir, ceiling,zdir)->{
            BlockState grapeVines=config.vineProvider.getState(random,pos2);

            if(grapeVines.getBlock() instanceof MultifaceBlock){
                BooleanProperty propertyX=j==0?BlockStateProperties.EAST:BlockStateProperties.WEST;
                BooleanProperty propertyZ=k==0?BlockStateProperties.SOUTH:BlockStateProperties.NORTH;
                grapeVines=grapeVines.setValue(propertyX,xdir).setValue(propertyZ,zdir).setValue(BlockStateProperties.UP,ceiling);
                pos2=pos2.offset(xdir?(j==0?-1:1):0,0,zdir?(k==0?-1:1):0);
                level.setBlock(pos2,grapeVines,19);
            }
        };
        TriConsumer<BlockPos,Integer,Integer> placeCocoas=(pos2, j, k)->{
            placeCocoa.accept(pos2.offset(j+(j==0?-1:1),0,k),j==0?Direction.EAST:Direction.WEST);
            placeCocoa.accept(pos2.offset(j,0,k+(k==0?-1:1)),k==0?Direction.SOUTH:Direction.NORTH);
        };
        QuintConsumer<Integer,Integer,BlockPos,Integer,Integer> placeLeavesSquare=(minDist,maxDist,pos2,j,k)->{
            for(int i=0;i<maxDist+1;i++){
                for(int l=0;l<maxDist+1;l++){
                    if(i>=minDist||l>=minDist){
                        placeLeaves.accept(pos2.offset(j+(j==0?-i:i),0,k+(k==0?-l:l)));
                    }
                }
            }
        };
        QuintConsumer<Integer,Integer,BlockPos,Integer,Integer> placeLeavesPlus=(padding,outerRadius,pos2,j,k)->{
            for(int i=0;i<outerRadius;i++){
                for(int l=0;l<outerRadius;l++){
                    int pDx = j + (j == 0 ? -i : i);
                    int pDz = k + (k == 0 ? -l : l);
                    if(padding==1){
                        if(!(i==outerRadius-1&&l>=outerRadius-2||l==outerRadius-1&&i>=outerRadius-2)){
                            placeLeaves.accept(pos2.offset(pDx,0, pDz));
                        }
                    }
                    else if(padding==2){
                        if(!(i==outerRadius-1&&l>=outerRadius-3||l==outerRadius-1&&i>=outerRadius-3)){
                            if(i!=outerRadius-2||j!=outerRadius-2){
                                placeLeaves.accept(pos2.offset(pDx,0, pDz));
                            }
                        }
                    }
                }
            }
        };
        BiConsumer<BlockPos,Direction> placeBeeNest=(pos2,dir)->{
            BlockState state=Blocks.BEE_NEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,dir);
            level.setBlock(pos2,state,19);
        };
        for(int i=0;i<20;i++){
            for(int j=0;j<2;j++){
                for(int k=0;k<2;k++){
                    if(i<10){
                        placeTrunk.accept(pos1.offset(j,0,k), Direction.Axis.Y);
                        if((i==4||i==5||i==7)&&config.hasCocoaBeans){
                            placeCocoas.accept(pos1,j,k);
                        }
                        else if(config.hasVinesOnTrunk){
                            placeVines.accept(pos1.offset(j,0,k),j,k,true,i==9,false);
                            placeVines.accept(pos1.offset(j,0,k),j,k,false,i==9,true);
                        }
                    }
                    else if(i<12){
                        placeTrunkCross.accept(0,1,pos1,j,k);
                        if(config.hasVinesOnTrunk){
                            placeVines.accept(pos1.offset(j,0,k),j,k,true,false,true);
                            placeVines.accept(pos1.offset(j==0?-1:2,0,k),j,k,true,false,false);
                            placeVines.accept(pos1.offset(j,0,k==0?-1:2),j,k,false,false,true);
                        }
                    }
                    else if(i<14){
                        placeTrunkCross.accept(0,2,pos1,j,k);
                    }
                    else if(i==14){
                        placeTrunkCross.accept(0,4,pos1,j,k);//2,4...
                        placeBeeNest.accept(pos1.offset(j==0?-1:2,0,k),k==0?Direction.NORTH:Direction.SOUTH);
                        placeBeeNest.accept(pos1.offset(j,0,k==0?-1:2),j==0?Direction.WEST:Direction.EAST);
                    }
                    else if(i==15){
                        placeLeavesSquare.accept(0,8,pos1,j,k);//3,8...
                        placeTrunk.accept(pos1.offset(j,0,k), Direction.Axis.Y);
                        placeTrunkCross.accept(3,3,pos1,j,k);
                    }
                    else if(i==16){
                        placeLeavesSquare.accept(0,8,pos1,j,k);
                        placeTrunk.accept(pos1.offset(j,0,k), Direction.Axis.Y);
                    }
                    else if(i==17){
                        placeLeavesPlus.accept(2,7,pos1,j,k);
                        placeTrunk.accept(pos1.offset(j,0,k), Direction.Axis.Y);
                    }
                    else if(i==18){
                        placeLeavesPlus.accept(1,5,pos1,j,k);
                    }
                    else {
                        placeLeavesPlus.accept(1,4,pos1,j,k);
                    }
                }
            }
            pos1.move(Direction.UP,1);
        }
        if(config.hasCaveVines){
            BlockState caveVinesHead=ModBlocks.CAVE_VINES_HEAD.get().defaultBlockState().setValue(BlockStateProperties.BERRIES,true)
                    .setValue(BlockStateProperties.AGE_25,25);
            for(int i=14;i>=10;i--){
                for(int j=0;j<2;j++){
                    for(int l=-3;l<5;l++){
                        if(l!=0&&l!=1){
                            level.setBlock(pos.offset(l,i,j==0?-4:5), caveVinesHead,19);
                            level.setBlock(pos.offset(j==0?-4:5,i,l), caveVinesHead,19);
                        }
                    }
                }
            }
        }
        if(config.hasSporeBlossoms){
            BlockState sporeBlossom=Blocks.SPORE_BLOSSOM.defaultBlockState();
            for(int j=0;j<2;j++){
                for(int k=0;k<2;k++){
                    int xFact=j==0?-1:1;
                    int zFact=k==0?-1:1;
                    level.setBlock(pos.offset(j+xFact*4,14,k+zFact*6), sporeBlossom,19);
                    level.setBlock(pos.offset(j+xFact*6,14,k+zFact*4), sporeBlossom,19);
                }
            }
        }
        return true;
    }
}
