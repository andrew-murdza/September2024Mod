package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.portal.PortalDestination;
import net.amurdza.examplemod.portal.PortalFrame;
import net.amurdza.examplemod.portal.PortalNetwork;
import net.amurdza.examplemod.portal.PortalTravel;
import net.amurdza.examplemod.portal.WoodenPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class PortalEvents {
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private PortalEvents() { }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()
                || !event.getItemStack().is(Items.STICK)) {
            return;
        }
        var state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(BlockTags.PLANKS) && !(state.getBlock() instanceof WoodenPortalBlock)) {
            return;
        }
        if (event.getLevel().isClientSide) {
            if (PortalFrame.touching(event.getLevel(), event.getPos()).isPresent()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            var frame = PortalFrame.touching(player.serverLevel(), event.getPos());
            if (frame.isPresent()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (!canEdit(player, frame.get())) {
                    player.displayClientMessage(Component.translatable("portal.aoemod.cannot_edit"), true);
                    return;
                }
                int mask = PortalDestination.allowedFrom(player.level().dimension()).stream()
                        .filter(destination -> player.server.getLevel(destination.dimension()) != null)
                        .mapToInt(destination -> 1 << destination.ordinal()).reduce(0, (a, b) -> a | b);
                if (mask == 0) {
                    player.displayClientMessage(Component.translatable("portal.aoemod.no_routes"), true);
                    return;
                }
                UUID token = UUID.randomUUID();
                PENDING.put(player.getUUID(), new Pending(token, player.level().dimension(),
                        event.getPos().immutable(), frame.get(), player.server.overworld().getGameTime() + 600));
                PortalNetwork.open(player, token, mask);
            } else {
                player.displayClientMessage(Component.translatable("portal.aoemod.invalid_frame"), true);
            }
        }
    }

    public static boolean canEdit(ServerPlayer player, PortalFrame frame) {
        if (!player.mayBuild() || player.isSpectator()) {
            return false;
        }
        for (BlockPos pos : frame.interior()) {
            if (!player.level().mayInteract(player, pos)) {
                return false;
            }
        }
        for (BlockPos pos : frame.border()) {
            if (!player.level().mayInteract(player, pos)) {
                return false;
            }
        }
        return true;
    }

    public static void select(ServerPlayer player, UUID token, PortalDestination destination) {
        Pending pending = PENDING.get(player.getUUID());
        // A forged/replayed packet cannot consume or activate another pending request.
        if (pending == null || !pending.token.equals(token)) {
            return;
        }
        PENDING.remove(player.getUUID());
        if (pending.expires < player.server.overworld().getGameTime()
                || !pending.dimension.equals(player.level().dimension())
                || player.distanceToSqr(pending.clicked.getCenter()) > 64
                || !player.getMainHandItem().is(Items.STICK)
                || !destination.allowedFromSource(player.level().dimension())
                || player.server.getLevel(destination.dimension()) == null
                || !pending.frame.valid(player.level()) || !canEdit(player, pending.frame)) {
            player.displayClientMessage(Component.translatable("portal.aoemod.selection_expired"), true);
            return;
        }
        if (!pending.frame.activate(player.serverLevel(), destination)) {
            player.displayClientMessage(Component.translatable("portal.aoemod.cannot_edit"), true);
            return;
        }
        player.displayClientMessage(Component.translatable("portal.aoemod.opened",
                Component.translatable(destination.translationKey())), true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
        PortalTravel.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        PENDING.clear();
        PortalTravel.clear();
    }

    private record Pending(UUID token, ResourceKey<Level> dimension, BlockPos clicked,
            PortalFrame frame, long expires) { }
}
