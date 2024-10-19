package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Blocks.class)
public class BrighterGlowLichen {
//    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/GlowLichenBlock;emission(I)Ljava/util/function/ToIntFunction;"))
//    private static ToIntFunction<BlockState> hi(int pLight){
//        return GlowLichenBlock.emission(15);
//    }
}
