package net.amurdza.examplemod.mixins.no_fire_in_tropical;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class NoFireSpreadInTropical {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aoemod$removeFireInTropicalBiomes(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        if (level.getBiome(pos).is(ModTags.Biomes.tropicalBiomes)) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }
}
