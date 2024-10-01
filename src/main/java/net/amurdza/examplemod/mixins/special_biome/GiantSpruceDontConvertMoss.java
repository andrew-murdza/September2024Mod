package net.amurdza.examplemod.mixins.special_biome;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AlterGroundDecorator.class)
public class GiantSpruceDontConvertMoss {
    @Redirect(method = "placeBlockAt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/Feature;isGrassOrDirt(Lnet/minecraft/world/level/LevelSimulatedReader;Lnet/minecraft/core/BlockPos;)Z"))
    public boolean dontConvertMoss(LevelSimulatedReader world, BlockPos pos){
        BlockState state;
        if(world instanceof Level){
            state=((Level)world).getBlockState(pos);
        }
        else{
            state=((ServerLevelAccessor)world).getBlockState(pos);
        }
        return state.is(BlockTags.DIRT)&&!state.is(Blocks.MOSS_BLOCK);
    }
}
