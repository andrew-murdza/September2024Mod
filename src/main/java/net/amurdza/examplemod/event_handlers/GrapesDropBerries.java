package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class GrapesDropBerries {
    @SubscribeEvent
    public static void gatherFromGrapes(PlayerInteractEvent.RightClickBlock event){
        Level pLevel=event.getLevel();
        BlockPos pPos=event.getPos();
        Player pEntity=event.getEntity();
        BlockState pState=event.getLevel().getBlockState(pPos);
        int numGrapes= Helper.isSpecialBiome(pLevel,pPos)? Config.NUM_GRAPES:1;
        if(pState.is(ModBlocks.GRAPE_VINE.get())&&pState.getValue(BlockStateProperties.BERRIES)){
            Block.popResource(pLevel, pPos, new ItemStack(ModBlocks.GRAPE_VINE.get().asItem(), numGrapes));
            float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
            pLevel.playSound(null, pPos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
            BlockState blockstate = pState.setValue(BlockStateProperties.BERRIES, false);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
        }
    }
}
