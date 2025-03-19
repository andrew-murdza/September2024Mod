//package net.amurdza.examplemod.mixins.othermods.trailsandtalesplus;
//
//import com.belgieyt.trailsandtalesplus.Objects.world.TTFeatures;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.world.level.biome.Biome;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//
//@Mixin(TTFeatures.class)
//public class RemoveTTFeaturesBiome {
//    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lcom/belgieyt/trailsandtalesplus/Objects/world/TTFeatures;register(Ljava/lang/String;)Lnet/minecraft/resources/ResourceKey;"),remap = false)
//    private static ResourceKey<Biome> hi(String p_48229_){
//        return null;
//    }
//}
