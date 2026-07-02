package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LapisGolemPlacement {
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!event.getPlacedBlock().is(Blocks.LAPIS_BLOCK)) {
            return;
        }

        BlockPos headPos = event.getPos();
        if (matchesGolem(level, headPos, Direction.Axis.X)) {
            spawnGolem(level, headPos, Direction.Axis.X);
        } else if (matchesGolem(level, headPos, Direction.Axis.Z)) {
            spawnGolem(level, headPos, Direction.Axis.Z);
        }
    }

    private static boolean matchesGolem(ServerLevel level, BlockPos headPos, Direction.Axis armAxis) {
        BlockPos body = headPos.below();
        BlockPos feet = headPos.below(2);

        if (!level.getBlockState(body).is(Blocks.IRON_BLOCK) || !level.getBlockState(feet).is(Blocks.IRON_BLOCK)) {
            return false;
        }

        BlockPos armA = armAxis == Direction.Axis.X ? body.west() : body.north();
        BlockPos armB = armAxis == Direction.Axis.X ? body.east() : body.south();
        return level.getBlockState(armA).is(Blocks.IRON_BLOCK) && level.getBlockState(armB).is(Blocks.IRON_BLOCK);
    }

    private static void spawnGolem(ServerLevel level, BlockPos headPos, Direction.Axis armAxis) {
        BlockPos body = headPos.below();
        BlockPos feet = headPos.below(2);
        BlockPos armA = armAxis == Direction.Axis.X ? body.west() : body.north();
        BlockPos armB = armAxis == Direction.Axis.X ? body.east() : body.south();

        level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(body, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(feet, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(armA, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(armB, Blocks.AIR.defaultBlockState(), 2);

        IronGolem golem = EntityType.IRON_GOLEM.create(level);
        if (golem == null) {
            return;
        }

        golem.setPlayerCreated(true);
        golem.moveTo(headPos.getX() + 0.5D, headPos.getY() - 1.95D, headPos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(golem);
    }
}
