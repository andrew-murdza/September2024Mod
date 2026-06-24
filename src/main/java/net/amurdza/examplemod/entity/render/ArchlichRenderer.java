package net.amurdza.examplemod.entity.render;

import com.github.alexthe666.iceandfire.client.render.entity.RenderDreadLich;
import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.ArchlichEntity;
import net.amurdza.examplemod.entity.model.ArchlichModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ArchlichRenderer extends MobRenderer<ArchlichEntity, ArchlichModel> {
    public ArchlichRenderer(EntityRendererProvider.Context context) {
        super(context, new ArchlichModel(), 0.5F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public @NotNull RenderType renderType() {
                return RenderType.eyes(RenderDreadLich.TEXTURE_EYES);
            }
        });
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(ArchlichEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.1F, 1.1F, 1.1F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(ArchlichEntity entity) {
        return switch (entity.getVariant()) {
            case 1 -> RenderDreadLich.TEXTURE_1;
            case 2 -> RenderDreadLich.TEXTURE_2;
            case 3 -> RenderDreadLich.TEXTURE_3;
            case 4 -> RenderDreadLich.TEXTURE_4;
            default -> RenderDreadLich.TEXTURE_0;
        };
    }
}
