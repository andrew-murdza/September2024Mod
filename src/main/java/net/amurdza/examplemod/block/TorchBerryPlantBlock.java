package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import twilightforest.block.TorchberryPlantBlock;

public class TorchBerryPlantBlock extends TorchberryPlantBlock {

    public TorchBerryPlantBlock(Properties properties) {
        super(properties);
    }
    public boolean isRandomlyTicking(BlockState pState) {
        return !pState.getValue(HAS_BERRIES);
    }

    public void randomTick(@NotNull BlockState pState, @NotNull ServerLevel pLevel, @NotNull BlockPos pPos, RandomSource pRandom) {
        if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(pLevel, pPos, pState, pRandom.nextInt(5) == 0)) {
            BlockState blockstate = pState.setValue(HAS_BERRIES, true);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(blockstate));
            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(pLevel, pPos, pState);
        }

    }
}
