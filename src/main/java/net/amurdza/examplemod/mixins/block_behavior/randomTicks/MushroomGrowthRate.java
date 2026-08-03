package net.amurdza.examplemod.mixins.block_behavior.randomTicks;

import net.amurdza.examplemod.block.BlockHelper;
import net.amurdza.examplemod.worldgen.MushroomCaveRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MushroomBlock.class)
public abstract class MushroomGrowthRate {
    @Shadow public abstract boolean growMushroom(ServerLevel pLevel, BlockPos pPos, BlockState pState, RandomSource pRandom);

    /**
     * Adds a chance for dense mushrooms on MUSHROOM_GROW_BLOCK floors to grow into
     * their large mushroom feature instead of only failing the spread attempt.
     * @author amurdza
     * @reason simplest way to do what I want
     */
    @Overwrite
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockHelper.spreadMushroom(state, level, pos, random, this::growMushroom);
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void aoe$onlyGrowGiantMushroomsInMushroomCaves(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        BlockPos floorPos = pos.below();
        if (!level.getBlockState(floorPos).is(Blocks.MYCELIUM)
                || !MushroomCaveRegion.containsFloor(floorPos)) {
            ci.cancel();
        }
    }
}
