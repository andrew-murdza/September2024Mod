package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BlockGrowth {
    private static final IntegerProperty[] agePropertyList=new IntegerProperty[]{
            BlockStateProperties.AGE_1,BlockStateProperties.AGE_2, BlockStateProperties.AGE_3,
            BlockStateProperties.AGE_4,BlockStateProperties.AGE_5, BlockStateProperties.AGE_7,
            BlockStateProperties.AGE_15,BlockStateProperties.AGE_25};
    private static final int[] maxAges=new int[]{15,1,2,3,4,5,7,25};
    @SubscribeEvent
    public static void blockGrowthPre(BlockEvent.CropGrowEvent.Pre event) {
        BlockState state=event.getState();
        Block block=state.getBlock();
        BlockPos pos=event.getPos();
        ServerLevel level=(ServerLevel) event.getLevel();
        if(Helper.isSpecialBiome(level,pos)&&Config.BLOCK_GROWTH_DATA.containsKey(block)){
            double chance=Config.BLOCK_GROWTH_DATA.get(block).d;
            if(block instanceof CropBlock||block instanceof StemBlock){
                if(!level.getBlockState(pos.below()).isFertile(level,pos.below())){
                    chance=chance/2;
                }
            }
            int numOfAgeGrowths=Config.BLOCK_GROWTH_DATA.get(block).i;
            boolean successful=Helper.withChance(level,chance);
            event.setResult(Event.Result.ALLOW);
            if(successful){
                if(numOfAgeGrowths!=1){
                    for(int i=0;i<agePropertyList.length;i++){
                        event.setResult(Event.Result.DENY);
                        IntegerProperty property=agePropertyList[i];
                        if(state.hasProperty(property)){
                            int newAge=state.getValue(property)+numOfAgeGrowths;
                            if(newAge>maxAges[i]){
                                level.setBlockAndUpdate(pos, block.defaultBlockState());
                                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos.below(), block.defaultBlockState());
                                level.setBlock(pos.below(), state.setValue(property,0),3);
                            }
                            else{
                                level.setBlock(pos, state.setValue(property,newAge), 4);
                            }
                        }
                    }
                }
            }
            //sapling
            //mushroom
            else{
                event.setResult(Event.Result.DENY);
            }
        }
    }
}
