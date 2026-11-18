package net.amurdza.examplemod.entity.ai;

import net.amurdza.examplemod.event_handlers.ColdCowYaks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;

import java.util.Comparator;
import java.util.EnumSet;

public final class YakCaravanGoal extends Goal {
    private final Cow follower;
    private final YakAccess yak;
    private Cow leader;

    public YakCaravanGoal(Cow follower, YakAccess yak) {
        this.follower = follower;
        this.yak = yak;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!ColdCowYaks.isColdCow(follower) || follower.isLeashed() || follower.isVehicle() || yak.aoe$yakStaying()) return false;
        leader = follower.level().getEntitiesOfClass(Cow.class, follower.getBoundingBox().inflate(9, 4, 9),
                cow -> cow != follower && ColdCowYaks.isColdCow(cow) && cow instanceof YakAccess candidate
                        && candidate.aoe$caravanTail() == null && leadsCaravan(cow) && follower.distanceToSqr(cow) >= 4)
                .stream().min(Comparator.comparingDouble(cow -> follower.distanceToSqr(cow))).orElse(null);
        return leader != null;
    }

    // A ridden or leashed yak anchors a single-file train of up to eight followers.
    private boolean leadsCaravan(Cow cow) {
        for (int n = 0; n < 8 && cow != null && cow.isAlive() && cow != follower; n++) {
            if (cow.isLeashed() || cow.isVehicle()) return true;
            cow = ((YakAccess) cow).aoe$caravanHead();
        }
        return false;
    }

    @Override public void start() {
        yak.aoe$setCaravanHead(leader);
        ((YakAccess) leader).aoe$setCaravanTail(follower);
    }

    @Override public boolean canContinueToUse() {
        return !yak.aoe$yakStaying() && !follower.isVehicle() && !follower.isLeashed()
                && leadsCaravan(leader) && follower.distanceToSqr(leader) < 32 * 32;
    }

    @Override public void tick() {
        var offset = leader.position().subtract(follower.position());
        var target = leader.position().subtract(offset.normalize().scale(Math.min(2, offset.length())));
        follower.getNavigation().moveTo(target.x, target.y, target.z, 1.15);
    }

    @Override public void stop() {
        if (leader != null && ((YakAccess) leader).aoe$caravanTail() == follower) {
            ((YakAccess) leader).aoe$setCaravanTail(null);
        }
        yak.aoe$setCaravanHead(null);
        leader = null;
        follower.getNavigation().stop();
    }
}
