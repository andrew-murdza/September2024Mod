package net.amurdza.examplemod.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hybridlabs.aquatic.entity.cephalopod.UmbrellaOctopusEntity;
import net.amurdza.examplemod.client.UmbrellaOctopusEyeLayer;
import net.amurdza.examplemod.util.TropicalUmbrellaVariants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;
import java.util.UUID;

/** Called by the existing opt-in aquatic client audit after resources and live renderers load. */
public final class ClientUmbrellaEyeAudit {
    private ClientUmbrellaEyeAudit() { }

    @SuppressWarnings("unchecked")
    public static List<String> verify() {
        var minecraft = Minecraft.getInstance();
        var octopus = (UmbrellaOctopusEntity) ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation("hybrid_aquatic:umbrella_octopus")).create(minecraft.level);
        try {
            var renderer = (GeoEntityRenderer<UmbrellaOctopusEntity>) minecraft.getEntityRenderDispatcher().getRenderer(octopus);
            var eyes = renderer.getRenderLayers().stream().filter(UmbrellaOctopusEyeLayer.class::isInstance)
                    .map(UmbrellaOctopusEyeLayer.class::cast).findFirst().orElseThrow(
                            () -> new IllegalStateException("The live umbrella-octopus renderer is missing its eye layer"));
            require(minecraft.getResourceManager().getResource(UmbrellaOctopusEyeLayer.WHITE_MATERIAL).isPresent(),
                    "The existing white eye material must resolve");
            var model = renderer.getGeoModel().getBakedModel(renderer.getGeoModel().getModelResource(octopus));
            var body = model.getBone("body").orElseThrow();
            int variants = 0;
            for (int color = 0; color < 5; color++) {
                octopus.setUUID(new UUID(0, color));
                var expected = new ResourceLocation("hybrid_aquatic", "textures/entity/cephalopod/umbrella_octopus/umbrella_octopus_"
                        + TropicalUmbrellaVariants.color(octopus) + ".png");
                require(renderer.getGeoModel().getTextureResource(octopus).equals(expected),
                        "Eye changes must preserve the original variant texture selection");
                var originalType = RenderType.entityCutoutNoCull(expected);
                var vertices = new EyeVertices();
                var lastBuffer = new RenderType[1];
                eyes.renderForBone(new PoseStack(), octopus, body, originalType,
                        type -> { lastBuffer[0] = type; return vertices; }, vertices, 0, 0x00700070, OverlayTexture.NO_OVERLAY);
                require(vertices.count == 16 && vertices.white == 8 && vertices.black == 8,
                        "Every variant must render two white eyeballs with two black pupils");
                require(vertices.lit == vertices.count && vertices.finite && vertices.left == 8 && vertices.right == 8,
                        "Both eyes must follow the baked side faces and retain incoming world light");
                require(lastBuffer[0] == originalType, "The native material must be restored before rendering fins and tentacles");
                octopus.setInvisible(true);
                eyes.renderForBone(new PoseStack(), octopus, body, originalType, type -> vertices, vertices,
                        0, 0x00700070, OverlayTexture.NO_OVERLAY);
                require(vertices.count == 16, "Invisible octopuses must not reveal their eye overlays");
                octopus.setInvisible(false);
                variants++;
            }
            return List.of("Umbrella live renderer eye layer installed", variants + " variants retain native body textures",
                    "Two white eyeballs and black pupils cover the native eye faces", "Eyes retain world light and respect invisibility");
        } finally {
            octopus.discard();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Umbrella octopus eyes: " + message);
    }

    private static final class EyeVertices implements VertexConsumer {
        private int count, white, black, lit, left, right;
        private boolean finite = true;
        @Override public VertexConsumer vertex(double x, double y, double z) {
            count++;
            finite &= Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
            if (x < -3.0 / 16) left++;
            if (x > 3.0 / 16) right++;
            return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            if (r == 255 && g == 255 && b == 255 && a == 255) white++;
            if (r == 0 && g == 0 && b == 0 && a == 255) black++;
            return this;
        }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { if (u == 112 && v == 112) lit++; return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    }
}
