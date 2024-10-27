package net.amurdza.examplemod.event_handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class HoeOnPodzolMoss {
    @SubscribeEvent
    public static void hoeOnMossPodzol(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        Player player=event.getEntity();
        BlockPos pos=event.getPos();
        BlockState blockState=level.getBlockState(pos);
        if(event.getItemStack().getItem() instanceof HoeItem){
            if(blockState.is(Blocks.MOSS_BLOCK)|| blockState.is(Blocks.PODZOL)){
                level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!level.isClientSide) {
                    Block block=Blocks.FARMLAND;
                    //begin new
                    if(blockState.is(Blocks.MOSS_BLOCK)){
                        block=vectorwing.farmersdelight.common.registry.ModBlocks.RICH_SOIL_FARMLAND.get();
                    }
                    //end new
                    level.setBlock(pos,block.defaultBlockState(),11);
                    if (player != null) {
                        event.getItemStack().hurtAndBreak(1, player, (p_150845_) -> p_150845_.broadcastBreakEvent(event.getHand()));
                    }
                }
            }
        }
    }
}
