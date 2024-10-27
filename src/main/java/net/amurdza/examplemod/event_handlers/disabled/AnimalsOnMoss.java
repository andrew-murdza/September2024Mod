package net.amurdza.examplemod.event_handlers.disabled;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class AnimalsOnMoss {
    @SubscribeEvent
    public static void updateSpawnConditions(final FMLCommonSetupEvent event) {
        //Handled by Data Pack
//        addSpawn(EntityType.MOOSHROOM,false,(p,q,r,s,t)->mooshroom(q,s),event);
//        addSpawn(EntityType.PARROT,false,(p,q,r,s,t)->parrot(q,s),event);
//        EntityType[] animals=new EntityType[]{EntityType.CHICKEN,EntityType.DONKEY,EntityType.HORSE,EntityType.COW,
//                EntityType.OCELOT,EntityType.PIG,EntityType.SHEEP,EntityType.CAT,EntityType.PANDA,
//                EntityType.WOLF,EntityType.MULE,EntityType.TURTLE};
//        EntityType[] snowAnimals=new EntityType[]{EntityType.RABBIT,EntityType.POLAR_BEAR,EntityType.FOX,
//                EntityType.LLAMA,EntityType.GOAT};
//        for(EntityType animal: animals){
//            addAnimal(animal,false,event);
//        }
//        for(EntityType animal: snowAnimals){
//            addAnimal(animal,true,event);
//        }
    }
//    public static boolean mooshroom(ServerLevelAccessor level, BlockPos pos) {
//        Block block=level.getBlockState(pos.below()).getBlock();
//        return List.of(Blocks.MOSS_BLOCK,Blocks.MYCELIUM,Blocks.PODZOL,Blocks.GRASS_BLOCK).contains(block);
//    }

    //    public static boolean parrot(ServerLevelAccessor level, BlockPos pos) {
//        List<Block> blocks= Arrays.asList(new Block[]{Blocks.GRASS_BLOCK,Blocks.PODZOL,Blocks.MOSS_BLOCK});
//        BlockState state=level.getBlockState(pos.below());
//        Block block=state.getBlock();
//        return blocks.contains(block)||state.is(BlockTags.LOGS_THAT_BURN)||state.is(BlockTags.LEAVES)
//                &&level.getRawBrightness(pos,0)>8;
//    }
//    public static void addAnimal(EntityType animal,boolean snow,FMLCommonSetupEvent event){
//        addSpawn(animal,false,(p,q,r,s,t)->animal(q,s,false),event);
//    }

//    public static boolean animal(ServerLevelAccessor level, BlockPos pos, boolean isSnow) {
//        List<Block> blocks= Arrays.asList(Blocks.GRASS_BLOCK,Blocks.PODZOL,Blocks.MOSS_BLOCK);
//        List<Block> snow=Arrays.asList(Blocks.SNOW_BLOCK,Blocks.SNOW,Blocks.ICE,Blocks.BLUE_ICE,Blocks.PACKED_ICE);
//        Block block=level.getBlockState(pos.below()).getBlock();
//        if(blocks.contains(block)||isSnow&&snow.contains(block)){
//            return level.getRawBrightness(pos,0)>8;//(block==Blocks.MOSS_BLOCK?7:8);
//        }
//        return false;
//    }
}
