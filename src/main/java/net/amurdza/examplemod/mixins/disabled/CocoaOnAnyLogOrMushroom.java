package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.CocoaBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CocoaBlock.class)
public class CocoaOnAnyLogOrMushroom {
//    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
//    private boolean hi(BlockState instance, TagKey<Block> tagKey){
//        return instance.is(BlockTags.LOGS_THAT_BURN);//||instance.is(Blocks.MUSHROOM_STEM);
//    }
}
