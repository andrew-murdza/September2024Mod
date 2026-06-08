package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class ItemsPlaceModBlocks {

    @SubscribeEvent
    public static void sunflowerPlace1(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();

        if(!stack.is(Items.SUNFLOWER)&&stack.is(Items.SWEET_BERRIES)&&stack.is(Items.GLOW_LICHEN)){
            return;
        }

        BlockPlaceContext context = new BlockPlaceContext(
                player,
                event.getHand(),
                stack,
                event.getHitVec()
        );
        BlockPos pos = context.getClickedPos();

        BlockState state;
        if(stack.is(Items.SUNFLOWER)){
            Direction facing = player.getDirection().getOpposite();
            state = ModBlocks.SUNFLOWER.get().defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        else {
            return;
        }

        if (!level.getBlockState(pos).canBeReplaced(context)) {
            return;
        }

        if (stack.is(Items.SUNFLOWER)&&!level.getBlockState(pos.above()).canBeReplaced(context)) {
            return;
        }

        if (!state.canSurvive(level, pos)) {
            return;
        }

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));

        if (level.isClientSide()) {
            return;
        }

        DoublePlantBlock.placeAt(level, state, pos, 3);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}