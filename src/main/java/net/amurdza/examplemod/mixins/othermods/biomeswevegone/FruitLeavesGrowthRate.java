package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.potionstudios.biomeswevegone.world.level.block.plants.tree.leaves.BWGFruitLeavesBlock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BWGFruitLeavesBlock.class)
public class FruitLeavesGrowthRate {
    @Redirect(method = "randomTick",at= @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private float hi(RandomSource instance, @NotNull BlockState state, @NotNull ServerLevel level,
                     @NotNull BlockPos pos, @NotNull RandomSource random){
        return Helper.isSpecialBiome(level,pos)?Helper.withChanceToInt(level,Config.FRUIT_LEAVES_CHANCE):
                instance.nextFloat();
    }
}
