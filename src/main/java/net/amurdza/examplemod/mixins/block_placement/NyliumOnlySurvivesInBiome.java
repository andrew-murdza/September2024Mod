package net.amurdza.examplemod.mixins.block_placement;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NyliumBlock.class)
public class NyliumOnlySurvivesInBiome {
    @Redirect(method = "canBeNylium",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/LightEngine;getLightBlockInto(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;I)I"))
    private static int stayAlive(BlockGetter world, BlockState state1, BlockPos pos1, BlockState state2, BlockPos pos2, Direction direction,
                                 int blockLight, BlockState pState, LevelReader pReader, BlockPos pPos) {
        Holder<Biome> biome = pReader.getBiome(pPos);
        if(biome.is(ModTags.Biomes.warpedForestBiomes)&&pState.is(Blocks.WARPED_NYLIUM)
                ||biome.is(ModTags.Biomes.crimsonForestBiomes)&&pState.is(Blocks.CRIMSON_NYLIUM)){
            return LightEngine.getLightBlockInto(world, state1, pos1, state2, pos2, direction, blockLight);
        }
        return 1000;
    }
}
