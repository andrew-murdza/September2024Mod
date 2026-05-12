package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.CrimsonshellSquid;
import net.amurdza.examplemod.lava_fish.BottomLavaStrollGoal;
import net.amurdza.examplemod.lava_fish.LavaBoundPathNavigation;
import net.amurdza.examplemod.lava_fish.RandomLavaSwimmingGoal;
import net.amurdza.examplemod.lava_fish.TryFindLavaGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(CrimsonshellSquid.class)
public abstract class CrimsonshellInLava extends PathfinderMob {

    protected CrimsonshellInLava(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
}
