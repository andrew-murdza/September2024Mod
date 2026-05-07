package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BambooShearing {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!state.is(Blocks.BAMBOO)&&!state.is(Blocks.BAMBOO_SAPLING)) return;
        if (!stack.canPerformAction(net.minecraftforge.common.ToolActions.SHEARS_HARVEST)) return;

        if (!level.isClientSide) {
            BlockPos top = pos;

            while (level.getBlockState(top.above()).is(Blocks.BAMBOO)) {
                top = top.above();
            }

            BlockState topState = level.getBlockState(top);

            if (topState.hasProperty(BambooStalkBlock.STAGE)
                    && topState.getValue(BambooStalkBlock.STAGE) == 0) {
                level.setBlock(top, topState.setValue(BambooStalkBlock.STAGE, 1), 3);

                stack.hurtAndBreak(1, player, p ->
                        p.broadcastBreakEvent(event.getHand())
                );

                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            else if (state.is(Blocks.BAMBOO_SAPLING)) {
                level.setBlock(pos,
                        Blocks.BAMBOO.defaultBlockState()
                                .setValue(BambooStalkBlock.STAGE, 1)
                                .setValue(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.NONE),
                        3
                );

                stack.hurtAndBreak(1, player, p ->
                        p.broadcastBreakEvent(event.getHand())
                );

                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }
}
