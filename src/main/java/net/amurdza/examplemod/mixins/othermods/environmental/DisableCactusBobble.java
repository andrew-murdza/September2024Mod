package net.amurdza.examplemod.mixins.othermods.environmental;

import com.teamabnormals.environmental.core.EnvironmentalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.BlockColumnFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockColumnFeature.class,priority = 1001)
public abstract class DisableCactusBobble {
//    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
//    private boolean place(WorldGenLevel level, BlockPos pos, BlockState state, int i) {
//        return level.setBlock(pos, state, i);
//    }
}