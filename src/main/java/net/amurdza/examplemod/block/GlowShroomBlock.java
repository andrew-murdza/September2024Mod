package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.NotNull;

public class GlowShroomBlock extends MushroomBlock {
    public GlowShroomBlock(BlockBehaviour.Properties pProperties, ResourceKey<ConfiguredFeature<?, ?>> pFeature) {
        super(pProperties,pFeature);
    }
    public void animateTick(@NotNull BlockState stateIn, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (rand.nextInt(12) == 0 && worldIn.getBlockState(pos.above()).isAir()) {
            worldIn.addParticle(ParticleTypes.END_ROD, (double)pos.getX() + 0.4 + rand.nextDouble() * 0.2, (double)pos.getY() + 0.5 + rand.nextDouble() * 0.1, (double)pos.getZ() + 0.4 + rand.nextDouble() * 0.2, (Math.random() - 0.5) * 0.04, (1.0 + Math.random()) * 0.02, (Math.random() - 0.5) * 0.04);
        }
    }
}
