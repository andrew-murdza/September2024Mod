package net.amurdza.examplemod.mixins.trees;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.amurdza.examplemod.worldgen.TropicalTreeShapes;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractHugeMushroomFeature.class)
public abstract class TropicalMushroomHeight {
    @ModifyReturnValue(method = "getTreeHeight", at = @At("RETURN"), require = 1)
    private int aoemod$raiseTropicalMushrooms(int height) {
        return TropicalTreeShapes.tallMushroom() ? height + 1 : height;
    }
}
