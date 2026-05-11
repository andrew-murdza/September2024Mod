package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.blocks.natural.SnowcapPinheadBlock;
import com.legacy.blue_skies.registries.SkiesConfiguredFeatures;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(SnowcapPinheadBlock.class)
public class SnowcapPinheadChanges {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void aoe$mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos,
                                CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.isFaceSturdy(level, pos, Direction.UP));
    }

    @Intrinsic(displace = true)
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        aoe$spreadLikeMushroom(state, level, pos, random);
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
                        .getHolder(SkiesConfiguredFeatures.GIANT_SNOWCAP_MUSHROOM);

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

        if (event.getFeature().value().place(level, level.getChunkSource().getGenerator(), random, pos)) {
            return;
        }

        level.setBlock(pos, state, 3);
    }
}
