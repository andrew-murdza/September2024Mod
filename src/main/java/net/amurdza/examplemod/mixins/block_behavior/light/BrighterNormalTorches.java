package net.amurdza.examplemod.mixins.block_behavior.light;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Standing and wall torches share this constructor; soul and redstone torches retain their own emissions. */
@Mixin(TorchBlock.class)
public abstract class BrighterNormalTorches {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static BlockBehaviour.Properties aoemod$fullTorchLight(BlockBehaviour.Properties properties,
            @Local(argsOnly = true) ParticleOptions particle) {
        return particle == ParticleTypes.FLAME ? properties.lightLevel(state -> 15) : properties;
    }
}
