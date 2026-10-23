package net.amurdza.examplemod.block;

import net.amurdza.examplemod.worldgen.AzaleaSaplingTreeGrower;
import net.minecraft.world.level.block.SaplingBlock;

public class AzaleaSaplingBlock extends SaplingBlock {
    public AzaleaSaplingBlock(Properties properties) {
        super(new AzaleaSaplingTreeGrower(), properties);
    }

}
