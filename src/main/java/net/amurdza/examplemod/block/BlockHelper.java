package net.amurdza.examplemod.block;

import net.amurdza.examplemod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.Tags;

public class BlockHelper {
    public static InteractionResult useCaveVines(BlockState pState, Level pLevel, BlockPos pPos, Player pEntity, InteractionHand hand){
        if(hand != InteractionHand.MAIN_HAND || pEntity.getItemInHand(hand).is(Tags.Items.SHEARS)) {
            return InteractionResult.PASS;
        }
        boolean flag=false;
        Property<Boolean> property = null;
        SoundEvent sound=null;
        if (pState.getValue(BlockStateProperties.ENABLED)&&pEntity.getItemInHand(hand).getItem() instanceof AxeItem) {
            flag=true;
            property=BlockStateProperties.ENABLED;
            sound=SoundEvents.AXE_STRIP;
        }
        else if (pState.getValue(BlockStateProperties.BERRIES)) {
            flag=true;
            property=BlockStateProperties.BERRIES;
            Block.popResource(pLevel, pPos, new ItemStack(ModItems.GLOW_BERRIES.get(), 1));
            sound=SoundEvents.CAVE_VINES_PICK_BERRIES;
        }
        if(flag){
            float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
            pLevel.playSound(null, pPos, sound, SoundSource.BLOCKS, 1.0F, f);
            BlockState blockstate = pState.setValue(property, false);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
