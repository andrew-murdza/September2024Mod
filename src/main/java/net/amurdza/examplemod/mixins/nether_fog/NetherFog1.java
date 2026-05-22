package net.amurdza.examplemod.mixins.nether_fog;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.dimension.NetherFog;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DimensionSpecialEffects.class)
public class NetherFog1 {

    @WrapOperation(method = "forType",at= @At(value = "INVOKE", target = "Lnet/minecraftforge/client/DimensionSpecialEffectsManager;getForType(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/DimensionSpecialEffects;"),remap = false)
    private static DimensionSpecialEffects hi(ResourceLocation type, Operation<DimensionSpecialEffects> original){
        return type.equals(new ResourceLocation(AOEMod.MOD_ID,"aoedim"))?
                new NetherFog() : original.call(type);
    }
}
