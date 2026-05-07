package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;

public class SeaPickleNew extends SeaPickleBlock {
    public static final BooleanProperty SHEARED = BooleanProperty.create("sheared");

    public SeaPickleNew(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(PICKLES, 1)
                .setValue(WATERLOGGED, true)
                .setValue(SHEARED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SHEARED);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(PICKLES) < 4
                && state.getValue(WATERLOGGED)
                && !state.getValue(SHEARED);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SHEARED)
                && state.getValue(PICKLES) < 4
                && ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt(4) == 0)) {
            level.setBlockAndUpdate(pos, state.setValue(PICKLES, state.getValue(PICKLES) + 1));
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(Items.SHEARS) && !state.getValue(SHEARED)) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(SHEARED, true));
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);

                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }
}