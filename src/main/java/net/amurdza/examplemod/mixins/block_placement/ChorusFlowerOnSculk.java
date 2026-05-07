package net.amurdza.examplemod.mixins.block_placement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerOnSculk {
    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/ChorusFlowerBlock;AGE:Lnet/minecraft/world/level/block/state/properties/IntegerProperty;", opcode = Opcodes.GETSTATIC)
            ),
            require = 0
    )
    private boolean aoe$soilOverride(BlockState state, Block block) {
        return aOEMod1_20_1V2$isSoil(state, block);
    }

    @ModifyExpressionValue(
            method = "canSurvive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean aoe$allowExtraSoil(boolean original, BlockState blockstate) {
        return aOEMod1_20_1V2$isSoil(blockstate, Blocks.END_STONE);
    }

    @Unique
    private boolean aOEMod1_20_1V2$isSoil(BlockState instance, Block block){
        return instance.is(block)||instance.is(Blocks.SCULK)||instance.is(Blocks.SCULK_CATALYST);
    }
}
