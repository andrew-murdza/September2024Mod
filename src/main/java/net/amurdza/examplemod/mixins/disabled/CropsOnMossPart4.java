package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public class CropsOnMossPart4 {
//    @Redirect(method = "canSustainPlant",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",ordinal = 1))
//    private boolean hi(BlockState instance, Block block){
//        return instance.is(block)||instance.is(Blocks.MOSS_BLOCK);
//    }
}
