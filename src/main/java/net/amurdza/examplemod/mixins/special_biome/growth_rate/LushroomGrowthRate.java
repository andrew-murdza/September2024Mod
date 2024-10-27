package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import com.belgieyt.trailsandtalesplus.Objects.blocks.LushroomBlock;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LushroomBlock.class)
public class LushroomGrowthRate {
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/util/RandomSource;nextInt(I)I",ordinal = 0))
    private int nextInt(RandomSource random, int n, BlockState state, ServerLevel world, BlockPos pos, RandomSource random1){
        return Helper.nextIntCropsGrow(world,pos,state,random,5);//n
    }

    @ModifyConstant(method = "randomTick",constant = @Constant(intValue = 5))
    private int injected(int value,BlockState state, ServerLevel world, BlockPos pos, RandomSource random1) {
        return Helper.isSpecialBiome(world,pos)?Config.MAX_MUSHROOMS_FOR_GROWTH:value;
    }
}
