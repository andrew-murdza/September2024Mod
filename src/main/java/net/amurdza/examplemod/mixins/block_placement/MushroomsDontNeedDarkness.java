package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MushroomBlock.class)
public class MushroomsDontNeedDarkness {
    @Redirect(method = "canSurvive",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean canPlaceAt(BlockState instance, TagKey<Block> tag){
        return instance.is(tag)||instance.is(Blocks.MOSS_BLOCK);
    }
}
