package net.amurdza.examplemod.mixins.othermods.quark;

import net.amurdza.examplemod.block.BlockHelper;
import net.amurdza.examplemod.worldgen.feature.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
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

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void aoe$performBonemeal(ServerLevel level, RandomSource random, BlockPos pos,
                                     BlockState state, CallbackInfo ci) {
        ci.cancel();
        aoe$growMushroom(level, pos, state, random);
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                           @NotNull RandomSource random) {
        BlockHelper.spreadMushroom(state, level, pos, random, this::aoe$growMushroom);
    }

    @Unique
    private void aoe$growMushroom(ServerLevel level, BlockPos pos, BlockState state,
                                  RandomSource random) {
        Optional<? extends Holder<ConfiguredFeature<?, ?>>> optional =
                level.registryAccess()
                        .registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .getHolder(ModConfiguredFeatures.HUGE_GLOW_SHROOM);

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