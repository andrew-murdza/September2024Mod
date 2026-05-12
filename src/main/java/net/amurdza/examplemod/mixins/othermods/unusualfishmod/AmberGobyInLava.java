package net.amurdza.examplemod.mixins.othermods.unusualfishmod;
import codyhuh.unusualfishmod.common.entity.AmberGoby;
import codyhuh.unusualfishmod.common.entity.util.goal.FollowSchoolLeaderGoal;
import com.scouter.netherdepthsupgrade.entity.ai.FishSwimGoal;
import net.amurdza.examplemod.lava_fish.LavaBoundPathNavigation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AmberGoby.class)
public abstract class AmberGobyInLava extends PathfinderMob {

    protected AmberGobyInLava(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true)
    private void aoemod$useLavaNavigation(
            Level pLevel,
            CallbackInfoReturnable<PathNavigation> cir
    ) {
        cir.setReturnValue(new LavaBoundPathNavigation(this, pLevel));
    }

    @Inject(method = "registerGoals", at = @At("HEAD"), cancellable = true)
    private void aoemod$replaceGoalsWithNetherFishGoals(CallbackInfo ci) {
        ci.cancel();

        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));

        this.goalSelector.addGoal(3, new FishSwimGoal(this));

        this.goalSelector.addGoal(4, new FollowSchoolLeaderGoal(this));
    }
}
