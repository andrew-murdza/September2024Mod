package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(SaplingBlock.class)
public class WitchHazelGrowsInDarkness {
    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getMaxLocalRawBrightness(Lnet/minecraft/core/BlockPos;)I"
            )
    )
    private int aoemod$ignoreLightForOneSapling(ServerLevel level, BlockPos brightnessPos) {
        BlockPos saplingPos = brightnessPos.below();
        BlockState state = level.getBlockState(saplingPos);

        if (state.getBlock() == Objects.requireNonNull(BWGWood.WITCH_HAZEL.sapling()).getBlock()) {
            return 15;
        }

        return level.getMaxLocalRawBrightness(brightnessPos);
    }
}
