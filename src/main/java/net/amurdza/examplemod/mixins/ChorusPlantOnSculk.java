package net.amurdza.examplemod.mixins;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(ChorusPlantBlock.class)
public class ChorusPlantOnSculk {
    @Redirect(method = "canSurvive",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",ordinal = 2))
    private boolean hi(BlockState instance, Block block){
        return aOEMod1_20_1V2$isSoil(instance,block);
    }
    @Redirect(method = "canSurvive",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",ordinal = 4))
    private boolean hi1(BlockState instance, Block block){
        return aOEMod1_20_1V2$isSoil(instance,block);
    }

    @Unique
    private boolean aOEMod1_20_1V2$isSoil(BlockState instance, Block block){
        return instance.is(block)||instance.is(Blocks.SCULK)||instance.is(Blocks.SCULK_CATALYST);
    }
}
