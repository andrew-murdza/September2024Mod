package net.amurdza.examplemod.mixins.othermods.ecologics;

import net.amurdza.examplemod.util.CactusGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
@Mixin(CactusBlock.class)
public abstract class CactusAlwaysGrowsFruitPart2 {

    /**
     * Short cacti grow taller; columns of at least three blocks can instead grow prickly pears.
     * The Forge pre-event retains the configured growth multiplier for both paths.
     * @author amurdza
     * @reason simplest way to get the wanted behavior.
     */
    @Overwrite
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockPos blockpos = pPos.above();
        if (pLevel.isEmptyBlock(blockpos)) {
            if(ForgeHooks.onCropsGrowPre(pLevel, blockpos, pState, true)){
                CactusGrowth.advance(pLevel, pPos, pState, 1);
                ForgeHooks.onCropsGrowPost(pLevel, pPos, pState);
            }
        }
    }
}
