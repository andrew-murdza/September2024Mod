package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalPlayerSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.NetworkConstants;

import java.io.IOException;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalPlayerSpawnTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void newPlayerLoginAndSavedLocationUseTheirCorrectDimensions(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        GameProfile profile = profile("TropicalLogin");
        BlockPos savedPosition = preparePlatform(helper);
        try (TestPlayer session = new TestPlayer(server, profile)) {
            assertTropical(helper, session.player, "A new player must log in directly to tropical");
            helper.assertTrue(GameplayDimensions.TROPICAL.equals(session.connection.loginDimension),
                    "The initial login packet must name tropical, without an Overworld detour");
            session.player.teleportTo(helper.getLevel(), savedPosition.getX() + 0.5D,
                    savedPosition.getY(), savedPosition.getZ() + 0.5D, 37.0F, 0.0F);
        }

        // PlayerList.remove saved actual player NBT; a subsequent normal login must honor it.
        try (TestPlayer restored = new TestPlayer(server, profile)) {
            helper.assertTrue(restored.player.serverLevel() == helper.getLevel(),
                    "Existing players must retain their saved dimension instead of being moved to tropical");
            helper.assertTrue(restored.player.blockPosition().equals(savedPosition),
                    "Existing players must retain their saved position");
            helper.assertTrue(Level.OVERWORLD.equals(restored.connection.loginDimension),
                    "The reconnect packet must use the saved dimension");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void absentAndDestroyedRespawnPointsFallBackToTropical(GameTestHelper helper) {
        try (TestPlayer session = new TestPlayer(helper.getLevel().getServer(), profile("TropicalRespawn"))) {
            helper.assertTrue(session.player.getRespawnPosition() == null,
                    "The fixture must begin without a bed or forced respawn point");
            session.respawn();
            assertTropical(helper, session.player, "Death without a respawn point must return to tropical");
            helper.assertTrue(GameplayDimensions.TROPICAL.equals(session.connection.respawnDimension),
                    "The respawn packet must send the client directly to tropical");

            BlockPos missingBed = preparePlatform(helper);
            session.player.setRespawnPosition(Level.OVERWORLD, missingBed, 0.0F, false, false);
            session.respawn();
            assertTropical(helper, session.player, "A missing or destroyed bed must fall back to tropical");
            helper.assertTrue(GameplayDimensions.TROPICAL.equals(session.connection.respawnDimension),
                    "An invalid bed must not briefly send the client to the Overworld");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void validBedsAndForcedRespawnPointsRemainRespected(GameTestHelper helper) {
        BlockPos forcedPosition = preparePlatform(helper);
        try (TestPlayer session = new TestPlayer(helper.getLevel().getServer(), profile("TropicalBed"))) {
            session.player.setRespawnPosition(Level.OVERWORLD, forcedPosition, 61.0F, true, false);
            session.respawn();
            helper.assertTrue(session.player.serverLevel() == helper.getLevel()
                            && session.player.blockPosition().equals(forcedPosition),
                    "A valid forced spawn must retain its selected dimension and position");

            BlockPos foot = forcedPosition.offset(1, 0, 1);
            BlockPos head = foot.north();
            helper.getLevel().setBlock(foot, Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT), 18);
            helper.getLevel().setBlock(head, Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD), 18);
            session.player.setRespawnPosition(Level.OVERWORLD, head, 0.0F, false, false);
            session.respawn();
            helper.assertTrue(session.player.serverLevel() == helper.getLevel()
                            && session.player.blockPosition().distSqr(head) < 16.0D,
                    "A valid bed must respawn the player beside that bed in its original dimension");
            helper.assertTrue(head.equals(session.player.getRespawnPosition())
                            && !session.player.isRespawnForced(),
                    "Bed respawning must retain the bed instead of replacing it with a forced tropical point");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void tropicalSpawnIsSavedAndUnsafeArrivalIsRepaired(GameTestHelper helper) throws IOException {
        ServerLevel tropical = TropicalPlayerSpawn.level(helper.getLevel().getServer());
        BlockPos original = TropicalPlayerSpawn.safePosition(tropical);
        var oldFeet = tropical.getBlockState(original);
        var obstruction = tropical.getBlockState(original.below());
        BlockPos replacement;
        try {
            helper.assertTrue(tropical.setBlock(original, obstruction, 18)
                            && tropical.getBlockState(original).equals(obstruction),
                    "The tropical-compatible fixture must actually obstruct the saved spawn");
            helper.assertTrue(!TropicalPlayerSpawn.isSafe(tropical, original),
                    "The obstruction must make the old arrival unsafe before recovery is tested");
            replacement = TropicalPlayerSpawn.safePosition(tropical);
            helper.assertTrue(!replacement.equals(original) && TropicalPlayerSpawn.isSafe(tropical, replacement),
                    "A blocked spawn must be replaced with a safe arrival without removing the obstacle");
            helper.assertTrue(tropical.getBlockState(original).equals(obstruction),
                    "Finding a replacement spawn must not destroy existing blocks");
        } finally {
            tropical.setBlock(original, oldFeet, 18);
        }
        tropical.getDataStorage().save();
        var savedFile = tropical.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("dimensions/aoemod/tropical_dimension/data/aoemod_tropical_player_spawn.dat").toFile();
        var saved = NbtIo.readCompressed(savedFile);
        helper.assertTrue(BlockPos.of(saved.getCompound("data").getLong("Position")).equals(replacement),
                "The repaired tropical spawn must be written to the world's persistent saved data");
        helper.assertTrue(tropical.getSharedSpawnPos().equals(replacement),
                "Minecraft's shared-spawn accessor must report the persisted tropical point");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void spawnSafetyRejectsWaterObstructionsAndCanopies(GameTestHelper helper) {
        BlockPos feet = preparePlatform(helper);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(TropicalPlayerSpawn.isSafe(level, feet), "Dry supported clear space must be safe");
        level.setBlock(feet, Blocks.WATER.defaultBlockState(), 18);
        helper.assertTrue(!TropicalPlayerSpawn.isSafe(level, feet), "A player must not start submerged");
        level.setBlock(feet, Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(feet.above(), Blocks.STONE.defaultBlockState(), 18);
        helper.assertTrue(!TropicalPlayerSpawn.isSafe(level, feet), "A player needs clear headroom");
        level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), 18);
        for (var floor : new net.minecraft.world.level.block.Block[]{
                Blocks.OAK_LEAVES, Blocks.OAK_LOG, Blocks.RED_MUSHROOM_BLOCK, Blocks.MAGMA_BLOCK}) {
            level.setBlock(feet.below(), floor.defaultBlockState(), 18);
            helper.assertTrue(!TropicalPlayerSpawn.isSafe(level, feet),
                    "Canopies and hazardous floors must not become player spawn surfaces: " + floor);
        }
        helper.succeed();
    }

    private static BlockPos preparePlatform(GameTestHelper helper) {
        BlockPos feet = helper.absolutePos(new BlockPos(3, 3, 3));
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 3; dz++) {
                helper.getLevel().setBlock(feet.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                for (int dy = 0; dy <= 2; dy++) {
                    helper.getLevel().setBlock(feet.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }
        return feet;
    }

    private static void assertTropical(GameTestHelper helper, ServerPlayer player, String message) {
        helper.assertTrue(player.level().dimension().equals(GameplayDimensions.TROPICAL), message);
        helper.assertTrue(TropicalPlayerSpawn.isSafe(player.serverLevel(), player.blockPosition()),
                "The player must arrive on a dry, supported surface with sufficient headroom");
    }

    private static GameProfile profile(String name) {
        return new GameProfile(UUID.randomUUID(), name);
    }

    private static final class TestPlayer implements AutoCloseable {
        private final PlayerList players;
        private final RecordingConnection connection = new RecordingConnection();
        private ServerPlayer player;

        private TestPlayer(MinecraftServer server, GameProfile profile) {
            players = server.getPlayerList();
            player = players.getPlayerForLogin(profile);
            players.placeNewPlayer(connection, player);
        }

        private void respawn() {
            player = players.respawn(player, false);
        }

        @Override
        public void close() {
            try {
                players.remove(player);
            } finally {
                connection.close();
            }
        }
    }

    /** Exercise normal server login/respawn without opening a socket or running a client. */
    private static final class RecordingConnection extends Connection implements AutoCloseable {
        private final EmbeddedChannel testChannel = new EmbeddedChannel();
        private ResourceKey<Level> loginDimension;
        private ResourceKey<Level> respawnDimension;

        private RecordingConnection() {
            super(PacketFlow.SERVERBOUND);
            testChannel.attr(AttributeKey.<String>valueOf("fml:netversion")).set(NetworkConstants.NETVERSION);
        }

        @Override
        public Channel channel() {
            return testChannel;
        }

        @Override
        public boolean isConnected() {
            return testChannel.isOpen();
        }

        @Override
        public void send(Packet<?> packet) {
            if (packet instanceof ClientboundLoginPacket login) loginDimension = login.dimension();
            if (packet instanceof ClientboundRespawnPacket respawn) respawnDimension = respawn.getDimension();
        }

        @Override
        public void send(Packet<?> packet, PacketSendListener listener) {
            send(packet);
        }

        @Override
        public void close() {
            testChannel.finishAndReleaseAll();
        }
    }
}
