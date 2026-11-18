package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import io.netty.buffer.Unpooled;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Explicit QA only: legacy save -> quit -> real new-world UI -> quit -> rejoin -> quit. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientFreshWorldSmokeAudit {
    private static final String OUTPUT = System.getProperty("aoemod.freshWorldAudit.report");
    private static final String PRIOR_WORLD = System.getProperty("aoemod.freshWorldAudit.priorWorld", "");
    private static final String WORLD_NAME = System.getProperty("aoemod.freshWorldAudit.worldName", "FreshWorldSmoke");
    private static final String SEED = System.getProperty("aoemod.freshWorldAudit.seed", "5481635986579024772");
    private static final long TIMEOUT_MILLIS = Math.max(60, Integer.getInteger("aoemod.freshWorldAudit.timeoutSeconds", 900)) * 1000L;
    private static final List<Map<String, Object>> CHECKS = new ArrayList<>();
    private static final Map<BlockState, Integer> FROZEN_STATE_IDS = new IdentityHashMap<>();
    private static final Map<String, Integer> FROZEN_CHARNIA_IDS = new LinkedHashMap<>();
    private enum Phase { TITLE, PRIOR_JOIN, PRIOR_QUIT, CREATE_SCREEN, NEW_JOIN, NEW_QUIT, REJOIN, FINAL_QUIT }
    private static Phase phase = Phase.TITLE;
    private static long started;
    private static boolean completed;
    private static int settledTicks;
    private static boolean spectatorRequested;
    private static String newWorldFolder;
    private static CompletableFuture<Map<String, Object>> packetCheck;

    private ClientFreshWorldSmokeAudit() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (OUTPUT == null || OUTPUT.isBlank() || completed || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (started == 0) {
                started = System.currentTimeMillis();
                requireIsolatedDirectory(minecraft);
                minecraft.options.pauseOnLostFocus = false;
                writeReport("running", null);
            }
            if (System.currentTimeMillis() - started > TIMEOUT_MILLIS)
                throw new IllegalStateException("Fresh-world audit timed out at " + phase + ", screen=" + screenName(minecraft));
            if (minecraft.getOverlay() != null) return;
            if (phase == Phase.TITLE && minecraft.screen instanceof AccessibilityOnboardingScreen) {
                press(minecraft.screen, "gui.continue");
                return;
            }
            if (acceptKnownWorldWarning(minecraft)) return;
            switch (phase) {
                case TITLE -> {
                    if (!(minecraft.screen instanceof TitleScreen)) return;
                    checkRegistry("initial title");
                    if (PRIOR_WORLD.isBlank()) openCreateScreen(minecraft);
                    else {
                        requireWorldExists(minecraft, PRIOR_WORLD);
                        transition(Phase.PRIOR_JOIN);
                        minecraft.createWorldOpenFlows().loadLevel(new TitleScreen(), PRIOR_WORLD);
                    }
                }
                case PRIOR_JOIN, NEW_JOIN, REJOIN -> {
                    if (phase == Phase.PRIOR_JOIN && !spectatorRequested && minecraft.player != null
                            && minecraft.getSingleplayerServer() != null) {
                        spectatorRequested = true;
                        var server = minecraft.getSingleplayerServer();
                        var playerId = minecraft.player.getUUID();
                        server.execute(() -> {
                            var player = server.getPlayerList().getPlayer(playerId);
                            if (player != null) {
                                player.setGameMode(GameType.SPECTATOR);
                                player.setInvulnerable(true);
                            }
                        });
                    }
                    if (!joinedAndSettled(minecraft)) return;
                    if (packetCheck == null) {
                        checkRegistry("joined " + phase);
                        packetCheck = serializeLoadedChunk(minecraft);
                        return;
                    }
                    if (!packetCheck.isDone()) return;
                    CHECKS.add(packetCheck.join());
                    Phase next = phase == Phase.PRIOR_JOIN ? Phase.PRIOR_QUIT
                            : phase == Phase.NEW_JOIN ? Phase.NEW_QUIT : Phase.FINAL_QUIT;
                    transition(next);
                    // Press the real pause-menu button: this includes Forge's registry teardown.
                    minecraft.setScreen(new PauseScreen(true));
                    press(minecraft.screen, "menu.returnToMenu");
                }
                case PRIOR_QUIT, NEW_QUIT, FINAL_QUIT -> {
                    if (!(minecraft.screen instanceof TitleScreen) || minecraft.getSingleplayerServer() != null
                            || minecraft.level != null) return;
                    checkRegistry("after " + phase);
                    if (phase == Phase.PRIOR_QUIT) openCreateScreen(minecraft);
                    else if (phase == Phase.NEW_QUIT) {
                        requireWorldExists(minecraft, newWorldFolder);
                        transition(Phase.REJOIN);
                        minecraft.createWorldOpenFlows().loadLevel(new TitleScreen(), newWorldFolder);
                    } else finish(minecraft, null);
                }
                case CREATE_SCREEN -> {
                    if (!(minecraft.screen instanceof CreateWorldScreen create)) return;
                    var ui = create.getUiState();
                    ui.setName(WORLD_NAME);
                    ui.setSeed(SEED);
                    ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    ui.setAllowCheats(true);
                    newWorldFolder = ui.getTargetFolder();
                    if (Files.exists(minecraft.getLevelSource().getBaseDir().resolve(newWorldFolder)))
                        throw new IllegalStateException("Expected a genuinely new world folder: " + newWorldFolder);
                    CHECKS.add(Map.of("step", "new world screen", "screen", create.getClass().getName(),
                            "worldFolder", newWorldFolder, "seed", SEED));
                    transition(Phase.NEW_JOIN);
                    press(create, "selectWorld.create");
                }
            }
        } catch (Throwable failure) {
            AOEMod.LOGGER.error("Fresh-world smoke audit failed at {}", phase, failure);
            finish(minecraft, failure);
        }
    }

    private static void openCreateScreen(Minecraft minecraft) throws Exception {
        transition(Phase.CREATE_SCREEN);
        // This is the same path as the world's Create New World button. It constructs
        // fresh registries through WorldLoader; reopening a copied level does not.
        CreateWorldScreen.openFresh(minecraft, new TitleScreen());
    }

    private static boolean joinedAndSettled(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null
                || minecraft.screen != null) {
            settledTicks = 0;
            return false;
        }
        int cx = minecraft.player.blockPosition().getX() >> 4, cz = minecraft.player.blockPosition().getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            if (minecraft.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) == null) {
                settledTicks = 0;
                return false;
            }
        }
        return ++settledTicks >= 100;
    }

    private static CompletableFuture<Map<String, Object>> serializeLoadedChunk(Minecraft minecraft) {
        var result = new CompletableFuture<Map<String, Object>>();
        var server = minecraft.getSingleplayerServer();
        var playerId = minecraft.player.getUUID();
        String currentPhase = phase.name();
        server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Integrated-server player disappeared during packet audit");
                var chunk = player.serverLevel().getChunkAt(player.blockPosition());
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    // Uses the same palette-writing constructor implicated in the crash,
                    // including Immersive Portals' registry-ID validation mixin.
                    new ClientboundLevelChunkPacketData(chunk).write(buffer);
                    int bytes = buffer.readableBytes();
                    new ClientboundLevelChunkPacketData(buffer, chunk.getPos().x, chunk.getPos().z);
                    if (bytes == 0 || buffer.isReadable()) throw new IllegalStateException("Incomplete chunk packet round trip");
                    result.complete(Map.of("step", currentPhase + " joined and serialized", "dimension",
                            player.level().dimension().location().toString(), "chunkX", chunk.getPos().x,
                            "chunkZ", chunk.getPos().z, "packetBytes", bytes, "clientChunksReceived", 25,
                            "settledClientTicks", 100));
                } finally { buffer.release(); }
            } catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        return result;
    }

    private static void checkRegistry(String step) {
        int states = 0;
        boolean initial = step.equals("initial title"), restored = step.startsWith("after ");
        for (Block block : BuiltInRegistries.BLOCK) for (var state : block.getStateDefinition().getPossibleStates()) {
            int id = Block.BLOCK_STATE_REGISTRY.getId(state);
            if (id < 0 || Block.BLOCK_STATE_REGISTRY.byId(id) != state)
                throw new IllegalStateException("Invalid state ID at " + step + ": " + state + " -> " + id);
            if (initial) FROZEN_STATE_IDS.put(state, id);
            else if (restored && !Integer.valueOf(id).equals(FROZEN_STATE_IDS.get(state)))
                throw new IllegalStateException("State ID did not return to its frozen value at " + step + ": " + state);
            states++;
        }
        var cyanId = new ResourceLocation("betterend:charnia_cyan");
        var blueId = new ResourceLocation("betterend:charnia_light_blue");
        var cyanBlock = BuiltInRegistries.BLOCK.get(cyanId);
        var blueBlock = BuiltInRegistries.BLOCK.get(blueId);
        var cyanItem = BuiltInRegistries.ITEM.get(cyanId);
        var blueItem = BuiltInRegistries.ITEM.get(blueId);
        if (cyanBlock == blueBlock || cyanItem == blueItem
                || !cyanId.equals(BuiltInRegistries.BLOCK.getKey(cyanBlock))
                || !blueId.equals(BuiltInRegistries.BLOCK.getKey(blueBlock))
                || !cyanId.equals(BuiltInRegistries.ITEM.getKey(cyanItem))
                || !blueId.equals(BuiltInRegistries.ITEM.getKey(blueItem)))
            throw new IllegalStateException("Cyan and light-blue Charnia must remain distinct real block/item entries at " + step);
        Map<String, Integer> charniaIds = Map.of("cyanBlock", BuiltInRegistries.BLOCK.getId(cyanBlock),
                "lightBlueBlock", BuiltInRegistries.BLOCK.getId(blueBlock),
                "cyanItem", BuiltInRegistries.ITEM.getId(cyanItem), "lightBlueItem", BuiltInRegistries.ITEM.getId(blueItem));
        if (BuiltInRegistries.BLOCK.byId(charniaIds.get("cyanBlock")) != cyanBlock
                || BuiltInRegistries.BLOCK.byId(charniaIds.get("lightBlueBlock")) != blueBlock
                || BuiltInRegistries.ITEM.byId(charniaIds.get("cyanItem")) != cyanItem
                || BuiltInRegistries.ITEM.byId(charniaIds.get("lightBlueItem")) != blueItem)
            throw new IllegalStateException("Charnia registry IDs did not round trip at " + step);
        if (initial) FROZEN_CHARNIA_IDS.putAll(charniaIds);
        else if (restored && !FROZEN_CHARNIA_IDS.equals(charniaIds))
            throw new IllegalStateException("Charnia IDs did not return to their frozen values at " + step);
        CHECKS.add(Map.of("step", step, "registeredBlockStateRoundTrips", states,
                "charniaIds", charniaIds, "frozenIdsRestored", restored));
    }

    private static boolean acceptKnownWorldWarning(Minecraft minecraft) {
        if (!(minecraft.screen instanceof ConfirmScreen screen)) return false;
        String title = screen.getTitle().getString();
        boolean creation = title.equals(Component.translatable("selectWorld.warning.experimental.title").getString())
                || title.equals(Component.translatable("selectWorld.warning.deprecated.title").getString());
        boolean reopen = title.equals(Component.translatable("selectWorld.backupQuestion.experimental").getString());
        if (phase == Phase.NEW_JOIN && creation) { press(screen, "gui.yes"); return true; }
        if ((phase == Phase.PRIOR_JOIN || phase == Phase.REJOIN) && reopen) {
            press(screen, "gui.proceed");
            return true;
        }
        return false;
    }

    private static void press(Screen screen, String label) {
        String text = Component.translatable(label).getString();
        for (var child : screen.children()) {
            if (child instanceof Button button && button.active && button.visible && button.getMessage().getString().equals(text)) {
                button.onPress();
                return;
            }
        }
        throw new IllegalStateException("Missing native UI button " + label + " on " + screen.getClass().getSimpleName());
    }

    private static void requireWorldExists(Minecraft minecraft, String folder) {
        Path base = minecraft.getLevelSource().getBaseDir().toAbsolutePath().normalize();
        Path world = base.resolve(folder).normalize();
        if (!world.startsWith(base) || world.equals(base) || !Files.isRegularFile(world.resolve("level.dat")))
            throw new IllegalStateException("Missing isolated world " + folder);
    }

    private static void requireIsolatedDirectory(Minecraft minecraft) {
        Path game = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
        for (Path parent = game.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getFileName() != null && parent.getFileName().toString().equals("build")) return;
        }
        throw new IllegalStateException("Fresh-world audit must run in a disposable directory under build: " + game);
    }

    private static void transition(Phase next) throws Exception {
        phase = next;
        settledTicks = 0;
        spectatorRequested = false;
        packetCheck = null;
        AOEMod.LOGGER.info("Fresh-world smoke audit entering {}", phase);
        writeReport("running", null);
    }

    private static String screenName(Minecraft minecraft) {
        return minecraft.screen == null ? "none" : minecraft.screen.getClass().getName();
    }

    private static void writeReport(String status, Throwable failure) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", status);
        report.put("phase", phase.name());
        report.put("started", Instant.ofEpochMilli(started).toString());
        report.put("elapsedMillis", System.currentTimeMillis() - started);
        report.put("priorWorld", PRIOR_WORLD);
        report.put("newWorldFolder", newWorldFolder);
        report.put("checks", CHECKS);
        report.put("screen", screenName(Minecraft.getInstance()));
        if (failure != null) report.put("failure", failure.toString());
        Path path = Path.of(OUTPUT).toAbsolutePath();
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }

    private static void finish(Minecraft minecraft, Throwable failure) {
        completed = true;
        try { writeReport(failure == null ? "passed" : "failed", failure); }
        catch (Exception error) { AOEMod.LOGGER.error("Cannot write fresh-world smoke report", error); }
        if (Boolean.parseBoolean(System.getProperty("aoemod.freshWorldAudit.exit", "true"))) minecraft.stop();
    }
}
