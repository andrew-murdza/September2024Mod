package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Blocks.class)
public class SoulSandSoilHoneyDontSlow {
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;speedFactor(F)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;"))
    private static BlockBehaviour.Properties hi(BlockBehaviour.Properties instance, float pSpeedFactor){
        return instance;
    }
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;jumpFactor(F)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;"))
    private static BlockBehaviour.Properties hi1(BlockBehaviour.Properties instance, float pSpeedFactor){
        return instance;
    }
}
