package net.amurdza.examplemod.mixins.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.RandomPatchFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomPatchFeature.class)
public abstract class RandomPatchCenteredInMiddleOfChunk extends Feature<RandomPatchConfiguration> {

    public RandomPatchCenteredInMiddleOfChunk(Codec<RandomPatchConfiguration> codec) {
        super(codec);
    }

    /**
     * Vanilla RandomPatchFeature uses the placement origin directly.
     * If the placed feature has no x/z placement modifier like minecraft:in_square,
     * that origin is usually the chunk corner.
     * This shifts the origin by +8, +8 so the random patch is centered around
     * the middle of the chunk instead.
     */
//    @Inject(
//            method = "place",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private void aoemod$placeAroundChunkCenter(
//            FeaturePlaceContext<RandomPatchConfiguration> context,
//            CallbackInfoReturnable<Boolean> cir
//    ) {
//        RandomPatchConfiguration config = context.config();
//        RandomSource random = context.random();
//        WorldGenLevel level = context.level();
//
//        BlockPos shiftedOrigin = context.origin().offset(8, 0, 8);
//
//        int placedCount = 0;
//        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
//
//        int xzRange = config.xzSpread() + 1;
//        int yRange = config.ySpread() + 1;
//
//        for (int attempt = 0; attempt < config.tries(); ++attempt) {
//            mutablePos.setWithOffset(
//                    shiftedOrigin,
//                    random.nextInt(xzRange) - random.nextInt(xzRange),
//                    random.nextInt(yRange) - random.nextInt(yRange),
//                    random.nextInt(xzRange) - random.nextInt(xzRange)
//            );
//
//            if (config.feature().value().place(
//                    level,
//                    context.chunkGenerator(),
//                    random,
//                    mutablePos
//            )) {
//                ++placedCount;
//            }
//        }
//
//        cir.setReturnValue(placedCount > 0);
//    }
}