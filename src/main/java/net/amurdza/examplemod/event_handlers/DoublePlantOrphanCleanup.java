package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DoublePlantOrphanCleanup {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();

        removeOrphanHalf(level, pos.above());
        removeOrphanHalf(level, pos.below());
    }

    private static void removeOrphanHalf(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof DoublePlantBlock) || !state.hasProperty(DoublePlantBlock.HALF)) {
            return;
        }

        DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
        BlockPos otherHalfPos = half == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
        BlockState otherHalfState = level.getBlockState(otherHalfPos);

        boolean hasMatchingHalf = otherHalfState.is(state.getBlock())
                && otherHalfState.hasProperty(DoublePlantBlock.HALF)
                && otherHalfState.getValue(DoublePlantBlock.HALF) != half;

        if (!hasMatchingHalf) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
