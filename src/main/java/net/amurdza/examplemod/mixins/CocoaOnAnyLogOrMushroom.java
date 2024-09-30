package net.amurdza.examplemod.mixins;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CocoaBlock.class)
public class CocoaOnAnyLogOrMushroom {
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hi(BlockState instance, TagKey<Block> tagKey){
        return instance.is(BlockTags.LOGS_THAT_BURN);//||instance.is(Blocks.MUSHROOM_STEM);
    }
}
