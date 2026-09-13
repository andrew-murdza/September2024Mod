package net.amurdza.examplemod.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.PolarBear;

public class PolarBearOwnerMeleeAttackGoal extends MeleeAttackGoal {
    private final PolarBear polarBear;

    public PolarBearOwnerMeleeAttackGoal(PolarBear polarBear, double speedModifier) {
        super(polarBear, speedModifier, true);
        this.polarBear = polarBear;
    }

    @Override
    public boolean canUse() {
        if (!this.polarBear.isVehicle()) {
            return super.canUse();
        }

        LivingEntity target = this.polarBear.getTarget();
        return this.isMountedTargetInReach(target);
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.polarBear.isVehicle()) {
            return super.canContinueToUse();
        }

        return this.isMountedTargetInReach(this.polarBear.getTarget());
    }

    @Override
    public void start() {
        if (!this.polarBear.isVehicle()) {
            super.start();
            return;
        }

        this.polarBear.getNavigation().stop();
        this.polarBear.setAggressive(true);
    }

    @Override
    public void tick() {
        if (!this.polarBear.isVehicle()) {
            super.tick();
            return;
        }

        LivingEntity target = this.polarBear.getTarget();
        this.polarBear.getNavigation().stop();
        if (target != null) {
            this.polarBear.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.checkAndPerformAttack(target, this.polarBear.distanceToSqr(target));
        }
    }

    private boolean isMountedTargetInReach(LivingEntity target) {
        return target != null
                && target.isAlive()
                && this.polarBear.canAttack(target)
                && this.polarBear.distanceToSqr(target) <= this.getAttackReachSqr(target);
    }
}
