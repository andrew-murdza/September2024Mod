package net.amurdza.examplemod.block;

import net.amurdza.examplemod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.Tags;

public class BlockHelper {
    public static InteractionResult useCaveVines(BlockState pState, Level pLevel, BlockPos pPos, Player pEntity, InteractionHand hand){
        if(hand != InteractionHand.MAIN_HAND || pEntity.getItemInHand(hand).is(Tags.Items.SHEARS)) {
            return InteractionResult.PASS;
        }
        if (pState.getValue(BlockStateProperties.BERRIES)) {
            Block.popResource(pLevel, pPos, new ItemStack(ModItems.GLOW_BERRIES.get(), 1));
            float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
            pLevel.playSound(null, pPos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
            BlockState blockstate = pState.setValue(BlockStateProperties.BERRIES, Boolean.FALSE);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
