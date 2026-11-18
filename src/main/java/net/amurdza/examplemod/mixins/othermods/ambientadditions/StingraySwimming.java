package net.amurdza.examplemod.mixins.othermods.ambientadditions;

import codyhuh.ambientadditions.common.entities.BlueSpottedStingray;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retain native fish goals while allowing their destinations throughout the water column. */
@Mixin(BlueSpottedStingray.class)
public abstract class StingraySwimming extends AbstractFish {
    protected StingraySwimming(EntityType<? extends AbstractFish> type, Level level) { super(type, level); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$swimAboveTheBottom(EntityType<? extends AbstractFish> type, Level level, CallbackInfo ci) {
        navigation = new WaterBoundPathNavigation(this, level);
        moveControl = new SmoothSwimmingMoveControl(this, 85, 10, .02F, .1F, true);
    }
}
