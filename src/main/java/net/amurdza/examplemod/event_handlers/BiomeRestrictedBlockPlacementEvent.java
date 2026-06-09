package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BiomeRestrictedBlockPlacementEvent {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void restrictPlayerBlockPlacement(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockHitResult hit = event.getHitVec();

        BlockPlaceContext context = new BlockPlaceContext(player, event.getHand(), stack, hit);
        BlockPos placePos = context.getClickedPos();

        Optional<Component> failureMessage =
                BiomeRestrictedBlockPlacementHelper.getFailureMessage(level, placePos, blockItem.getBlock());

        if (failureMessage.isEmpty()) {
            return;
        }

        // Cancel on BOTH SIDES.
        // This prevents the client-side fake placement / flash.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);

        // But only send the message from the server.
        if (!level.isClientSide) {
            player.displayClientMessage(failureMessage.get(), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void restrictFallbackPlacement(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        Optional<Component> failureMessage =
                BiomeRestrictedBlockPlacementHelper.getFailureMessage(
                        level,
                        event.getPos(),
                        event.getPlacedBlock().getBlock()
                );

        if (failureMessage.isEmpty()) {
            return;
        }

        event.setCanceled(true);

        if (!level.isClientSide && event.getEntity() instanceof Player player) {
            player.displayClientMessage(failureMessage.get(), true);
        }
    }
}