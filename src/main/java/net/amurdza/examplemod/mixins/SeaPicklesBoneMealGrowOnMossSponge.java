package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(SeaPickleBlock.class)
public class SeaPicklesBoneMealGrowOnMossSponge {
    @Redirect(method = "performBonemeal",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean getBlockState(BlockState instance, TagKey<Block> tag, ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState){
        if(Helper.isSpecialBiome(pLevel,pPos)){
            if(Helper.isOkToPlace(pLevel,pPos.above(),Blocks.SEA_PICKLE)){
                return false;
            }
            return instance.is(BlockTags.CORAL_BLOCKS)||instance.is(Blocks.MOSS_BLOCK)||instance.is(Blocks.WET_SPONGE);
        }
        return instance.is(BlockTags.CORAL_BLOCKS);
    }
}
