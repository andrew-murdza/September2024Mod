package net.amurdza.examplemod.mixins.no_fire_in_tropical;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseFireBlock.class)
public class NoBaseFirePlacementInTropical {

    @Inject(method = "canBePlacedAt", at = @At("HEAD"), cancellable = true)
    private static void aoemod$cannotPlaceFireInTropical(
            Level pLevel, BlockPos pPos, Direction pDirection, CallbackInfoReturnable<Boolean> cir
    ) {
        if (pLevel.getBiome(pPos).is(ModTags.Biomes.tropicalBiomes)) {
            cir.setReturnValue(false);
        }
    }
}