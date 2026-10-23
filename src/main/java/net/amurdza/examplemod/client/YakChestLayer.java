package net.amurdza.examplemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ai.YakAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Paired chest geometry rendered from the vanilla chest model, retaining resource-pack support. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class YakChestLayer extends RenderLayer<Cow, EntityModel<Cow>> {
    public YakChestLayer(RenderLayerParent<Cow, EntityModel<Cow>> parent) { super(parent); }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayer(EntityRenderersEvent.AddLayers event) {
        net.minecraft.client.renderer.entity.LivingEntityRenderer renderer = event.getRenderer(EntityType.COW);
        if (renderer != null) renderer.addLayer(new YakChestLayer((RenderLayerParent) renderer));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, Cow cow,
                       float limbSwing, float limbAmount, float partialTick, float age, float headYaw, float headPitch) {
        if (!(cow instanceof YakAccess yak) || !yak.aoe$hasYakChest() || cow.isInvisible()) return;
        for (int side : new int[]{-1, 1}) {
            pose.pushPose();
            if (cow.isBaby()) { pose.scale(0.5F, 0.5F, 0.5F); pose.translate(0, 1.5, 0); }
            pose.translate(side * 0.58, 0.48, 0.12);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.scale(0.9F, 0.9F, 0.9F);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Items.CHEST),
                    ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, cow.level(), cow.getId());
            pose.popPose();
        }
    }
}
