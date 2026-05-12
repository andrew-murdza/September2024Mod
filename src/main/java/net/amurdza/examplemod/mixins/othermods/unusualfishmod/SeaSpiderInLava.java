package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.CrimsonshellSquid;
import codyhuh.unusualfishmod.common.entity.SeaSpider;
import codyhuh.unusualfishmod.common.entity.util.goal.BottomStrollGoal;
import net.amurdza.examplemod.lava_fish.BottomLavaStrollGoal;
import net.amurdza.examplemod.lava_fish.LavaBoundPathNavigation;
import net.amurdza.examplemod.lava_fish.RandomLavaSwimmingGoal;
import net.amurdza.examplemod.lava_fish.TryFindLavaGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SeaSpider.class)
public abstract class SeaSpiderInLava extends PathfinderMob {

    protected SeaSpiderInLava(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$useGroundNavigation(Level level, CallbackInfoReturnable<PathNavigation> cir) {
        SeaSpider self = (SeaSpider)(Object)this;
        cir.setReturnValue(new LavaBoundPathNavigation(self, level));
    }
}
