package net.amurdza.examplemod.gametest;

import com.github.alexmodguy.alexscaves.server.entity.living.TripodfishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.client.TripodfishGlowLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;

/** Opt-in renderer check used by the isolated screenshot client. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientTripodfishGlowAudit {
    private static final String REPORT = System.getProperty("aoemod.qa.tripodfishGlowReport");
    private static boolean attempted;
    private ClientTripodfishGlowAudit() { }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (REPORT == null || attempted || event.phase != TickEvent.Phase.END
                || Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;
        attempted = true;
        var result = new LinkedHashMap<String, Object>();
        try {
            var checks = new java.util.ArrayList<>(verify());
            checks.addAll(ClientUmbrellaEyeAudit.verify());
            result.put("checks", checks);
            result.put("passed", true);
        } catch (Throwable failure) {
            result.put("passed", false);
            result.put("error", failure.toString());
        }
        try {
            Path report = Path.of(REPORT).toAbsolutePath();
            Files.createDirectories(report.getParent());
            Files.writeString(report, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot write tripodfish QA report", failure);
        }
    }

    public static List<String> verify() throws ReflectiveOperationException {
        var minecraft = Minecraft.getInstance();
        var fish = (TripodfishEntity) ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation("alexscaves:tripodfish")).create(minecraft.level);
        try {
            var renderer = minecraft.getEntityRenderDispatcher().getRenderer(fish);
            TripodfishGlowLayer glow = null;
            // Inspect the actual AddLayers result; no renderer is constructed by the test.
            for (var field : LivingEntityRenderer.class.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                for (Object layer : (List<?>) field.get(renderer)) {
                    if (layer instanceof TripodfishGlowLayer found) glow = found;
                }
            }
            require(glow != null, "The live tripodfish renderer must contain its emissive layer");
            require(minecraft.getResourceManager().getResource(new ResourceLocation(
                    "alexscaves:textures/entity/tripodfish.png")).isPresent(), "The native fish texture must resolve");
            var vertices = new GlowVertices();
            glow.render(new PoseStack(), type -> vertices, 0, fish, 0, 0, 0, 0, 0, 0);
            require(vertices.count > 0 && vertices.fullBright == vertices.count,
                    "Every glow vertex must use fullbright even with zero incoming world light");
            int visibleVertices = vertices.count;
            fish.setInvisible(true);
            glow.render(new PoseStack(), type -> vertices, 0, fish, 0, 0, 0, 0, 0, 0);
            require(vertices.count == visibleVertices, "Invisible fish must not reveal an emissive silhouette");
            return List.of("Tripodfish live renderer layer installed", "Native texture resolved",
                    visibleVertices + " vertices emissive in zero light", "Invisibility hides glow");
        } finally {
            fish.discard();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Tripodfish glow: " + message);
    }

    private static final class GlowVertices implements VertexConsumer {
        private int count, fullBright;
        @Override public VertexConsumer vertex(double x, double y, double z) { count++; return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        // Vanilla EyesLayer uses the emissive eyes shader and its fixed 0/240 lightmap.
        @Override public VertexConsumer uv2(int u, int v) { if (u == 0 && v == 240) fullBright++; return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    }
}
