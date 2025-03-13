package net.amurdza.examplemod.mixins.othermods.nethersdelight;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import umpaz.nethersdelight.common.block.PropelplantBerryStemBlock;
import umpaz.nethersdelight.common.block.PropelplantStemBlock;

@Mixin(PropelplantBerryStemBlock.class)
public class PropellantCanePlacement1 {
    @Redirect(method = "mayPlaceOn",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hi(BlockState instance, TagKey tagKey){
        return instance.is(tagKey)||instance.is(Blocks.MOSS_BLOCK);
    }
}
