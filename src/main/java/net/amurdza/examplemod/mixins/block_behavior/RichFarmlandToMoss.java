package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.block.RichSoilFarmlandBlock;

@Mixin(RichSoilFarmlandBlock.class)
public class RichFarmlandToMoss {
    @Redirect(method = "turnToRichSoil",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState hi(Block instance){
        return Blocks.MOSS_BLOCK.defaultBlockState();
    }
}
