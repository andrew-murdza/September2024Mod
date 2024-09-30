package net.amurdza.examplemod.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SporeBlossomBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SporeBlossomBlock.class)
public class SporeBlossomOnLeaves {
    @Redirect(method = "canSurvive",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;canSupportCenter(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    public boolean canPlaceAt(LevelReader level, BlockPos pos, Direction direction){
        return Block.canSupportCenter(level,pos,direction)||level.getBlockState(pos).is(BlockTags.LEAVES);
    }
}
