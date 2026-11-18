package net.amurdza.examplemod.mixins.othermods.embeddium;

import net.amurdza.examplemod.client.ExactBiomeTintView;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use Embeddium's single-block vertex path on exact AOE biome columns. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.model.quad.blender.BlendedColorProvider", remap = false)
public abstract class ExactVertexBiomeTint {
    @Shadow private static boolean shouldUseVertexBlending;

    @Redirect(method = "getColors", at = @At(value = "FIELD",
            target = "Lme/jellysquid/mods/sodium/client/model/quad/blender/BlendedColorProvider;shouldUseVertexBlending:Z"))
    private boolean aoemod$useExactBlock(@Coerce Object view, BlockPos pos, Object state, @Coerce Object quad, int[] colors) {
        return shouldUseVertexBlending && !((ExactBiomeTintView) view).aoemod$exactBiomeTints();
    }
}
