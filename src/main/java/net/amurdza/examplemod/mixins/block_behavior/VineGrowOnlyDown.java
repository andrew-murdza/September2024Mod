package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VineBlock.class)
public class VineGrowOnlyDown {
    @Redirect(method = "randomTick",at=@At(value = "INVOKE",target = "Lnet/minecraft/core/Direction;getRandom(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/core/Direction;"))
    public Direction hi(RandomSource pRandom){
        return Direction.DOWN;
    }
}
