package net.amurdza.examplemod.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.client.SmallAquaticSeagrass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

@Mixin(GeoEntityRenderer.class)
public abstract class SmallAquaticModelCapture<T extends Entity & GeoAnimatable> implements GeoRenderer<T> {
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void aoemod$beginMask(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                                 int light, CallbackInfo ci) {
        SmallAquaticSeagrass.begin(entity, pose, partialTick, getTextureLocation(entity));
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void aoemod$endMask(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                               int light, CallbackInfo ci) {
        SmallAquaticSeagrass.end(entity);
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f pose, Vector3f normal, VertexConsumer buffer,
                                     int light, int overlay, float red, float green, float blue, float alpha) {
        SmallAquaticSeagrass.quad(quad, pose);
        // GeckoLib 4.8.4's default emission is four vertices. An interface-super invocation here
        // is rewritten to an invalid invocation by Mixin, so emit those vertices directly.
        for (var vertex : quad.vertices()) {
            Vector4f point = pose.transform(new Vector4f(vertex.position(), 1));
            buffer.vertex(point.x(), point.y(), point.z(), red, green, blue, alpha,
                    vertex.texU(), vertex.texV(), overlay, light, normal.x(), normal.y(), normal.z());
        }
    }
}
