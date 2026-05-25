package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.world.FeatureLeafcutterAnthill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = FeatureLeafcutterAnthill.class, remap = false)
public class AntHillChance {
    @ModifyConstant(
            method = "place",
            constant = @Constant(floatValue = 0.0175F)
    )
    private float aoemod$increaseAnthillChance(float original) {
        return 1F;
    }
}
