package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.world.IafWorldRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IafWorldRegistry.class, remap = false)
public abstract class PixieSpawnCorrection {

    @Inject(
            method = "isFarEnoughFromSpawn",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void aoemod$fixSpawnDistanceCheck(LevelAccessor level, BlockPos position, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}