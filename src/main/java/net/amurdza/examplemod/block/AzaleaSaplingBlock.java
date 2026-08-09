package net.amurdza.examplemod.block;

import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.worldgen.AzaleaSaplingTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AzaleaSaplingBlock extends SaplingBlock {
    public AzaleaSaplingBlock(Properties properties) {
        super(new AzaleaSaplingTreeGrower(), properties);
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!level.getBiome(pos).is(ModTags.Biomes.aoeJungleBiomes)) {
            return;
        }

        super.advanceTree(level, pos, state, random);
    }
}
