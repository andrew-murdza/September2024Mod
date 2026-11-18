package net.amurdza.examplemod.gametest;

import blueduck.outer_end.entity.Stalker;
import com.github.alexthe666.alexsmobs.client.event.ClientEvents;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.client.StalkerGlowLayer;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.StriderRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Opt-in destructive-to-position QA: launch only against a disposable integrated-server world. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPortalTransitionAudit {
    private static final String OUTPUT = System.getProperty("aoemod.portalAudit.report");
    private static final List<ResourceKey<Level>> ROUTE = List.of(
            GameplayDimensions.DESERT, GameplayDimensions.SNOWY, GameplayDimensions.DESERT, GameplayDimensions.SNOWY);
    private static final List<Map<String, Object>> CHECKS = new ArrayList<>();
    private static int stage;
    private static int settledTicks;
    private static boolean requested;
    private static boolean completed;
    private static long started;

    private ClientPortalTransitionAudit() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (OUTPUT == null || completed || event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.getOverlay() != null) return;
        if (started == 0) started = System.currentTimeMillis();
        try {
            if (System.currentTimeMillis() - started > 300_000) throw new IllegalStateException("Dimension transition audit timed out");
            var server = minecraft.getSingleplayerServer();
            if (server == null) throw new IllegalStateException("Portal audit requires a disposable integrated server");
            minecraft.options.pauseOnLostFocus = false;
            if (stage == ROUTE.size()) {
                auditRenderers(minecraft);
                finish(minecraft, null);
                return;
            }
            var target = ROUTE.get(stage);
            if (!requested) {
                requested = true;
                var playerId = minecraft.player.getUUID();
                int leg = stage;
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(playerId);
                    var destination = server.getLevel(target);
                    if (player == null || destination == null) throw new IllegalStateException("Missing portal audit world/player");
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(destination, 8.5, Math.min(220, destination.getMaxBuildHeight() - 16), 8.5, 0, 0);
                    // Exercise the real effect toggle that calls Alex's chunk refresh during travel.
                    if ((leg & 1) == 0) player.addEffect(new MobEffectInstance(AMEffectRegistry.LAVA_VISION.get(), 2400));
                    else player.removeEffect(AMEffectRegistry.LAVA_VISION.get());
                });
                return;
            }
            if (!minecraft.level.dimension().equals(target) || minecraft.screen != null) {
                settledTicks = 0;
                return;
            }
            if (++settledTicks < 80) return;
            var viewField = Arrays.stream(LevelRenderer.class.getDeclaredFields())
                    .filter(field -> field.getType() == ViewArea.class).findFirst().orElseThrow();
            viewField.setAccessible(true);
            var view = (ViewArea) viewField.get(minecraft.levelRenderer);
            long nullSlots = view == null ? 0 : Arrays.stream(view.chunks).filter(java.util.Objects::isNull).count();
            var refresh = ClientEvents.class.getDeclaredMethod("updateAllChunks");
            refresh.setAccessible(true);
            refresh.invoke(new ClientEvents());
            Map<String, Object> check = new LinkedHashMap<>();
            check.put("dimension", target.location().toString());
            check.put("nullVanillaRenderSlots", nullSlots);
            check.put("alexLavaVisionChunkRefresh", "passed");
            CHECKS.add(check);
            AOEMod.LOGGER.info("Portal transition audit completed {} with {} null vanilla render slots", target.location(), nullSlots);
            stage++;
            requested = false;
            settledTicks = 0;
        } catch (Throwable failure) {
            AOEMod.LOGGER.error("Portal transition audit failed", failure);
            finish(minecraft, failure);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void auditRenderers(Minecraft minecraft) throws Exception {
        var stalker = (Stalker) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("outer_end", "stalker")).create(minecraft.level);
        stalker.setColor("cobalt");
        var renderer = (LivingEntityRenderer) minecraft.getEntityRenderDispatcher().getRenderer(stalker);
        require(renderer.getTextureLocation(stalker).equals(new ResourceLocation("outer_end", "textures/entity/stalker_cobalt.png")),
                "Stalker renderer did not use native cobalt texture");
        var layers = Arrays.stream(LivingEntityRenderer.class.getDeclaredFields())
                .filter(field -> List.class.isAssignableFrom(field.getType())).findFirst().orElseThrow();
        layers.setAccessible(true);
        long glowLayers = ((List<?>) layers.get(renderer)).stream().filter(StalkerGlowLayer.class::isInstance).count();
        require(glowLayers == 1, "Stalker renderer must contain exactly one cobalt emissive layer");
        require(minecraft.getResourceManager().getResource(new ResourceLocation("outer_end", "textures/entity/stalker_cobaltglow.png")).isPresent(),
                "Native cobalt glow mask is missing");
        var land = EntityType.STRIDER.create(minecraft.level);
        land.setSuffocating(true);
        var striderRenderer = (StriderRenderer) minecraft.getEntityRenderDispatcher().getRenderer(land);
        require(striderRenderer.getTextureLocation(land).getPath().endsWith("strider_cold.png") && !land.isSuffocating(),
                "Land strider must use cold texture without shivering state");
        var lava = new Strider(EntityType.STRIDER, minecraft.level) {
            @Override public boolean isInLava() { return true; }
        };
        require(striderRenderer.getTextureLocation(lava).getPath().endsWith("/strider.png"),
                "Lava strider must return to red texture");
        CHECKS.add(Map.of("renderers", "native cobalt texture + one emissive layer; cold land strider + red lava strider; no suffocating state"));
        stalker.discard();
        land.discard();
        lava.discard();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void finish(Minecraft minecraft, Throwable failure) {
        completed = true;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", failure == null ? "passed" : "failed");
        if (failure != null) report.put("failure", failure.toString());
        report.put("checks", CHECKS);
        try {
            Path path = Path.of(OUTPUT).toAbsolutePath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
        } catch (Exception error) { AOEMod.LOGGER.error("Cannot write portal audit report", error); }
        if (Boolean.getBoolean("aoemod.portalAudit.exit")) minecraft.stop();
    }
}
