package net.amurdza.examplemod.mixins.othermods.snowrealmagic;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.snow.block.EntitySnowLayerBlock;

@Mixin(EntitySnowLayerBlock.class)
public class BerriesDontSlowMobs {
    @Redirect(method = "entityInside",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;",ordinal = 1))
    private Block hi(BlockState instance){
        return Blocks.AIR;
    }
}
