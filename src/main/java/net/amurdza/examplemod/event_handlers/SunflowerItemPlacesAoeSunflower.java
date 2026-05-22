package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class SunflowerItemPlacesAoeSunflower {

    @SubscribeEvent
    public static void sunflowerPlace1(BlockEvent.EntityMultiPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        Entity entity = event.getEntity();

        BlockState placedState = event.getPlacedBlock();
        Block placedBlock = placedState.getBlock();

        if (placedBlock != Blocks.SUNFLOWER) {
            return;
        }

        Direction facing = entity == null ? Direction.NORTH : entity.getDirection().getOpposite();

        for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
            BlockPos snapshotPos = snapshot.getPos();
            BlockState currentState = level.getBlockState(snapshotPos);

            DoubleBlockHalf half = currentState.getValue(DoublePlantBlock.HALF);

            BlockState newState = ModBlocks.SUNFLOWER.get().defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, half)
                    .setValue(HorizontalDirectionalBlock.FACING, facing);

            level.setBlock(snapshotPos, newState, 2);
        }
    }

}
