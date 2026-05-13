package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.world.gen.WorldGenPixieVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.registry.ModBlocks;

@Mixin(value = WorldGenPixieVillage.class, remap = false)
public class PixieGroundTerrain {
    @Redirect(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private boolean aoemod$replacePixieVillageGroundBlocks(
            WorldGenLevel level,
            BlockPos pos,
            BlockState state,
            int flags
    ) {
        if (state.is(Blocks.COARSE_DIRT)) {
            return level.setBlock(pos, Blocks.MOSS_BLOCK.defaultBlockState(), flags);
        }

        if (state.is(Blocks.DIRT_PATH) || state.is(Blocks.SPRUCE_PLANKS)) {
            return level.setBlock(pos, ModBlocks.RICH_SOIL.get().defaultBlockState(), flags);
        }

        return level.setBlock(pos, state, flags);
    }
}
