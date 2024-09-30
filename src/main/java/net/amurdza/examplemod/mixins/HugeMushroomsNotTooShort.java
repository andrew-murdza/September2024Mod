package net.amurdza.examplemod.mixins;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractHugeMushroomFeature.class)
public class HugeMushroomsNotTooShort {
    @Redirect(method = "getTreeHeight",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/util/RandomSource;nextInt(I)I",ordinal = 0))
    private int getHeight(RandomSource random, int n){
        return Math.max(random.nextInt(3), 1);
    }
}
