// mixins/DimensionSpecialEffectsAccessor.java
package net.amurdza.examplemod.mixins.nether_fog;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DimensionSpecialEffects.class)
public interface NetherFog1 {
    @Accessor("EFFECTS")
    static Object2ObjectMap<ResourceLocation, DimensionSpecialEffects> aoemod$getEffects() {
        throw new AssertionError();
    }
}