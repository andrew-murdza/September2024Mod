package net.amurdza.examplemod.lava_fish;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class LavaFishMoveControl extends MoveControl {
    private final PathfinderMob fish;

    public LavaFishMoveControl(PathfinderMob pMob) {
        super(pMob);
        this.fish = pMob;
    }

    public void tick() {
        if (this.fish.isEyeInFluid(FluidTags.LAVA)) {
            this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(0.0, 0.005, 0.0));
        }

        if (this.operation == Operation.MOVE_TO && !this.fish.getNavigation().isDone()) {
            float f = (float)(this.speedModifier * this.fish.getAttributeValue(Attributes.MOVEMENT_SPEED));
            this.fish.setSpeed(Mth.lerp(0.125F, this.fish.getSpeed(), f));
            double d0 = this.wantedX - this.fish.getX();
            double d1 = this.wantedY - this.fish.getY();
            double d2 = this.wantedZ - this.fish.getZ();
            if (d1 != 0.0) {
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(0.0, (double)this.fish.getSpeed() * (d1 / d3) * 0.1, 0.0));
            }

            if (d0 != 0.0 || d2 != 0.0) {
                float f1 = (float)(Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F;
                this.fish.setYRot(this.rotlerp(this.fish.getYRot(), f1, 90.0F));
                this.fish.yBodyRot = this.fish.getYRot();
                this.fish.yHeadRot = this.fish.getYRot();
            }
        } else {
            this.fish.setSpeed(0.0F);
        }

    }
}