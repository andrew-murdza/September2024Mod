package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MobCategory.class)
public class MobCap {
//    @ModifyConstant(method = "<clinit>",constant = @Constant(intValue = 10))
//    private static int hi(int constant){
//        return 50;
//    }
//    @ModifyConstant(method = "<clinit>",constant = @Constant(intValue = 5,ordinal = 1))
//    private static int hi1(int constant){
//        return 15;
//    }
//    @ModifyConstant(method = "<clinit>",constant = @Constant(intValue = 20))
//    private static int hi2(int constant){
//        return 100;
//    }
}
