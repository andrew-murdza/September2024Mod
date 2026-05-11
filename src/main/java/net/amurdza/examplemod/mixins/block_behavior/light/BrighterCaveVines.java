package net.amurdza.examplemod.mixins.block_behavior.light;

import net.minecraft.world.level.block.CaveVines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(net.minecraft.world.level.block.Blocks.class)
public class BrighterCaveVines {
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;lightLevel(Ljava/util/function/ToIntFunction;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;",
                    ordinal = 48
            ),
            index = 0
    )
    private static java.util.function.ToIntFunction<?> aoe$caveVinesLight(
            java.util.function.ToIntFunction<?> original
    ) {
        return CaveVines.emission(15);
    }

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;lightLevel(Ljava/util/function/ToIntFunction;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;",
                    ordinal = 49
            ),
            index = 0
    )
    private static java.util.function.ToIntFunction<?> aoe$caveVinesPlantLight(
            java.util.function.ToIntFunction<?> original
    ) {
        return CaveVines.emission(15);
    }
}
