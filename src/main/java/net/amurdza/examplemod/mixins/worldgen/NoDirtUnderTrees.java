package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(TrunkPlacer.class)
public abstract class NoDirtUnderTrees {
//
//    @Inject(
//            method = "setDirtAt",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private static void aoemod$cancelSetDirtAt(
//            LevelSimulatedReader level,
//            BiConsumer<BlockPos, BlockState> blockSetter,
//            RandomSource random,
//            BlockPos pos,
//            TreeConfiguration config,
//            CallbackInfo ci
//    ) {
//        ci.cancel();
//    }
}