package net.amurdza.examplemod.block;

import net.amurdza.examplemod.config.BlockBehaviorConfig;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;

public class BlockHelper {
    public static boolean canGrowGiantMushroom(LevelReader level, BlockPos mushroomPos) {
        return !GameplayDimensions.isCustom(level)
                || GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                || GameplayDimensions.biomeIs(level, mushroomPos, "mushroom_caves");
    }

    public static InteractionResult useCaveVines(BlockState pState, Level pLevel, BlockPos pPos, Player pEntity, InteractionHand hand){
        BlockState blockState=null;
        ItemStack heldItem = pEntity.getItemInHand(hand);

        SoundEvent sound=null;
        if (heldItem.getItem() instanceof AxeItem) {
            if(pState.getValue(BlockStateProperties.ENABLED)){
                blockState = pState.setValue(BlockStateProperties.ENABLED,false);
                sound=SoundEvents.AXE_STRIP;
            }
            else {
                return InteractionResult.sidedSuccess(pLevel.isClientSide);
            }
        }
        else if(isShears(heldItem)){
            if(pState.is(Blocks.CAVE_VINES)&&pState.getValue(BlockStateProperties.AGE_25)<25){
                blockState = pState.setValue(BlockStateProperties.AGE_25,25);
                sound=SoundEvents.SHEEP_SHEAR;
            }
            else {
                return InteractionResult.sidedSuccess(pLevel.isClientSide);
            }
        }
        else if (pState.getValue(BlockStateProperties.BERRIES)&&!heldItem.is(Items.GLOW_BERRIES)) {
            blockState = pState.setValue(BlockStateProperties.BERRIES,false);
            Float value = BlockBehaviorConfig.DIMENSION_TO_GLOW_BERRY_AMOUNTS.get(pLevel.dimension());
            int count = Helper.computeIncrements(pLevel.random, value != null ? value : 1.0f);
            Block.popResource(pLevel, pPos, new ItemStack(Items.GLOW_BERRIES, count));
            sound=SoundEvents.CAVE_VINES_PICK_BERRIES;
        }
        if(blockState != null){
            float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
            pLevel.playSound(null, pPos, sound, SoundSource.BLOCKS, 1.0F, f);
            pLevel.setBlock(pPos, blockState, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockState));
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static boolean isShears(ItemStack stack) {
        return stack.is(Tags.Items.SHEARS) || stack.getItem() instanceof ShearsItem;
    }

    public static void spreadMushroom(BlockState state, ServerLevel level, BlockPos pos, RandomSource random){
        if (ForgeHooks.onCropsGrowPre(level,pos,state,random.nextInt(25)==0)) {
            int i = BlockBehaviorConfig.MAX_MUSHROOMS_FOR_GROWTH;

            for (BlockPos scanPos : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
                if (level.getBlockState(scanPos).is(state.getBlock())) {
                    --i;
                }
            }

            if (i <= 0) {
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
