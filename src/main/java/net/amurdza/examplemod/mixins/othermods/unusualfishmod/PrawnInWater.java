package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.Prawn;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Prawn.class, remap = false)
public class PrawnInWater extends Monster {
    protected PrawnInWater(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("HEAD"), cancellable = true)
    private void aoemod$replacePrawnGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 0.5F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        ci.cancel();
    }

    @Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
    private void aoemod$removePrawnAmbientSound(CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(null);
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void aoemod$replacePrawnDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(AMSoundRegistry.LOBSTER_HURT.get());
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void aoemod$replacePrawnHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(AMSoundRegistry.LOBSTER_HURT.get());
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public @NotNull MobType getMobType() {
        return MobType.WATER;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public int getExperienceReward() {
        return 1 + this.level().random.nextInt(3);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    @Override
    public void baseTick() {
        int air = this.getAirSupply();
        super.baseTick();

        if (this.isAlive() && !this.isInWaterOrBubble()) {
            this.setAirSupply(air - 1);
            if (this.getAirSupply() == -20) {
                this.setAirSupply(0);
                this.hurt(this.damageSources().drown(), 2.0F);
            }
        } else {
            this.setAirSupply(300);
        }
    }
}
