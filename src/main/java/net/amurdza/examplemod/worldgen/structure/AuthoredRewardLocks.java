package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.event_handlers.SavannaCampProtection;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class AuthoredRewardLocks {
    private AuthoredRewardLocks() { }

    public static boolean isRewardBlock(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof StemBlock || state.getBlock() instanceof AttachedStemBlock
                || state.is(BlockTags.CROPS) || state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)
                || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)
                || "hearthandharvest:corn_stalk".equals(String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())));
    }

    public static boolean chestLocked(BlockEntity entity) {
        return entity.getLevel() instanceof ServerLevel level && entity.getBlockState().getBlock() instanceof ChestBlock
                && (SavannaCampProtection.rewardsLocked(level, entity.getBlockPos())
                || MansionProtection.rewardsLocked(level, entity.getBlockPos()));
    }

    public static boolean protectedBlock(ServerLevel level, BlockPos pos) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA) && !GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return false;
        // Fire is never authored construction; extinguishing must remain possible.
        if (level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock) return false;
        return GameplayDimensions.is(level, GameplayDimensions.SAVANNA) ? SavannaCampProtection.protectedBlock(level, pos)
                : MansionProtection.protectedAt(level, pos);
    }
}
