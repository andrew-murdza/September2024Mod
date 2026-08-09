package net.amurdza.examplemod.entity.ai;

import net.amurdza.examplemod.mixins.accessor.SimpleCriterionTriggerInvoker;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.BredAnimalsTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.stats.Stats;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

public final class DolphinBreedGoal extends Goal {
    private static final double MATE_SEARCH_DISTANCE = 8.0D;
    private static final double BREEDING_DISTANCE_SQUARED = 9.0D;
    private static final int BREEDING_TIME = 60;

    private final Dolphin dolphin;
    private final DolphinBreedingAccess breeding;
    private final double speedModifier;
    @Nullable
    private Dolphin mate;
    private int loveTime;

    public DolphinBreedGoal(Dolphin dolphin, double speedModifier) {
        this.dolphin = dolphin;
        this.breeding = (DolphinBreedingAccess) dolphin;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.breeding.aoe$isInLove() || this.breeding.aoe$getAge() != 0) {
            return false;
        }

        this.mate = this.dolphin.level().getEntitiesOfClass(
                        Dolphin.class,
                        this.dolphin.getBoundingBox().inflate(MATE_SEARCH_DISTANCE),
                        this.breeding::aoe$canMate
                ).stream()
                .min(Comparator.comparingDouble(this.dolphin::distanceToSqr))
                .orElse(null);
        return this.mate != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.mate != null
                && this.mate.isAlive()
                && this.breeding.aoe$canMate(this.mate)
                && this.loveTime < BREEDING_TIME;
    }

    @Override
    public void start() {
        this.loveTime = 0;
    }

    @Override
    public void stop() {
        this.mate = null;
        this.loveTime = 0;
        this.dolphin.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.mate == null) {
            return;
        }

        this.dolphin.getLookControl().setLookAt(this.mate, 10.0F, 10.0F);
        this.dolphin.getNavigation().moveTo(this.mate, this.speedModifier);
        ++this.loveTime;
        if (this.loveTime >= BREEDING_TIME
                && this.dolphin.distanceToSqr(this.mate) < BREEDING_DISTANCE_SQUARED
                && this.breeding.aoe$canMate(this.mate)
                && this.dolphin.level() instanceof ServerLevel serverLevel) {
            this.spawnBaby(serverLevel, this.mate);
        }
    }

    private void spawnBaby(ServerLevel level, Dolphin otherParent) {
        DolphinBreedingAccess otherBreeding = (DolphinBreedingAccess) otherParent;
        Dolphin baby = EntityType.DOLPHIN.create(level);
        if (baby == null) {
            return;
        }

        DolphinBreedingAccess babyBreeding = (DolphinBreedingAccess) baby;
        ServerPlayer breeder = this.breeding.aoe$getLoveCause();
        if (breeder == null) {
            breeder = otherBreeding.aoe$getLoveCause();
        }

        babyBreeding.aoe$setAge(DolphinBreedingAccess.BABY_START_AGE);
        baby.moveTo(this.dolphin.getX(), this.dolphin.getY(), this.dolphin.getZ(), 0.0F, 0.0F);
        if (breeder != null) {
            this.awardBreedingCredit(breeder, otherParent, baby);
        }

        this.breeding.aoe$setAge(DolphinBreedingAccess.BREEDING_COOLDOWN);
        otherBreeding.aoe$setAge(DolphinBreedingAccess.BREEDING_COOLDOWN);
        this.breeding.aoe$resetLove();
        otherBreeding.aoe$resetLove();

        level.addFreshEntityWithPassengers(baby);
        level.broadcastEntityEvent(this.dolphin, (byte) 18);
        level.broadcastEntityEvent(otherParent, (byte) 18);

        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(
                    level,
                    this.dolphin.getX(),
                    this.dolphin.getY(),
                    this.dolphin.getZ(),
                    this.dolphin.getRandom().nextInt(7) + 1
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private void awardBreedingCredit(ServerPlayer player, Dolphin otherParent, Dolphin baby) {
        player.awardStat(Stats.ANIMALS_BRED);

        LootContext parentContext = EntityPredicate.createContext(player, this.dolphin);
        LootContext partnerContext = EntityPredicate.createContext(player, otherParent);
        LootContext childContext = EntityPredicate.createContext(player, baby);
        ((SimpleCriterionTriggerInvoker<BredAnimalsTrigger.TriggerInstance>) (Object) CriteriaTriggers.BRED_ANIMALS)
                .aoe$trigger(
                        player,
                        instance -> instance.matches(parentContext, partnerContext, childContext)
                );
    }
}
