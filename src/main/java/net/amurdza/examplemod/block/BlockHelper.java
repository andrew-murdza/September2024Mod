package net.amurdza.examplemod.block;

import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.util.QuadConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;

public class BlockHelper {
    public static InteractionResult useCaveVines(BlockState pState, Level pLevel, BlockPos pPos, Player pEntity, InteractionHand hand){
        BlockState blockState=null;

        SoundEvent sound=null;
        if (pState.getValue(BlockStateProperties.ENABLED)&&pEntity.getItemInHand(hand).getItem() instanceof AxeItem) {
            blockState = pState.setValue(BlockStateProperties.ENABLED,false);
            sound=SoundEvents.AXE_STRIP;
        }
        else if(pState.getValue(BlockStateProperties.AGE_25)<25&&pEntity.getItemInHand(hand).is(Tags.Items.SHEARS)){
            blockState = pState.setValue(BlockStateProperties.AGE_25,25);
            sound=SoundEvents.SHEEP_SHEAR;
        }
        else if (pState.getValue(BlockStateProperties.BERRIES)) {
            blockState = pState.setValue(BlockStateProperties.BERRIES,false);
            Block.popResource(pLevel, pPos, new ItemStack(Items.GLOW_BERRIES, BlockConfig.GLOW_BERRY_HARVEST_AMOUNT));
            sound=SoundEvents.CAVE_VINES_PICK_BERRIES;
        }
        if(blockState != null){
            float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
            pLevel.playSound(null, pPos, sound, SoundSource.BLOCKS, 1.0F, f);
            pLevel.setBlock(pPos, blockState, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockState));
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }
        return InteractionResult.FAIL;
    }
    public static void spreadMushroom(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                        QuadConsumer<ServerLevel,BlockPos,BlockState,RandomSource> growMushroom){
        if (ForgeHooks.onCropsGrowPre(level,pos,state,random.nextInt(25)==0)) {
            int i = BlockConfig.MAX_MUSHROOMS_FOR_GROWTH;

            for (BlockPos scanPos : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
                if (level.getBlockState(scanPos).is(state.getBlock())) {
                    --i;
                }
            }

            if (i <= 0 && random.nextInt(4) == 0) {
                BlockState floorState = level.getBlockState(pos.below());
                if(floorState.is(BlockTags.MUSHROOM_GROW_BLOCK)){
                    growMushroom.accept(level, pos, state, random);
                }
                return;
            }

            BlockPos targetPos = pos.offset(
                    random.nextInt(3) - 1,
                    random.nextInt(2) - random.nextInt(2),
                    random.nextInt(3) - 1
            );

            for (int k = 0; k < 4; ++k) {
                if (level.isEmptyBlock(targetPos) && state.canSurvive(level, targetPos)) {
                    pos = targetPos;
                }

                targetPos = pos.offset(
                        random.nextInt(3) - 1,
                        random.nextInt(2) - random.nextInt(2),
                        random.nextInt(3) - 1
                );
            }

            if (level.isEmptyBlock(targetPos) && state.canSurvive(level, targetPos)) {
                level.setBlock(targetPos, state, 2);
            }
        }
    }
}
