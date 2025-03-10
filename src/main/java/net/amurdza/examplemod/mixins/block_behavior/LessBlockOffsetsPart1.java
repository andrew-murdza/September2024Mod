package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(BlockBehaviour.Properties.class)
public class LessBlockOffsetsPart1 {
    @Redirect(method = "offsetType",at= @At(value = "INVOKE", target = "Ljava/util/Optional;of(Ljava/lang/Object;)Ljava/util/Optional;",ordinal = 1))
    private <T> Optional<T> hi(T value){
        return Optional.empty();
    }
//    @Redirect(method = "offsetType",at= @At(value = "INVOKE", target = "Ljava/util/Optional;of(Ljava/lang/Object;)Ljava/util/Optional;",ordinal = 0))
//    private <T> Optional<BlockBehaviour.OffsetFunction> hi1(T value){
//        return Optional.of((p_272562_, p_272563_, p_272564_) -> {
//            Block block = p_272562_.getBlock();
//            long i = Mth.getSeed(p_272564_.getX(), 0, p_272564_.getZ());
//            double d0 = ((double)((float)(i >> 4 & 15L) / 15.0F) - 1.0) * (double)block.getMaxVerticalOffset();
////            float f = block.getMaxHorizontalOffset();
////            double d1 = Mth.clamp(((double)((float)(i & 15L) / 15.0F) - 0.5) * 0.5, (double)(-f), (double)f);
////            double d2 = Mth.clamp(((double)((float)(i >> 8 & 15L) / 15.0F) - 0.5) * 0.5, (double)(-f), (double)f);
//            return new Vec3(0, d0, 0);});//Vec3(d1, d0, d2);});
//    }
}


