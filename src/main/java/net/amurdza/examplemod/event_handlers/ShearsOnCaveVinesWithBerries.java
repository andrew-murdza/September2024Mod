package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class ShearsOnCaveVinesWithBerries {

    @SubscribeEvent
    public static void shearsPreventGrowingBerries(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        BlockPos blockpos=event.getPos();
        ItemStack itemStack=event.getItemStack();
        BlockState blockstate=event.getLevel().getBlockState(blockpos);
        if(itemStack.is(ItemTags.AXES)){
            if(blockstate.is(ModBlocks.CAVE_VINES_PLANT.get())||blockstate.is(ModBlocks.CAVE_VINES.get())&&
                    blockstate.getValue(BlockStateProperties.ENABLED)){
                Player player = event.getEntity();
                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemStack);
                }
                level.playSound(player, blockpos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
                BlockState blockstate1 = blockstate.setValue(BlockStateProperties.ENABLED,false);
                level.setBlockAndUpdate(blockpos, blockstate1);
                level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, blockstate1));
                if (player != null) {
                    itemStack.hurtAndBreak(1, player, (p_186374_) -> p_186374_.broadcastBreakEvent(event.getHand()));
                }
            }
        }
    }

}
