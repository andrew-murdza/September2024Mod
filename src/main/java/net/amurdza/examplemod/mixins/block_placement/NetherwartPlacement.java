package net.amurdza.examplemod.mixins.block_placement;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NetherWartBlock.class, priority = 1001)
public class NetherwartPlacement {
    @Redirect(method = "mayPlaceOn",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean hi(BlockState instance, Block block){
        return instance.is(ModTags.Blocks.netherWartPlaceable);
    }
}
