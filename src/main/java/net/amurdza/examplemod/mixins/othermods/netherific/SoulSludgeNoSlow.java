package net.amurdza.examplemod.mixins.othermods.netherific;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mcreator.nourishednether.block.SoulSludgeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SoulSludgeBlock.class)
public class SoulSludgeNoSlow {
    @WrapOperation(method = "<init>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;jumpFactor(F)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;"))
    private static BlockBehaviour.Properties hi(BlockBehaviour.Properties instance, float pJumpFactor,
                                                Operation<BlockBehaviour.Properties> original){
        return instance;
    }
}
