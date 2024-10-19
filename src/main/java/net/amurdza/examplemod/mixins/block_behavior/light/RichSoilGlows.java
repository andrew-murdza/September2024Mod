package net.amurdza.examplemod.mixins.block_behavior.light;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import vectorwing.farmersdelight.common.block.RichSoilBlock;

@Mixin(RichSoilBlock.class)
public class RichSoilGlows {
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V"))
    private static BlockBehaviour.Properties hi(BlockBehaviour.Properties pProperties){
        return pProperties.lightLevel(p->15);
    }
}
