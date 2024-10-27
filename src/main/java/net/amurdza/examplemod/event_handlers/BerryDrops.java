package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BerryDrops {
    @SubscribeEvent
    public static void gatherFromGrapes(PlayerInteractEvent.RightClickBlock event){
        Level pLevel=event.getLevel();
        BlockPos pPos=event.getPos();
        Player pEntity=event.getEntity();
        BlockState pState=event.getLevel().getBlockState(pPos);
        int numGrapes=Helper.isSpecialBiome(pLevel,pPos)?Config.NUM_GRAPES:1;
        if(pState.is(ModBlocks.GRAPE_VINE.get())&&pState.getValue(BlockStateProperties.BERRIES)){
            Block.popResource(pLevel, pPos, new ItemStack(ModBlocks.GRAPE_VINE.get().asItem(), numGrapes));
            float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
            pLevel.playSound(null, pPos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
            BlockState blockstate = pState.setValue(BlockStateProperties.BERRIES, false);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
        }
    }

    @SubscribeEvent
    public static void berriesFromUse(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        BlockPos pos=event.getPos();
        BlockState state=level.getBlockState(pos);
        Block block=state.getBlock();
        if(Helper.isSpecialBiome(level,pos)){
            int amount=0;
            Item item = null;
            if((block instanceof CaveVines)&&state.getValue(BlockStateProperties.BERRIES)){
                amount=Config.GLOW_BERRY_HARVEST_AMOUNT-1;
                item=ModItems.GLOW_BERRIES.get();
            }
            if(state.is(Blocks.SWEET_BERRY_BUSH)){
                int age=state.getValue(BlockStateProperties.AGE_3);
                item=Items.SWEET_BERRIES;
                amount=0;
                if(age==2){
                    amount= Config.SWEET_BERRIES_PARTIALLY_GROWN;
                }
                if(age==3){
                    amount = Config.SWEET_BERRIES_FULLY_GROWN;
                }
            }
            if(state.is(ModBlocks.GRAPE_VINE.get())&&state.getValue(BlockStateProperties.BERRIES)){

            }
            if(amount>0){
                Block.popResource(level, pos, new ItemStack(item,amount));
            }
        }
    }
}
