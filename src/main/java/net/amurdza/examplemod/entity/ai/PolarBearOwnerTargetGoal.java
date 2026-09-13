package net.amurdza.examplemod.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.PolarBear;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PolarBearOwnerTargetGoal extends TargetGoal {
    private final PolarBear polarBear;
    private final boolean ownerWasHurt;
    @Nullable
    private LivingEntity ownerTarget;
    private int timestamp;

    public PolarBearOwnerTargetGoal(PolarBear polarBear, boolean ownerWasHurt) {
        super(polarBear, false);
        this.polarBear = polarBear;
        this.ownerWasHurt = ownerWasHurt;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!(this.polarBear instanceof PolarBearTamingAccess taming)
                || !taming.aoe$isTame()
                || !(this.polarBear instanceof OwnableEntity ownable)) {
            return false;
        }

        LivingEntity owner = ownable.getOwner();
        if (owner == null) {
            return false;
        }

        this.ownerTarget = this.ownerWasHurt ? owner.getLastHurtByMob() : owner.getLastHurtMob();
        int currentTimestamp = this.ownerWasHurt
                ? owner.getLastHurtByMobTimestamp()
                : owner.getLastHurtMobTimestamp();
        return currentTimestamp != this.timestamp
                && this.canAttack(this.ownerTarget, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerTarget);
        if (this.polarBear instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            if (owner != null) {
                this.timestamp = this.ownerWasHurt
                        ? owner.getLastHurtByMobTimestamp()
                        : owner.getLastHurtMobTimestamp();
            }
        }
        super.start();
    }
}
