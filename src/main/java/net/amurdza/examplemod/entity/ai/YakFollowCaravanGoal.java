package net.amurdza.examplemod.entity.ai;

import net.amurdza.examplemod.entity.YakEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.Comparator;
import java.util.EnumSet;

/** Like llamas, a leashed head can lead an eight-animal train; a ridden head also works. */
public final class YakFollowCaravanGoal extends Goal {
    private final YakEntity yak;
    private YakEntity leader;
    public YakFollowCaravanGoal(YakEntity yak) { this.yak = yak; setFlags(EnumSet.of(Flag.MOVE)); }
    @Override public boolean canUse() {
        if (yak.isLeashed() || yak.isVehicle()) return false;
        leader = yak.level().getEntitiesOfClass(YakEntity.class, yak.getBoundingBox().inflate(9, 4, 9),
                other -> other != yak && other.caravanTail() == null && anchored(other)
                        && yak.distanceToSqr(other) >= 4).stream()
                .min(Comparator.comparingDouble(yak::distanceToSqr)).orElse(null);
        return leader != null;
    }
    private boolean anchored(YakEntity current) {
        for (int depth = 0; depth < 8 && current != null && current.isAlive() && current != yak; depth++) {
            if (current.isLeashed() || current.isVehicle()) return true;
            current = current.caravanHead();
        }
        return false;
    }
    @Override public void start() { yak.joinCaravan(leader); }
    @Override public boolean canContinueToUse() {
        return !yak.isLeashed() && !yak.isVehicle() && anchored(leader) && yak.distanceToSqr(leader) < 32 * 32;
    }
    @Override public void tick() {
        var delta = leader.position().subtract(yak.position());
        var target = leader.position().subtract(delta.normalize().scale(Math.min(2, delta.length())));
        yak.getNavigation().moveTo(target.x, target.y, target.z, 1.15);
    }
    @Override public void stop() { yak.leaveCaravan(); leader = null; yak.getNavigation().stop(); }
}
