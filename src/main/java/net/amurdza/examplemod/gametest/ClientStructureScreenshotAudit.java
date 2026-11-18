package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.TropicalBiomeSync;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit QA-only waypoint capture. No normal client settings or world data are touched. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientStructureScreenshotAudit {
    private static final String VIEWS = System.getProperty("aoemod.structureAudit.views");
    private static final int SETTLE_TICKS = 120;
    // Larger estate reviews can request a wider ready area than the default 52-block targets.
    // This checks only already-requested client chunks; it never loads extra ones.
    private static final int READY_CHUNK_RADIUS = Mth.clamp(Integer.getInteger("aoemod.structureAudit.readyRadius", 4), 4, 10);
    private static final long STARTUP_TIMEOUT = 180_000_000_000L;
    private static final long VIEW_TIMEOUT = 240_000_000_000L;
    private static final long TOTAL_TIMEOUT = 900_000_000_000L;
    private static final List<View> views = new ArrayList<>();
    private static final List<Map<String, Object>> captures = new ArrayList<>();
    private static Path reportPath;
    private static Path screenshotDirectory;
    private static Instant started;
    private static long startNanos, phaseNanos;
    private static boolean initialized, requested;
    private static volatile boolean completed;
    private static int index, settledTicks;
    private static volatile int serverReadyIndex = -1;
    private static volatile String serverFailure;
    private static String terrainWait = "not yet checked";

    private record View(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch, int fov,
                        String label, com.google.gson.JsonArray biomeProbes) { }

    private ClientStructureScreenshotAudit() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (VIEWS == null || VIEWS.isBlank() || completed || event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        try {
            if (!initialized) initialize(minecraft);
            long now = System.nanoTime();
            if (now - startNanos > TOTAL_TIMEOUT) throw new IllegalStateException("Overall screenshot audit timeout");
            if (serverFailure != null) throw new IllegalStateException(serverFailure);
            if (minecraft.getSingleplayerServer() == null || minecraft.player == null || minecraft.level == null) {
                if (now - phaseNanos > (requested ? VIEW_TIMEOUT : STARTUP_TIMEOUT))
                    throw new IllegalStateException("Timed out waiting for the integrated-server world/player");
                return;
            }
            View view = views.get(index);
            if (!requested) {
                requestTeleport(minecraft, view);
                return;
            }
            if (now - phaseNanos > VIEW_TIMEOUT) throw new IllegalStateException("Timed out settling view " + view.label()
                    + ": " + terrainWait);
            boolean acknowledged = serverReadyIndex == index && minecraft.level.dimension().equals(view.dimension())
                    && minecraft.player.hasEffect(MobEffects.NIGHT_VISION)
                    && minecraft.player.distanceToSqr(view.x(), view.y(), view.z()) < .04
                    && Math.abs(Mth.wrapDegrees(minecraft.player.getYRot() - view.yaw())) < 1.5F
                    && Math.abs(minecraft.player.getXRot() - view.pitch()) < 1.5F;
            if (!acknowledged || minecraft.screen != null || minecraft.getOverlay() != null
                    || !terrainReady(minecraft)) {
                settledTicks = 0;
                return;
            }
            if (++settledTicks < SETTLE_TICKS) return;
            capture(minecraft, view);
            index++;
            if (index == views.size()) finish(minecraft, null);
            else {
                requested = false;
                settledTicks = 0;
                phaseNanos = System.nanoTime();
                writeReport("running", null);
            }
        } catch (Throwable failure) {
            AOEMod.LOGGER.error("Structure screenshot audit failed at view {}", index, failure);
            finish(minecraft, failure.toString());
        }
    }

    private static boolean terrainReady(Minecraft minecraft) {
        int centerX = minecraft.player.blockPosition().getX() >> 4;
        int centerZ = minecraft.player.blockPosition().getZ() >> 4;
        int missing = 0;
        for (int x = centerX - READY_CHUNK_RADIUS; x <= centerX + READY_CHUNK_RADIUS; x++) {
            for (int z = centerZ - READY_CHUNK_RADIUS; z <= centerZ + READY_CHUNK_RADIUS; z++) {
                // ClientLevel.hasChunk/hasChunkAt always return true. The cache's
                // nullable lookup is what proves that terrain packets arrived.
                if (minecraft.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) == null) missing++;
            }
        }
        if (missing > 0) {
            terrainWait = missing + " surrounding terrain chunks have not arrived";
            return false;
        }
        // Embeddium implements this native API using its own build queue.
        // Requiring the whole settle interval also covers deferred mesh uploads.
        if (!minecraft.levelRenderer.hasRenderedAllChunks()) {
            terrainWait = "terrain mesh compilation is still pending";
            return false;
        }
        terrainWait = "surrounding chunks arrived and terrain build queue is idle";
        return true;
    }

    private static void initialize(Minecraft minecraft) throws Exception {
        initialized = true;
        started = Instant.now();
        startNanos = phaseNanos = System.nanoTime();
        Path input = Path.of(VIEWS).toAbsolutePath().normalize();
        reportPath = Path.of(System.getProperty("aoemod.structureAudit.report",
                input.resolveSibling("structure-screenshot-report.json").toString())).toAbsolutePath().normalize();
        screenshotDirectory = reportPath.getParent().resolve("screenshots");
        Files.createDirectories(screenshotDirectory);
        var json = JsonParser.parseString(Files.readString(input)).getAsJsonArray();
        if (json.size() == 0 || json.size() > 64) throw new IllegalArgumentException("Expected 1–64 screenshot viewpoints");
        var labels = new HashSet<String>();
        for (var element : json) {
            var object = element.getAsJsonObject();
            String label = object.get("label").getAsString();
            if (!label.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,95}") || !labels.add(label))
                throw new IllegalArgumentException("View labels must be unique safe filenames: " + label);
            double x = object.get("x").getAsDouble(), y = object.get("y").getAsDouble(), z = object.get("z").getAsDouble();
            float yaw = object.get("yaw").getAsFloat(), pitch = object.get("pitch").getAsFloat();
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Invalid camera coordinates/angles for " + label);
            var dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(object.get("dimension").getAsString()));
            int fov = object.has("fov") ? Mth.clamp(object.get("fov").getAsInt(), 30, 110) : 70;
            views.add(new View(dimension, x, y, z, yaw, pitch, fov, label, object.getAsJsonArray("biomeProbes")));
        }
        minecraft.options.hideGui = true;
        minecraft.options.pauseOnLostFocus = false;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.options.renderDistance().set(Mth.clamp(Integer.getInteger("aoemod.structureAudit.renderDistance", 6), 6, 20));
        writeReport("waiting_for_world", null);
        AOEMod.LOGGER.info("Structure screenshot audit loaded {} viewpoints from {}", views.size(), input);
    }

    private static void requestTeleport(Minecraft minecraft, View view) throws Exception {
        requested = true;
        phaseNanos = System.nanoTime();
        settledTicks = 0;
        int requestIndex = index;
        var playerId = minecraft.player.getUUID();
        var server = minecraft.getSingleplayerServer();
        minecraft.options.fov().set(view.fov());
        minecraft.setCameraEntity(minecraft.player);
        server.execute(() -> {
            if (completed) return;
            try {
                var player = server.getPlayerList().getPlayer(playerId);
                var destination = server.getLevel(view.dimension());
                if (player == null || destination == null) throw new IllegalStateException("Missing QA player/dimension for " + view.label());
                destination.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
                destination.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                destination.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                // Custom dimensions share the Overworld's time/weather through DerivedLevelData.
                server.overworld().setDayTime(6000);
                server.overworld().setWeatherParameters(6000, 0, false, false);
                player.stopRiding();
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(destination, view.x(), view.y(), view.z(), view.yaw(), view.pitch());
                // Send the effect after the dimension-change packet, which recreates the client player.
                player.removeEffect(MobEffects.NIGHT_VISION);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60 * 10, 0, false, false, false));
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0;
                serverReadyIndex = requestIndex;
            } catch (Throwable failure) {
                serverFailure = "Server teleport failed for " + view.label() + ": " + failure;
                AOEMod.LOGGER.error("Structure screenshot server preparation failed", failure);
            }
        });
        writeReport("teleporting", null);
    }

    private static void capture(Minecraft minecraft, View view) throws Exception {
        Path imagePath = screenshotDirectory.resolve(view.label() + ".png");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", view.label());
        result.put("dimension", view.dimension().location().toString());
        result.put("x", minecraft.player.getX()); result.put("y", minecraft.player.getY()); result.put("z", minecraft.player.getZ());
        result.put("yaw", minecraft.player.getYRot()); result.put("pitch", minecraft.player.getXRot());
        result.put("settledClientTicks", settledTicks);
        result.put("readyChunkRadius", READY_CHUNK_RADIUS);
        result.put("terrainReadiness", terrainWait);
        if (view.label().equals("tropical-crabs-and-starfish")) {
            result.put("smallAquaticSeagrass", auditSmallAquaticSeagrass(minecraft));
        }
        if (view.label().equals("tropical-fish-buckets")) {
            var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TROPICAL_FISH_BUCKET);
            var model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
            var sprites = model.getQuads(null, null, net.minecraft.util.RandomSource.create(42)).stream()
                    .map(quad -> quad.getSprite().contents().name().toString()).distinct().toList();
            if (!sprites.contains("aoemod:item/tropical_fish_bucket")) {
                throw new IllegalStateException("The default tropical fish bucket lost its complete fish sprite: " + sprites);
            }
            result.put("defaultBucketSprites", sprites);
        }
        if (view.biomeProbes() != null) {
            List<Map<String, Object>> actual = new ArrayList<>();
            for (var element : view.biomeProbes()) {
                var probe = element.getAsJsonObject();
                int x = probe.get("x").getAsInt(), z = probe.get("z").getAsInt();
                var expected = probe.get("biome").getAsString();
                for (int y : new int[]{20, 39, 54, 70, 71, 100, 200}) {
                    var key = minecraft.level.getBiome(new BlockPos(x, y, z)).unwrapKey().orElseThrow();
                    actual.add(Map.of("x", x, "y", y, "z", z, "expected", expected, "actual", key.location().toString()));
                }
            }
            result.put("biomeProbeResults", actual);
        }
        if (view.label().equals("goblin-shield-front")) {
            List<String> checks = new ArrayList<>();
            ClientAssetRevisionAudit.auditGoblinShield(minecraft, checks);
            result.put("shieldPoseChecks", checks);
        }
        if (view.dimension().equals(GameplayDimensions.TROPICAL)) {
            result.put("tropicalClientBiomeAudit", auditTropicalClientBiomes(minecraft));
        }
        // ClientTickEvent runs on the render/client thread; the prior frame has
        // already rendered the acknowledged stationary camera for 120 ticks.
        try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            image.writeToFile(imagePath);
            result.put("width", image.getWidth()); result.put("height", image.getHeight());
        }
        result.put("path", imagePath.toString());
        captures.add(result);
        writeReport("running", null);
        AOEMod.LOGGER.info("Captured structure viewpoint {} at {}", view.label(), imagePath);
    }

    private static List<Map<String, Object>> auditSmallAquaticSeagrass(Minecraft minecraft) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (BlockPos pos : List.of(new BlockPos(59, 161, 4), new BlockPos(61, 161, 4),
                new BlockPos(63, 161, 4), new BlockPos(64, 161, 7))) {
            boolean occupied = pos.getZ() == 4;
            var state = minecraft.level.getBlockState(pos);
            if (!state.is(net.minecraft.world.level.block.Blocks.SEAGRASS))
                throw new IllegalStateException("QA plant missing at " + pos);
            var model = minecraft.getBlockRenderer().getBlockModel(state);
            var data = model.getModelData(minecraft.level, pos, state, net.minecraftforge.client.model.data.ModelData.EMPTY);
            var quads = model.getQuads(state, null, net.minecraft.util.RandomSource.create(91), data,
                    net.minecraft.client.renderer.RenderType.cutout());
            double maximum = Double.NEGATIVE_INFINITY;
            for (var quad : quads) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) maximum = Math.max(maximum,
                        Float.intBitsToFloat(vertices[vertex * stride + 1]));
            }
            boolean clipped = net.amurdza.examplemod.client.SmallAquaticSeagrass.clippedAt(pos);
            if (quads.isEmpty() || clipped != occupied || Math.abs(maximum - (occupied ? .125 : 1)) > .0001)
                throw new IllegalStateException("Seagrass model clipping mismatch at " + pos + ": occupied="
                        + occupied + ", clipped=" + clipped + ", maxQuadY=" + maximum + ", quads=" + quads.size());
            results.add(Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ(), "occupied", occupied,
                    "clipped", clipped, "quadCount", quads.size(), "maximumQuadY", maximum));
        }
        return results;
    }

    private static Map<String, Object> auditTropicalClientBiomes(Minecraft minecraft) {
        var layout = TropicalBiomeSync.clientLayout();
        if (layout == null) throw new IllegalStateException("Tropical client layout seed has not synchronized");
        int columns = 0, mangrove = 0, rainforest = 0, coral = 0, cherry = 0, heightChecks = 0;
        int centreX = minecraft.player.blockPosition().getX();
        int centreZ = minecraft.player.blockPosition().getZ();
        // All sampled columns fit inside the already-arrived four-chunk radius.
        // Query the actual ClientLevel API used by F3, foliage and plant lookups.
        int radius = 48;
        for (int x = centreX - radius; x <= centreX + radius; x++) {
            for (int z = centreZ - radius; z <= centreZ + radius; z++) {
                BlockPos pos = new BlockPos(x, TropicalChannelSurface.WATER_SURFACE_Y, z);
                var seaBiome = minecraft.level.getBiome(pos);
                var highBiome = minecraft.level.getBiome(pos.atY(120));
                if (!seaBiome.equals(highBiome)) {
                    throw new IllegalStateException("Tropical client biome changes between Y63 and Y120 at " + pos
                            + ": " + seaBiome.unwrapKey() + " / " + highBiome.unwrapKey());
                }
                if (seaBiome.is(TropicalChannelSurface.MANGROVE) || seaBiome.is(TropicalChannelSurface.RAINFOREST)) {
                    int surface = (int) Math.floor(layout.surfaceHeight(x, z) + .5);
                    boolean swamp = seaBiome.is(TropicalChannelSurface.MANGROVE);
                    if (swamp != (surface <= 63)) {
                        throw new IllegalStateException("Tropical client biome misses the terrain-height split at " + pos
                                + ": surface=" + surface + ", biome=" + seaBiome.unwrapKey());
                    }
                    heightChecks++;
                    if (swamp) mangrove++;
                    else rainforest++;
                } else if (seaBiome.is(TropicalChannelSurface.CORAL)) coral++;
                else if (seaBiome.is(TropicalChannelSurface.CHERRY)) cherry++;
                columns++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("layoutSynchronized", true);
        result.put("radiusBlocks", radius);
        result.put("columnsChecked", columns);
        result.put("altitudeComparisons", columns);
        result.put("terrainHeightComparisons", heightChecks);
        result.put("mangroveColumns", mangrove);
        result.put("rainforestColumns", rainforest);
        result.put("coralColumns", coral);
        result.put("cherryColumns", cherry);
        return result;
    }

    private static void writeReport(String status, String failure) throws Exception {
        if (reportPath == null) return;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", status);
        report.put("started", started == null ? null : started.toString());
        report.put("elapsedMillis", (System.nanoTime() - startNanos) / 1_000_000L);
        report.put("requestedViews", views.size());
        report.put("completedViews", captures.size());
        report.put("activeView", index < views.size() ? views.get(index).label() : null);
        report.put("views", captures);
        if (failure != null) report.put("failure", failure);
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }

    private static void finish(Minecraft minecraft, String failure) {
        completed = true;
        try { writeReport(failure == null ? "passed" : "failed", failure); }
        catch (Exception writeFailure) { AOEMod.LOGGER.error("Cannot write structure screenshot report", writeFailure); }
        if (Boolean.getBoolean("aoemod.structureAudit.exit")) minecraft.stop();
    }
}
