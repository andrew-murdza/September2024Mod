package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomSpreadFoliagePlacer.class)
public abstract class TropicalMangroveCanopy extends FoliagePlacer {
    protected TropicalMangroveCanopy(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Inject(method = "createFoliage", at = @At("HEAD"), cancellable = true)
    private void aoemod$roundedMangroveCrown(LevelSimulatedReader level, FoliageSetter setter,
            RandomSource random, TreeConfiguration config, int treeHeight, FoliageAttachment attachment,
            int foliageHeight, int foliageRadius, int offset, CallbackInfo ci) {
        if (!(level instanceof LevelReader reader) || !GameplayDimensions.is(reader, GameplayDimensions.TROPICAL)
                || !config.foliageProvider.getState(random, attachment.pos()).is(Blocks.MANGROVE_LEAVES)) return;
        BlockPos center = attachment.pos().above(offset);
        int broadRadius = Math.max(4, foliageRadius);
        for (int y = -2; y <= 2; y++) {
            int radius = broadRadius - Math.abs(y);
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius + 1) {
                    tryPlaceLeaf(level, setter, random, config, center.offset(x, y, z));
                }
            }
        }
        ci.cancel();
    }
}
