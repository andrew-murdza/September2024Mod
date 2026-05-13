package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.Prawn;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIFindWater;
import com.github.alexthe666.alexsmobs.entity.ai.BottomFeederAIWander;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
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

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aoemod$waterPathfinding(
            EntityType<? extends Monster> entityType,
            Level level,
            CallbackInfo ci
    ) {
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }

    @Inject(method = "canSpawn",at=@At(value = "HEAD"),cancellable = true)
    private static void hi(EntityType<Prawn> entityType, ServerLevelAccessor iServerWorld, MobSpawnType reason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(iServerWorld.getFluidState(pos).is(FluidTags.WATER) && iServerWorld.getBlockState(pos.above()).is(Blocks.WATER));
    }

    @Inject(method = "registerGoals", at = @At("HEAD"), cancellable = true)
    private void aoemod$replacePrawnGoals(CallbackInfo ci) {
        this.targetSelector.removeAllGoals(goal -> true);
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(1, new AnimalAIFindWater(this));
        this.goalSelector.addGoal(3, new BottomFeederAIWander(this, 1.0D, 10, 50));
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
        return MobType.ARTHROPOD;
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
