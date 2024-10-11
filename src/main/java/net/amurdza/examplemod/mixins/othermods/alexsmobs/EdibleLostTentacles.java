package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(AMItemRegistry.class)
public class EdibleLostTentacles {
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;"))
    private static RegistryObject<Item> hi(DeferredRegister<Item> instance, String s, Supplier<Item> itemSupplier){
        if(s.equals("lost_tentacle")){
            return  instance.register(s,()-> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.5F).meat().build())));
        }
        return instance.register(s,itemSupplier);
    }
}
