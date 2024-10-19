package net.amurdza.examplemod.mixins.block_behavior.light;

import net.minecraft.world.level.block.MossBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MossBlock.class)
public class MossBlocksGlow {
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V"))
    private static BlockBehaviour.Properties hi(BlockBehaviour.Properties pProperties){
        return pProperties.lightLevel(p->15);
    }
}
