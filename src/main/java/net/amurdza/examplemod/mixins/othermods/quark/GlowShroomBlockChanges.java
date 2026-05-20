package net.amurdza.examplemod.mixins.othermods.quark;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.worldgen.feature.ModFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.violetmoon.quark.content.world.block.GlowShroomBlock;

import java.util.Objects;
import java.util.Optional;

@Mixin(value = GlowShroomBlock.class,remap = false)
public abstract class GlowShroomBlockChanges extends BushBlock {


    public GlowShroomBlockChanges(Properties pProperties) {
        super(pProperties);
    }

    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void aoe$mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos,
                                CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.isFaceSturdy(level, pos, Direction.UP));
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                           @NotNull RandomSource random) {
        aoe$spreadLikeMushroom(state, level, pos, random);
    }

    @Override
    public boolean canSurvive(@NotNull BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (blockstate.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
            return true;
        } else {
            return blockstate.canSustainPlant(pLevel, blockpos,
                    net.minecraft.core.Direction.UP, this);
        }
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void aoe$performBonemeal(ServerLevel level, RandomSource random, BlockPos pos,
                                     BlockState state, CallbackInfo ci) {
        ci.cancel();
        aoe$growMushroom(level, pos, state, random);
    }

    @Unique
    private void aoe$spreadLikeMushroom(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Helper.nextIntCropsGrow(level, pos, state, random, 5) != 0) {
            return;
        }

        int maxMushrooms = Helper.isSpecialBiome(level, pos)
                ? Config.MAX_MUSHROOMS_FOR_GROWTH
                : 5;

        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(nearby).is(state.getBlock())) {
                --maxMushrooms;
                if (maxMushrooms <= 0) {
                    return;
                }
            }
        }

        BlockPos target = pos.offset(
                random.nextInt(3) - 1,
                random.nextInt(2) - random.nextInt(2),
                random.nextInt(3) - 1
        );

        for (int k = 0; k < 4; ++k) {
            if (level.isEmptyBlock(target) && state.canSurvive(level, target)) {
                pos = target;
            }

            target = pos.offset(
                    random.nextInt(3) - 1,
                    random.nextInt(2) - random.nextInt(2),
                    random.nextInt(3) - 1
            );
        }

        if (level.isEmptyBlock(target) && state.canSurvive(level, target)) {
            level.setBlock(target, state, 2);
        }
    }

    @Unique
    private void aoe$growMushroom(ServerLevel level, BlockPos pos, BlockState state,
                                  RandomSource random) {
        Optional<? extends Holder<ConfiguredFeature<?, ?>>> optional =
                level.registryAccess()
                        .registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .getHolder(ModFeatures.HUGE_GLOW_SHROOM);

        if (optional.isEmpty()) {
            return;
        }

        var event = net.minecraftforge.event.ForgeEventFactory.blockGrowFeature(
                level,
                random,
                pos,
                optional.get()
        );

        if (event.getResult().equals(Event.Result.DENY)) {
            return;
        }

        level.removeBlock(pos, false);

        if (Objects.requireNonNull(event.getFeature()).value().place(level, level.getChunkSource().getGenerator(),
                random, pos)) {
            return;
        }

        level.setBlock(pos, state, 3);
    }
}