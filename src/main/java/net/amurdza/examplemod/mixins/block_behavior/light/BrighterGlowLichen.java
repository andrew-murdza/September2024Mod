package net.amurdza.examplemod.mixins.block_behavior.light;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.ToIntFunction;

@Mixin(Blocks.class)
public class BrighterGlowLichen {
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/GlowLichenBlock;emission(I)Ljava/util/function/ToIntFunction;"))
    private static ToIntFunction<BlockState> hi(int pLight){
        return GlowLichenBlock.emission(15);
    }
}
