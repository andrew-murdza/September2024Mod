package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.treedecorators.GlowBerryDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlowBerryDecorator.class)
public class CorrectCaveVinesForTreeGen {
    @Redirect(method = "place", at=@At(value = "INVOKE", target = "Lnet/minecraft/util/valueproviders/IntProvider;sample(Lnet/minecraft/util/RandomSource;)I"),remap = false)
    private int hi(IntProvider instance, RandomSource randomSource){
        return 0;
    }
}
