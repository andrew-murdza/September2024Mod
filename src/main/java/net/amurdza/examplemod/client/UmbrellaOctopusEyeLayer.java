package net.amurdza.examplemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hybridlabs.aquatic.entity.cephalopod.UmbrellaOctopusEntity;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

/** Small geometry overlays replace only the native two purple eye patches in every color variant. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UmbrellaOctopusEyeLayer extends GeoRenderLayer<UmbrellaOctopusEntity> {
    // Reuse a shipped white material; the original octopus textures remain untouched.
    public static final ResourceLocation WHITE_MATERIAL = new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    private static final RenderType EYES = RenderType.entityCutoutNoCull(WHITE_MATERIAL);
    private static final float TEXTURE_SIZE = 32;

    public UmbrellaOctopusEyeLayer(GeoRenderer<UmbrellaOctopusEntity> renderer) {
        super(renderer);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayer(EntityRenderersEvent.AddLayers event) {
        EntityType type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", "umbrella_octopus"));
        if (type != null && event.getEntityRenderer(type) instanceof GeoEntityRenderer renderer) {
            renderer.addRenderLayer(new UmbrellaOctopusEyeLayer(renderer));
        }
    }

    @Override
    public void renderForBone(PoseStack pose, UmbrellaOctopusEntity octopus, GeoBone bone, RenderType renderType,
                              MultiBufferSource buffers, VertexConsumer original, float partialTick, int light, int overlay) {
        if (!bone.getName().equals("body") || bone.isHidden() || octopus.isInvisible()) return;
        var vertices = buffers.getBuffer(EYES);
        for (var cube : bone.getCubes()) {
            pose.pushPose();
            RenderUtils.translateToPivotPoint(pose, cube);
            RenderUtils.rotateMatrixAroundCube(pose, cube);
            RenderUtils.translateAwayFromPivotPoint(pose, cube);
            for (var quad : cube.quads()) {
                if (quad == null) continue;
                // Native 32x32 body UVs: the left and right eyes each occupy 2x2 texels.
                for (int left : new int[]{3, 13}) {
                    if (!contains(quad, left, 8, left + 2, 10)) continue;
                    rectangle(pose, vertices, quad, left, 8, left + 2, 10, 0.0005F, 255, light, overlay);
                    rectangle(pose, vertices, quad, left + 0.5F, 8.5F, left + 1.5F, 9.5F, 0.001F, 0, light, overlay);
                }
            }
            pose.popPose();
        }
        // Bone layers run before the native fins/tentacles; restore their material buffer.
        buffers.getBuffer(renderType);
    }

    private static boolean contains(GeoQuad quad, float left, float top, float right, float bottom) {
        float minU = Float.POSITIVE_INFINITY, minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
        for (var vertex : quad.vertices()) {
            minU = Math.min(minU, vertex.texU() * TEXTURE_SIZE);
            minV = Math.min(minV, vertex.texV() * TEXTURE_SIZE);
            maxU = Math.max(maxU, vertex.texU() * TEXTURE_SIZE);
            maxV = Math.max(maxV, vertex.texV() * TEXTURE_SIZE);
        }
        return left >= minU && right <= maxU && top >= minV && bottom <= maxV;
    }

    private static void rectangle(PoseStack pose, VertexConsumer consumer, GeoQuad quad,
                                  float left, float top, float right, float bottom, float offset,
                                  int color, int light, int overlay) {
        for (float[] uv : new float[][]{{left, top}, {right, top}, {right, bottom}, {left, bottom}}) {
            var position = positionAt(quad, uv[0] / TEXTURE_SIZE, uv[1] / TEXTURE_SIZE)
                    .fma(offset, quad.normal());
            consumer.vertex(pose.last().pose(), position.x(), position.y(), position.z())
                    .color(color, color, color, 255).uv(0.5F, 0.5F).overlayCoords(overlay).uv2(light)
                    .normal(pose.last().normal(), quad.normal().x(), quad.normal().y(), quad.normal().z()).endVertex();
        }
    }

    /** Project the exact native eye UV rectangle onto its animated body face, including mirrored faces. */
    private static Vector3f positionAt(GeoQuad quad, float u, float v) {
        var origin = quad.vertices()[0];
        var right = quad.vertices()[1];
        var down = quad.vertices()[3];
        float duRight = right.texU() - origin.texU(), dvRight = right.texV() - origin.texV();
        float duDown = down.texU() - origin.texU(), dvDown = down.texV() - origin.texV();
        float determinant = duRight * dvDown - duDown * dvRight;
        float alongRight = ((u - origin.texU()) * dvDown - (v - origin.texV()) * duDown) / determinant;
        float alongDown = ((v - origin.texV()) * duRight - (u - origin.texU()) * dvRight) / determinant;
        return new Vector3f(origin.position())
                .fma(alongRight, new Vector3f(right.position()).sub(origin.position()))
                .fma(alongDown, new Vector3f(down.position()).sub(origin.position()));
    }
}
