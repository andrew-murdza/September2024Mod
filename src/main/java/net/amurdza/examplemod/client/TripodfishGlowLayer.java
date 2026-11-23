package net.amurdza.examplemod.client;

import com.github.alexmodguy.alexscaves.client.model.TripodfishModel;
import com.github.alexmodguy.alexscaves.server.entity.living.TripodfishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Keeps the tripodfish visible in unlit deep water, using its original colors. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TripodfishGlowLayer extends EyesLayer<TripodfishEntity, TripodfishModel> {
    private static final RenderType GLOW = RenderType.eyes(
            new ResourceLocation("alexscaves", "textures/entity/tripodfish.png"));

    public TripodfishGlowLayer(RenderLayerParent<TripodfishEntity, TripodfishModel> parent) {
        super(parent);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayer(EntityRenderersEvent.AddLayers event) {
        EntityType type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexscaves", "tripodfish"));
        if (type == null) return;
        net.minecraft.client.renderer.entity.LivingEntityRenderer renderer = event.getRenderer(type);
        if (renderer != null) renderer.addLayer(new TripodfishGlowLayer((RenderLayerParent) renderer));
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, TripodfishEntity fish,
                       float limbSwing, float limbAmount, float partialTick, float age, float headYaw, float headPitch) {
        if (!fish.isInvisible()) {
            super.render(pose, buffers, light, fish, limbSwing, limbAmount, partialTick, age, headYaw, headPitch);
        }
    }
}
