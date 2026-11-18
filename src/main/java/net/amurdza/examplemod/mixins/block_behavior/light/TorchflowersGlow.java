package net.amurdza.examplemod.mixins.block_behavior.light;

import net.amurdza.examplemod.worldgen.TropicalPlantLighting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.spongepowered.asm.mixin.Mixin;

/** Override Forge's contextual hook; constructor light settings stay unchanged outside tropical. */
@Mixin(Block.class)
public abstract class TorchflowersGlow implements IForgeBlock {
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return TropicalPlantLighting.emission(state, level);
    }
}
