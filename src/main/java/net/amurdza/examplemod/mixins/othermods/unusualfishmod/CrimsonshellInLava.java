package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.CrimsonshellSquid;
import com.scouter.netherdepthsupgrade.entity.ai.LavaBoundPathNavigation;
import net.amurdza.examplemod.lava_fish.LavaBottomStrollGoal;
import net.amurdza.examplemod.lava_fish.RandomLavaSwimmingGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = CrimsonshellSquid.class,remap = false)
public abstract class CrimsonshellInLava extends PathfinderMob {

    protected CrimsonshellInLava(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true)
    private void aoemod$useLavaNavigation(
            Level pLevel,
            CallbackInfoReturnable<PathNavigation> cir
    ) {
        cir.setReturnValue(new LavaBoundPathNavigation(this, pLevel));
    }

    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lcodyhuh/unusualfishmod/common/entity/CrimsonshellSquid;isInWater()Z"))
    private boolean hi(CrimsonshellSquid instance){
        return instance.isInLava();
    }

    @Redirect(method = "predicate",at= @At(value = "INVOKE", target = "Lcodyhuh/unusualfishmod/common/entity/CrimsonshellSquid;isInWater()Z"))
    private boolean hi1(CrimsonshellSquid instance){
        return instance.isInLava();
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void aoemod$replaceGoal(CallbackInfo ci) {

        this.goalSelector.addGoal(4, new RandomLavaSwimmingGoal(this, 1.0D, 40));

        this.goalSelector.addGoal(3, new LavaBottomStrollGoal(this, 0.800000011920929, 7));

        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8, 15) {
            public boolean canUse() {
                return !this.mob.isInLava() && super.canUse();
            }
        });
    }
}