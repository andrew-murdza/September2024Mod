package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(MushroomBlock.class)
public class MushroomMixin {
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/util/RandomSource;nextInt(I)I",ordinal = 0))
    private int nextInt(RandomSource random, int n, BlockState state, ServerLevel world, BlockPos pos, RandomSource random1){
        return Helper.nextIntCropsGrow(world,pos,state,random,n);
    }

    @Redirect(method = "canSurvive",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean canPlaceAt(BlockState instance, TagKey<Block> tag){
        return instance.is(tag)||instance.is(Blocks.MOSS_BLOCK);
    }

    @ModifyConstant(method = "randomTick",constant = @Constant(intValue = 5))
    private int injected(int value,BlockState state, ServerLevel world, BlockPos pos, RandomSource random1) {
        return Helper.isSpecialBiome(world,pos)?Config.MAX_MUSHROOMS_FOR_GROWTH:value;
    }
}
