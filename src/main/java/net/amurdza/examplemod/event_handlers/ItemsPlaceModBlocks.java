package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class ItemsPlaceModBlocks {
    private static final ResourceLocation FRUITS_DELIGHT_BLUEBERRY = new ResourceLocation("fruitsdelight", "blueberry");

    @SubscribeEvent
    public static void sunflowerPlace1(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();

        if(!stack.is(Items.SUNFLOWER) && !isFruitsDelightBlueberry(stack)){
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
        else if (isFruitsDelightBlueberry(stack)) {
            state = ModBlocks.BLUE_BERRY_BUSH.get().defaultBlockState();
        }
        else {
            return;
        }

        if (!level.isInWorldBounds(pos) || stack.is(Items.SUNFLOWER) && !level.isInWorldBounds(pos.above())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
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

        var restriction = BiomePlacementRestrictions.placementFailureMessage(level, pos, state);
        if (restriction.isEmpty() && stack.is(Items.SUNFLOWER)) {
            restriction = BiomePlacementRestrictions.placementFailureMessage(level, pos.above(), state);
        }
        if (restriction.isPresent()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (!level.isClientSide()) player.displayClientMessage(restriction.get(), true);
            return;
        }

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));

        if (level.isClientSide()) {
            return;
        }

        boolean placed;
        if (stack.is(Items.SUNFLOWER)) {
            BlockState previousLower = level.getBlockState(pos);
            BlockState previousUpper = level.getBlockState(pos.above());
            DoublePlantBlock.placeAt(level, state, pos, 3);
            placed = level.getBlockState(pos).equals(state)
                    && level.getBlockState(pos.above()).equals(state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
            if (!placed) {
                level.setBlock(pos, previousLower, 3);
                level.setBlock(pos.above(), previousUpper, 3);
            }
        } else {
            placed = level.setBlock(pos, state, 3);
        }
        if (!placed) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static boolean isFruitsDelightBlueberry(ItemStack stack) {
        Item item = ForgeRegistries.ITEMS.getValue(FRUITS_DELIGHT_BLUEBERRY);
        return item != null && stack.is(item);
    }
}
