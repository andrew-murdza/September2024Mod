package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Target the declaring vanilla class so the shadow field receives normal mappings. */
@Mixin(EntityRenderer.class)
public interface CrabRendererShadow {
    @Accessor("shadowRadius")
    void aoemod$setCrabShadowRadius(float radius);
}
