package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.block.TrollRootBlock;
import twilightforest.init.TFBlocks;

@Mixin(TrollRootBlock.class)
public class TrollberBecomesUnripeWhenHarvested {
    @Redirect(method = "use",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hi(Block instance){
        return TFBlocks.UNRIPE_TROLLBER.get().defaultBlockState();
    }
}
