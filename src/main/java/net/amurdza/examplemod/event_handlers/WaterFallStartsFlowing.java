package net.amurdza.examplemod.event_handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class WaterFallStartsFlowing {

    private WaterFallStartsFlowing() {
    }

//    @SubscribeEvent
//    public static void onChunkLoad(ChunkEvent.Load event) {
//        LevelAccessor levelAccessor = event.getLevel();
//
//        if (!(levelAccessor instanceof ServerLevel level)) {
//            return;
//        }
//
//        ChunkPos chunkPos = event.getChunk().getPos();
//
//        int minX = chunkPos.getMinBlockX();
//        int minZ = chunkPos.getMinBlockZ();
//        int maxX = chunkPos.getMaxBlockX();
//        int maxZ = chunkPos.getMaxBlockZ();
//
//        for (int x = minX; x <= maxX; x++) {
//            for (int z = minZ; z <= maxZ; z++) {
//                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
//
//                for (int y = Math.max(level.getMinBuildHeight(), topY - 180);
//                     y <= Math.min(level.getMaxBuildHeight() - 1, topY + 8);
//                     y++) {
//
//                    BlockPos pos = new BlockPos(x, y, z);
//
//                    if (level.getBlockState(pos).is(Blocks.WATER)) {
//                        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
//                    }
//                }
//            }
//        }
//    }
}