package net.amurdza.examplemod.mixins.othermods.trailsandtalesplus;

import com.belgieyt.trailsandtalesplus.Objects.blocks.LushroomBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.worldgen.feature.ModFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LushroomBlock.class,remap = false)
public class LushroomBehavior {
    @Redirect(
            method = "canSurvive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean aoe$ignoreLightLevel(BlockState instance, Block block) {
        return instance.is(BlockTags.MUSHROOM_GROW_BLOCK);
    }

    @WrapOperation(
            method = "canSurvive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelReader;getRawBrightness(Lnet/minecraft/core/BlockPos;I)I"
            )
    )
    private int aoe$ignoreLightLevel(LevelReader instance, BlockPos blockPos, int i, Operation<Integer> original) {
        return 0;
    }

    /**
     * Replaces random.nextInt(25) with your custom mushroom growth logic.
     */
    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I",
                    ordinal = 0
            )
    )
    private int aoe$customGrowthChance(RandomSource random, int bound,
                                       BlockState state, ServerLevel level, BlockPos pos, RandomSource sameRandom) {
        return Helper.nextIntCropsGrow(level, pos, state, random, 5);
    }

    /**
     * Replaces the vanilla nearby mushroom cap of 5.
     */
    @ModifyConstant(
            method = "randomTick",
            constant = @Constant(intValue = 5)
    )
    private int aoe$modifyMaxNearbyMushrooms(int original,
                                             BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return Helper.isSpecialBiome(level, pos)
                ? Config.MAX_MUSHROOMS_FOR_GROWTH
                : original;
    }
    @Redirect(method = "growMushroom",at= @At(value = "FIELD", target = "Lcom/belgieyt/trailsandtalesplus/Objects/blocks/LushroomBlock;feature:Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<ConfiguredFeature<?, ?>> hi(LushroomBlock instance){
        return ModFeatures.HUGE_LUSHROOM;
    }
}
