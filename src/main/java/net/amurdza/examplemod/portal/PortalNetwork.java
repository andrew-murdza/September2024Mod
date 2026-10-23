package net.amurdza.examplemod.portal;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.client.PortalClient;
import net.amurdza.examplemod.event_handlers.PortalEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class PortalNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            AOEMod.makeID("wooden_portals"), () -> "1", "1"::equals, "1"::equals);

    private PortalNetwork() { }

    public static void register() {
        CHANNEL.registerMessage(0, OpenPicker.class, OpenPicker::encode, OpenPicker::decode,
                OpenPicker::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, SelectDestination.class, SelectDestination::encode, SelectDestination::decode,
                SelectDestination::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void open(ServerPlayer player, UUID token, int destinations) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenPicker(token, destinations));
    }

    public static void select(UUID token, PortalDestination destination) {
        CHANNEL.sendToServer(new SelectDestination(token, destination.ordinal()));
    }

    private record OpenPicker(UUID token, int destinations) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(token);
            buffer.writeVarInt(destinations);
        }

        private static OpenPicker decode(FriendlyByteBuf buffer) {
            return new OpenPicker(buffer.readUUID(), buffer.readVarInt());
        }

        private static void handle(OpenPicker packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> PortalClient.open(packet.token, packet.destinations)));
            context.setPacketHandled(true);
        }
    }

    private record SelectDestination(UUID token, int destination) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(token);
            buffer.writeVarInt(destination);
        }

        private static SelectDestination decode(FriendlyByteBuf buffer) {
            return new SelectDestination(buffer.readUUID(), buffer.readVarInt());
        }

        private static void handle(SelectDestination packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                context.enqueueWork(() -> {
                    if (packet.destination >= 0 && packet.destination < PortalDestination.values().length) {
                        PortalEvents.select(sender, packet.token, PortalDestination.values()[packet.destination]);
                    }
                });
            }
            context.setPacketHandled(true);
        }
    }
}
