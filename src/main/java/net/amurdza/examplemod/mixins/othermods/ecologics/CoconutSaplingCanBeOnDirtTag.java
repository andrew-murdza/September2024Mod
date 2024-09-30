package net.amurdza.examplemod.mixins.othermods.ecologics;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import samebutdifferent.ecologics.block.CoconutSaplingBlock;

@Mixin(CoconutSaplingBlock.class)
public class CoconutSaplingCanBeOnDirtTag {
    @Redirect(method = "mayPlaceOn",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean hi(BlockState instance, Block block){
        return instance.is(block)||instance.is(BlockTags.DIRT);
    }
}
