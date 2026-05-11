package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.AmberGoby;
import codyhuh.unusualfishmod.common.entity.PinkfinIdol;
import net.amurdza.examplemod.lava_fish.BottomLavaStrollGoal;
import net.amurdza.examplemod.lava_fish.RandomLavaSwimmingGoal;
import net.amurdza.examplemod.lava_fish.TryFindLavaGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PinkfinIdol.class)
public abstract class PinkfinIdolInLava extends PathfinderMob {

    protected PinkfinIdolInLava(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void aoemod$lavaPathing(EntityType<?> type, Level level, CallbackInfo ci) {
        this.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"), remap = false)
    private void aoemod$replaceWaterGoalsWithLavaGoals(CallbackInfo ci) {
        this.goalSelector.removeAllGoals(goal ->
                goal instanceof TryFindWaterGoal
                        || goal instanceof RandomStrollGoal
        );

        this.goalSelector.addGoal(0, new TryFindLavaGoal(this));
        this.goalSelector.addGoal(2, new RandomLavaSwimmingGoal(this, 0.8D, 1));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8, 1) {
            public boolean canUse() {
                return !this.mob.isInLava() && super.canUse();
            }
        });
    }

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lcodyhuh/unusualfishmod/common/entity/PinkfinIdol;isInWater()Z"
            ),
            remap = false
    )
    private boolean aoemod$lavaPreventsFlopping(PinkfinIdol squid) {
        return squid.isInLava();
    }

    @Redirect(method = "predicate",at= @At(value = "INVOKE", target = "Lcodyhuh/unusualfishmod/common/entity/PinkfinIdol;isInWater()Z"))
    private boolean hi(PinkfinIdol instance){
        return instance.isInLava();
    }
}
