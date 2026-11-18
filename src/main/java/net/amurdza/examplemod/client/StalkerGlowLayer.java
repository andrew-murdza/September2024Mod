package net.amurdza.examplemod.client;

import blueduck.outer_end.client.model.StalkerModel;
import blueduck.outer_end.entity.Stalker;
import net.amurdza.examplemod.AOEMod;
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

/** The source mod already supplies a mask containing exactly the cobalt highlights. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StalkerGlowLayer extends EyesLayer<Stalker, StalkerModel<Stalker>> {
    private static final RenderType GLOW = RenderType.eyes(
            new ResourceLocation("outer_end", "textures/entity/stalker_cobaltglow.png"));

    public StalkerGlowLayer(RenderLayerParent<Stalker, StalkerModel<Stalker>> parent) { super(parent); }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayer(EntityRenderersEvent.AddLayers event) {
        EntityType type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("outer_end", "stalker"));
        if (type == null) return;
        net.minecraft.client.renderer.entity.LivingEntityRenderer renderer = event.getRenderer(type);
        if (renderer != null) renderer.addLayer(new StalkerGlowLayer((RenderLayerParent) renderer));
    }

    @Override
    public RenderType renderType() { return GLOW; }

    @Override
    public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                       int light, Stalker stalker, float limbSwing, float limbAmount, float partialTick,
                       float age, float headYaw, float headPitch) {
        if (stalker.getColor().equals("cobalt") && !stalker.isInvisible()) {
            super.render(pose, buffers, light, stalker, limbSwing, limbAmount, partialTick, age, headYaw, headPitch);
        }
    }
}
