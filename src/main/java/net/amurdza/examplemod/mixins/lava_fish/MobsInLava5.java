package net.amurdza.examplemod.mixins.lava_fish;

import com.scouter.netherdepthsupgrade.entity.ai.LavaBoundPathNavigation;
import net.amurdza.examplemod.lava_fish.LavaFishMoveControl;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.amurdza.examplemod.lava_fish.LavaSwimmingGoal;
import net.amurdza.examplemod.lava_fish.RandomLavaSwimmingGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFish.class)
public abstract class MobsInLava5 extends WaterAnimal {


    protected MobsInLava5(EntityType<? extends WaterAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hi(EntityType<? extends AbstractFish> pEntityType, Level pLevel, CallbackInfo ci) {
        if (LavaMobs.isLavaMob(pEntityType)) {
            AbstractFish fish = (AbstractFish)(Object)this;
            this.moveControl = new LavaFishMoveControl(fish);
        }
    }

    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true)
    private void aoemod$useLavaNavigation(Level level, CallbackInfoReturnable<PathNavigation> cir) {
        AbstractFish fish = (AbstractFish) (Object) this;
        if (LavaMobs.isLavaMob(fish)) {
            cir.setReturnValue(new LavaBoundPathNavigation(fish, level));
        }
    }

    @Redirect(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/AbstractFish;isInWater()Z"
            )
    )
    private boolean aoemod$treatLavaAsWaterForTravel(AbstractFish fish) {
        return LavaMobs.isLavaMob(fish)? fish.isInLava(): fish.isInWater();
    }



    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/AbstractFish;isInWater()Z"
            )
    )
    private boolean aoemod$doNotFlopInLava(AbstractFish fish) {
        return LavaMobs.isLavaMob(fish)? fish.isInLava(): fish.isInWater();
    }

    @Inject(method = "registerGoals", at = @At("HEAD"), cancellable = true)
    private void hi(CallbackInfo ci) {
        AbstractFish fish = (AbstractFish)(Object)this;

        if (LavaMobs.isLavaMob(fish)) {
            super.registerGoals();
            this.goalSelector.addGoal(4, new LavaSwimmingGoal(fish));
            ci.cancel();
        }
    }

}
