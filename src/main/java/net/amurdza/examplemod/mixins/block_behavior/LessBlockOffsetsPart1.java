package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(BlockBehaviour.Properties.class)
public class LessBlockOffsetsPart1 {
    @Redirect(method = "offsetType",at= @At(value = "INVOKE", target = "Ljava/util/Optional;of(Ljava/lang/Object;)Ljava/util/Optional;"))
    private <T> Optional<T> hi(T value){
        return Optional.empty();
    }
}
