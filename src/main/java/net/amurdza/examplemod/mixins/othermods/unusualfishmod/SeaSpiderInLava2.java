package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "codyhuh.unusualfishmod.common.entity.SeaSpider$MoveHelperController",remap = false)
public abstract class SeaSpiderInLava2 {

    @Final
    @Shadow
    private Mob spider;
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"
            )
    )
    private boolean aoemod$checkLavaInstead(Mob instance, TagKey tagKey) {
        return spider.isEyeInFluid(FluidTags.LAVA);
    }
}
