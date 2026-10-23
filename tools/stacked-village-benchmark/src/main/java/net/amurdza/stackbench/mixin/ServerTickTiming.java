package net.amurdza.stackbench.mixin;

import java.util.function.BooleanSupplier;
import net.amurdza.stackbench.BenchmarkMetrics;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Measures the complete server tick, including Forge's END event subscribers. */
@Mixin(MinecraftServer.class)
public abstract class ServerTickTiming {
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void stackbench$start(BooleanSupplier hasTimeLeft, CallbackInfo callback) {
        BenchmarkMetrics.tickStarted();
    }

    @Inject(method = "tickServer", at = @At("RETURN"))
    private void stackbench$finish(BooleanSupplier hasTimeLeft, CallbackInfo callback) {
        BenchmarkMetrics.tickFinished();
    }
}
