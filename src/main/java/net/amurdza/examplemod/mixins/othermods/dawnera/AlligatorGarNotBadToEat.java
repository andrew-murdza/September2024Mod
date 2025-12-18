package net.amurdza.examplemod.mixins.othermods.dawnera;

//import net.minecraft.world.effect.MobEffectInstance;
//import net.minecraft.world.food.FoodProperties;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//import ru.xishnikus.thedawnera.common.item.TDEItems;
//
//import java.util.function.Supplier;
//
//@Mixin(TDEItems.class)
//public class AlligatorGarNotBadToEat {
//    @Redirect(method = "<clinit>",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodProperties$Builder;effect(Ljava/util/function/Supplier;F)Lnet/minecraft/world/food/FoodProperties$Builder;",ordinal = 3))
//    private static FoodProperties.Builder hi(FoodProperties.Builder instance, Supplier<MobEffectInstance> effectIn, float probability){
//        return instance;
//    }
//}
