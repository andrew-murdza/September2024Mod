package net.amurdza.examplemod.mixins.special_biome.bonemeal;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(BoneMealItem.class)
public class BoneMealWaterPlants {
    @Redirect(method = "growWaterPlant",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",ordinal = 0))
    private static boolean useOnGround(BlockState instance, Block block,ItemStack pStack, Level pLevel, BlockPos pPos, @Nullable Direction pClickedSide){
        BlockState state=pLevel.getBlockState(pPos.relative(Helper.reverseDirection(pClickedSide)));
        return instance.is(block)&&!(Helper.isSpecialBiome(pLevel,pPos)&&state.is(Blocks.MOSS_BLOCK));
    }

    //Sponges in mountains
//    @Redirect(method = "growWaterPlant",
//            at = @At(value = "INVOKE",
//                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
//    private static boolean useOnGround2(Level world, BlockPos pos, BlockState state, int flags){
//        if(BiomeScores.isMountains(world,pos)&&world.random.nextInt(20)==0){
//            state=Blocks.WET_SPONGE.defaultBlockState();
//        }
//        return world.setBlockAndUpdate(pos,state);
//    }
}
