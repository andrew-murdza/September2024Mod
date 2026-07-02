package net.amurdza.examplemod.block;

import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EyeblossomBlock extends FlowerBlock {
    private final boolean open;

    public EyeblossomBlock(boolean open, Properties properties) {
        super(open ? MobEffects.BLINDNESS : MobEffects.CONFUSION, open ? 11 : 7, properties);
        this.open = open;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tryChangingState(state, level, pos, random);
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tryChangingState(state, level, pos, random);
    }

    private void tryChangingState(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean shouldBeOpen = !level.isDay();
        if (shouldBeOpen == this.open) {
            return;
        }

        BlockState transformed = (shouldBeOpen ? ModBlocks.OPEN_EYEBLOSSOM : ModBlocks.CLOSED_EYEBLOSSOM)
                .get().defaultBlockState();
        level.setBlock(pos, transformed, 3);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F,
                shouldBeOpen ? 1.25F : 0.8F);

        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
            if (level.getBlockState(nearby).is(state.getBlock())) {
                int delay = 5 + random.nextInt(36);
                level.scheduleTick(nearby, state.getBlock(), delay);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (this.open && random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                    0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && level.getDifficulty() != Difficulty.PEACEFUL
                && entity instanceof Bee bee && !bee.hasEffect(MobEffects.POISON)) {
            bee.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 25));
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(state, level, pos);
    }
}
