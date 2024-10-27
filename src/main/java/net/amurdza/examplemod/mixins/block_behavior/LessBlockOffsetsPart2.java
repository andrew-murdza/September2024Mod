package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Blocks.class)
public class LessBlockOffsetsPart2 {
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;offsetType(Lnet/minecraft/world/level/block/state/BlockBehaviour$OffsetType;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;",ordinal=2))
    private static BlockBehaviour.Properties hi(BlockBehaviour.Properties instance, BlockBehaviour.OffsetType pOffsetType){
        return instance;
    }
}
