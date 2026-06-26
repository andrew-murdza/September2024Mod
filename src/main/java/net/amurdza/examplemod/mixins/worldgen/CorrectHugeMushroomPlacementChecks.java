package net.amurdza.examplemod.mixins.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.HugeBrownMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHugeMushroomFeature.class)
public abstract class CorrectHugeMushroomPlacementChecks {
    @Shadow
    protected abstract int getTreeRadiusForHeight(
            int pY,
            int pMaxHeight,
            int pFoliageRadius,
            int pCurrentY
    );

    @Redirect(
            method = "isValidPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/AbstractHugeMushroomFeature;getTreeRadiusForHeight(IIII)I"
            )
    )
    private int aoemod$useActualMushroomHeightForRadiusCheck(
            AbstractHugeMushroomFeature instance,
            int pY,
            int wrongMaxHeight,
            int foliageRadius,
            int currentY,
            LevelAccessor level,
            BlockPos pos,
            int maxHeight,
            BlockPos.MutableBlockPos mutablePos,
            HugeMushroomFeatureConfiguration config
    ) {
        return this.getTreeRadiusForHeight(pY, maxHeight, foliageRadius + 1, currentY);
    }

    @Inject(method = "isValidPosition", at = @At("HEAD"), cancellable = true)
    private void aoemod$requireBrownMushroomCanopyClearance(
            LevelAccessor level,
            BlockPos pos,
            int maxHeight,
            BlockPos.MutableBlockPos mutablePos,
            HugeMushroomFeatureConfiguration config,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object) this instanceof HugeBrownMushroomFeature)) {
            return;
        }

        int radius = config.foliageRadius + 1;
        for (int y = Math.max(0, maxHeight - 2); y <= maxHeight; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!level.getBlockState(mutablePos.setWithOffset(pos, dx, y, dz)).isAir()) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }
}
