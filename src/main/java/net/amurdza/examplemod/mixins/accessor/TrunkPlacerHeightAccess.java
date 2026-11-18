package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrunkPlacer.class)
public interface TrunkPlacerHeightAccess {
    @Accessor("baseHeight") int aoemod$baseHeight();
    @Accessor("heightRandA") int aoemod$heightRandA();
    @Accessor("heightRandB") int aoemod$heightRandB();
}
