package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SeaPickleBlock.class)
public class SeaPickleBehavior extends BushBlock {
    @Unique
    private static final BooleanProperty AOE_SHEARED = BooleanProperty.create("sheared");

    @Shadow
    @Final
    public static IntegerProperty PICKLES;

    @Shadow
    @Final
    public static BooleanProperty WATERLOGGED;

    public SeaPickleBehavior(Properties pProperties) {
        super(pProperties);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoe$addShearedDefaultState(BlockBehaviour.Properties properties, CallbackInfo ci) {
        this.registerDefaultState(
                defaultBlockState()
                        .setValue(PICKLES, 1)
                        .setValue(WATERLOGGED, true)
                        .setValue(AOE_SHEARED, false)
        );
    }
/*

 */
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"),cancellable = true)
    public void isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState, boolean pIsClient, CallbackInfoReturnable<Boolean> cir) {
       cir.setReturnValue(pState.getValue(PICKLES) < 4 && pState.getValue(BlockStateProperties.WATERLOGGED));
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"),cancellable = true)
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState, CallbackInfo ci) {
        BlockState state = pLevel.getBlockState(pPos).cycle(BlockStateProperties.PICKLES);
        pLevel.setBlock(pPos,state,2);
        ci.cancel();
    }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void aoe$addShearedProperty(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder,
                                        CallbackInfo ci) {
        builder.add(AOE_SHEARED);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(PICKLES) < 4
                && state.getValue(WATERLOGGED)
                && !state.getValue(AOE_SHEARED);
    }

    @Override
    public void randomTick(BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!state.getValue(AOE_SHEARED)
                && state.getValue(PICKLES) < 4
                && state.getValue(BlockStateProperties.WATERLOGGED)
                && ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt(4) == 0)) {
            level.setBlockAndUpdate(pos, state.setValue(PICKLES, state.getValue(PICKLES) + 1));
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!aoemod$isShears(stack) || state.getValue(AOE_SHEARED)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            BlockState shearedState = state.setValue(AOE_SHEARED, true);
            level.setBlock(pos, shearedState, 3);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.SHEAR, pos);
            stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Unique
    private static boolean aoemod$isShears(ItemStack stack) {
        return stack.is(Tags.Items.SHEARS) || stack.getItem() instanceof ShearsItem;
    }
}
