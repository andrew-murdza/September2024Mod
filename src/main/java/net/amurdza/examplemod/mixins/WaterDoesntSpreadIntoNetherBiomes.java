package net.amurdza.examplemod.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WaterFluid.class)
public abstract class WaterDoesntSpreadIntoNetherBiomes extends FlowingFluid {
    @Override
    protected void spreadTo(LevelAccessor pLevel, @NotNull BlockPos pPos, @NotNull BlockState pBlockState, @NotNull Direction pDirection, @NotNull FluidState pFluidState) {
        final Holder<Biome> biome = pLevel.getBiome(pPos);

        if(biome.is(Tags.Biomes.IS_COLD_NETHER) || biome.is(Tags.Biomes.IS_DENSE_NETHER) || biome.is(Tags.Biomes.IS_DRY_NETHER) || biome.is(Tags.Biomes.IS_HOT_NETHER) || biome.is(Tags.Biomes.IS_SPARSE_NETHER) || biome.is(Tags.Biomes.IS_WET_NETHER)) {
            return;
        }

        super.spreadTo(pLevel, pPos, pBlockState, pDirection, pFluidState);
    }
}
