package net.amurdza.examplemod.mixins.othermods.upgradeaquatic;

import com.teamabnormals.upgrade_aquatic.common.block.PickerelweedPlantBlock;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PickerelweedPlantBlock.class)
public abstract class PickelerweedGrowthRate {
    @Shadow public abstract boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos);

    @Redirect(method = "tick",at= @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private float hi(RandomSource instance, BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random){
        return Helper.withChanceToInt(worldIn,0.03D);
    }
}
