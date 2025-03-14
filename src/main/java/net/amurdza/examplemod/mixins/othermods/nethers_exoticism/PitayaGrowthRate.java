package net.amurdza.examplemod.mixins.othermods.nethers_exoticism;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.mcreator.nethersexoticism.procedures.PitayaStemDownUpdateTickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PitayaStemDownUpdateTickProcedure.class)
public class PitayaGrowthRate {
    @Redirect(method = "execute",at= @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"),remap = false)
    private static double hi(final LevelAccessor world, double x, double y, double z){
        return Helper.isSpecialBiome(world,new BlockPos((int) x, (int) y, (int) z))?Helper.withChanceToInt(world,Config.PITAYA_GROWTH_CHANCE):Math.random();
    }
    @Redirect(method = "execute",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),remap = false)
    private static boolean hi(LevelAccessor instance, BlockPos blockPos, BlockState blockState, int i){
        boolean isStem=instance.getBlockState(blockPos.below(2)).is(NethersExoticismModBlocks.PITAYA_STEM.get());
        BlockState state=(isStem?NethersExoticismModBlocks.PITAYA_BLOCK:NethersExoticismModBlocks.PITAYA_STEM).get().defaultBlockState();
        return instance.setBlock(blockPos,state,i);
    }
}
