package net.amurdza.examplemod.mixins.othermods.nethersdelight;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import umpaz.nethersdelight.common.block.PropelplantBerryCaneBlock;
import umpaz.nethersdelight.common.block.PropelplantBerryStemBlock;

@Mixin(PropelplantBerryStemBlock.class)
public class PropellantCaneGrowth {
    @Redirect(method = "randomTick",at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I",ordinal = 1))
    private int hi(RandomSource instance, int i){
        return 0;
    }
    @Redirect(method = "randomTick",at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I",ordinal = 0))
    private int hi1(RandomSource instance, int i, BlockState state, ServerLevel level, BlockPos pos, RandomSource random){
        return growthChance(level,pos,instance);
    }
    @Unique
    private int growthChance(ServerLevel level, BlockPos pos, RandomSource instance){
        return Helper.isSpecialBiome(level, pos) ? Helper.withChanceToInt(level, Config.PROPEL_GROWTH_CHANCE):instance.nextInt(8);
    }
}
