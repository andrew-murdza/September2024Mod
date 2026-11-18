package net.amurdza.examplemod.mixins.othermods.orcz;

import com.duke.orcz.client.model.ModelOrc_big_savage_2sword;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Isolate sword UVs shared by native head details, preserving all native geometry. */
@Mixin(value = ModelOrc_big_savage_2sword.class, remap = false)
public abstract class BoneBossSwordUV {
    @Redirect(method = "createBodyLayer", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;texOffs(II)Lnet/minecraft/client/model/geom/builders/CubeListBuilder;"))
    private static CubeListBuilder aoemod$isolateSwordTexture(CubeListBuilder builder, int u, int v) {
        return u == 0 && v == 18 ? builder.texOffs(80, 80) : builder.texOffs(u, v);
    }
}
