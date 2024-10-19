package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.RootsBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RootsBlock.class)
public class NetherRootsPlaceableOnMoreBlocks {
//    @Redirect(method = "mayPlaceOn",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
//    private boolean hi(BlockState instance, Block block){
//        return instance.is(ModTags.Blocks.netherRootsPlaceable);
//    }
}
