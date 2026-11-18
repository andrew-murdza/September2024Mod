package net.amurdza.examplemod.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hybridlabs.aquatic.client.render.entity.crustacean.FiddlerCrabEntityRenderer;
import dev.hybridlabs.aquatic.entity.crustacean.FiddlerCrabEntity;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.render.YakPannierLayer;
import net.amurdza.examplemod.entity.render.YakRenderer;
import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Exercises the actual reloaded renderer and resource manager, rather than only packaged files. */
final class November15AppearanceAudit {
    private November15AppearanceAudit() { }

    static void audit(Minecraft minecraft, List<String> checks) throws Exception {
        var yak = ModEntities.YAK.get().create(minecraft.level);
        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(yak);
        require(renderer instanceof YakRenderer, "Yak must keep the native cold-cow body renderer");
        var layer = ((YakRenderer) renderer).pannierLayer();
        var vertices = new PannierVertices();
        net.minecraft.client.renderer.MultiBufferSource buffers = type -> {
            require(type == RenderType.entityCutoutNoCull(YakPannierLayer.TEXTURE), "Panniers must use the actual donkey atlas");
            return vertices;
        };
        layer.render(new PoseStack(), buffers, 15728880, yak, 0, 0, 0, 0, 0, 0);
        require(vertices.count == 0, "Unequipped yak must have no chest geometry");
        yak.setChest(true);
        layer.render(new PoseStack(), buffers, 15728880, yak, 0, 0, 0, 0, 0, 0);
        require(vertices.count == 48 && vertices.left == 24 && vertices.right == 24,
                "Equipped yak needs exactly two complete native donkey chest cuboids");
        require(close(vertices.minAbsX, 6.5F / 16) && close(vertices.maxAbsX, 9.5F / 16)
                        && close(vertices.minY, 3F / 16) && close(vertices.maxY, 11F / 16)
                        && close(vertices.minZ, 1F / 16) && close(vertices.maxZ, 9F / 16),
                "Native 8x8x3 panniers must sit flush against both sides of the rear yak torso");
        require(close(vertices.minU, 26F / 64) && close(vertices.maxU, 48F / 64)
                        && close(vertices.minV, 21F / 64) && close(vertices.maxV, 32F / 64),
                "Both panniers must retain the complete native donkey chest UV island");
        require(minecraft.getResourceManager().getResource(YakPannierLayer.TEXTURE).isPresent(), "Native donkey atlas must resolve live");
        yak.setInvisible(true);
        layer.render(new PoseStack(), buffers, 15728880, yak, 0, 0, 0, 0, 0, 0);
        yak.setInvisible(false);
        yak.setChest(false);
        layer.render(new PoseStack(), buffers, 15728880, yak, 0, 0, 0, 0, 0, 0);
        require(vertices.count == 48, "Invisibility and chest removal must hide panniers immediately");
        yak.discard();
        checks.add("Yak: two unscaled vanilla donkey chest boxes, native atlas/UVs, flush torso anchors and equipment visibility passed.");

        var crab = (FiddlerCrabEntity) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:fiddler_crab")).create(minecraft.level);
        var crabRenderer = minecraft.getEntityRenderDispatcher().getRenderer(crab);
        require(crabRenderer instanceof FiddlerCrabEntityRenderer, "Fiddler crab must retain its native renderer");
        var nativeModel = ((FiddlerCrabEntityRenderer) crabRenderer).getGeoModel();
        var seen = new HashSet<String>();
        String[] names = {"blue", "purple", "red"};
        for (int seed = 0; seed < 64; seed++) {
            crab.setUUID(new UUID(0, seed));
            String variant = names[kotlin.random.RandomKt.Random((long) seed).nextInt(names.length)];
            var expected = new ResourceLocation(AOEMod.MOD_ID, "textures/entity/crustacean/fiddler_crab/fiddler_crab_" + variant + ".png");
            require(expected.equals(crabRenderer.getTextureLocation(crab)) && expected.equals(nativeModel.getTextureResource(crab)),
                    "Native UUID selection must resolve the repaired " + variant + " atlas through both model and renderer");
            if (!seen.add(variant)) continue;
            var resource = minecraft.getResourceManager().getResourceOrThrow(expected);
            byte[] actual;
            try (var stream = resource.open()) { actual = stream.readAllBytes(); }
            byte[] repaired = Files.readAllBytes(ModList.get().getModFileById(AOEMod.MOD_ID).getFile()
                    .findResource("assets", AOEMod.MOD_ID, expected.getPath()));
            require(Arrays.equals(actual, repaired), "Live " + variant + " texture must contain the repaired shell and underside pixels");
            var nativeId = new ResourceLocation("hybrid_aquatic", expected.getPath());
            byte[] legacy;
            try (var stream = minecraft.getResourceManager().getResourceOrThrow(nativeId).open()) { legacy = stream.readAllBytes(); }
            checks.add("Fiddler " + variant + ": native renderer -> " + expected + " pack=" + resource.sourcePackId()
                    + " sha256=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(actual))
                    + "; legacy namespace already repaired=" + Arrays.equals(legacy, repaired));
        }
        require(seen.size() == 3, "Audit must exercise every native fiddler crab variant");
        crab.discard();
    }

    private static boolean close(float actual, float expected) { return Math.abs(actual - expected) < .00001F; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }

    private static final class PannierVertices implements VertexConsumer {
        private int count, left, right;
        private float minAbsX = Float.POSITIVE_INFINITY, maxAbsX = Float.NEGATIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        private float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
        private float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
        @Override public VertexConsumer vertex(double x, double y, double z) {
            count++; if (x < 0) left++; else right++;
            minAbsX = Math.min(minAbsX, (float) Math.abs(x)); maxAbsX = Math.max(maxAbsX, (float) Math.abs(x));
            minY = Math.min(minY, (float) y); maxY = Math.max(maxY, (float) y);
            minZ = Math.min(minZ, (float) z); maxZ = Math.max(maxZ, (float) z);
            return this;
        }
        @Override public VertexConsumer uv(float u, float v) {
            minU = Math.min(minU, u); maxU = Math.max(maxU, u); minV = Math.min(minV, v); maxV = Math.max(maxV, v); return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    }
}
