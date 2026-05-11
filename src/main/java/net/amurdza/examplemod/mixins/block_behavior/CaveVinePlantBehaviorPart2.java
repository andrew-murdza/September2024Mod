package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Blocks.class)
public class CaveVinePlantBehaviorPart2 {
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/CaveVinesPlantBlock;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V"
            ),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=cave_vines_plant")
            ),
            index = 0
    )
    private static BlockBehaviour.Properties aoe$makeCaveVinesPlantRandomTick(BlockBehaviour.Properties properties) {
        return properties.randomTicks();
    }
}
