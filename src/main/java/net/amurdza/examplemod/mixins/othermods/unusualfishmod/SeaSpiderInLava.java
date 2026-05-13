package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.SeaSpider;
import net.amurdza.examplemod.lava_fish.LavaGroundPathNavigation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SeaSpider.class)
public abstract class SeaSpiderInLava extends PathfinderMob {

    protected SeaSpiderInLava(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true)
    private void aoemod$useLavaWalkNavigation(Level level, CallbackInfoReturnable<PathNavigation> cir) {
        cir.setReturnValue(new LavaGroundPathNavigation(this, level));
    }
}
