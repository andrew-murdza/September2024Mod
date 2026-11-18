package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** Send only the island layout seed so client biome lookup uses the server's exact contour. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class TropicalBiomeSync {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            AOEMod.makeID("tropical_biomes"), () -> "1", "1"::equals, "1"::equals);
    private static volatile TropicalLayout clientLayout;

    private TropicalBiomeSync() { }

    public static void register() {
        CHANNEL.registerMessage(0, LayoutSeed.class, LayoutSeed::encode, LayoutSeed::decode,
                LayoutSeed::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static TropicalLayout clientLayout() { return clientLayout; }
    public static void clearClientLayout() { clientLayout = null; }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var tropical = player.server.getLevel(GameplayDimensions.TROPICAL);
        if (tropical != null) {
            long seed = DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature()).seed();
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LayoutSeed(seed));
        }
    }

    public record LayoutSeed(long seed) {
        public void encode(FriendlyByteBuf buffer) { buffer.writeLong(seed); }
        public static LayoutSeed decode(FriendlyByteBuf buffer) { return new LayoutSeed(buffer.readLong()); }
        private static void handle(LayoutSeed packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> clientLayout = new TropicalLayout(packet.seed));
            context.setPacketHandled(true);
        }
    }
}
