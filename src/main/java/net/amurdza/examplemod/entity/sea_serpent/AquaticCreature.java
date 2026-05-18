package net.amurdza.examplemod.entity.sea_serpent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class AquaticCreature extends WaterAnimal implements IEntityAdditionalSpawnData {
    public int randNumTick;
    protected float currentYaw;
    protected float currentPitch;
    protected float prevCurrentYaw;
    protected float prevCurrentPitch;
    protected double netSpeed;
    protected double dPosX;
    protected double dPosY;
    protected double dPosZ;
    protected Vec3 targetVec;
    private float animationSpeed;
    private float animationSpeedOld;

    public float limbSwing;
    public float limbSwingAmount;
    public float prevLimbSwingAmount;

    protected AquaticCreature(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        this.randNumTick = this.random.nextInt(100);
    }

    public float getCurrentPitch(float partialTicks) {
        return this.prevCurrentPitch + (this.currentPitch - this.prevCurrentPitch) * partialTicks;
    }

    public boolean onLand() {
        return !this.isInWater() && this.onGround();
    }

    public boolean getTamed() {
        return false;
    }

    public boolean getRotatePitch() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.setYRot(this.yBodyRot = this.yHeadRot = Mth.wrapDegrees(this.yHeadRot));

        if (this.dPosX * this.dPosX + this.dPosY * this.dPosY + this.dPosZ * this.dPosZ < 1.6000001778593287E-5D) {
            this.setXRot(0.0F);
        }

        this.currentYaw = wrapAngleAround(this.currentYaw, this.getYRot());
        this.currentPitch = wrapAngleAround(this.currentPitch, this.getXRot());
        this.prevCurrentYaw = this.currentYaw;
        this.prevCurrentPitch = this.currentPitch;
        this.prevCurrentYaw = wrapAngleAround(this.prevCurrentYaw, this.currentYaw);
        this.prevCurrentPitch = wrapAngleAround(this.prevCurrentPitch, this.currentPitch);
        this.currentYaw += (this.getYRot() - this.currentYaw) * 0.6F;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.moveCreature();
    }

    /** Server side only */
    protected abstract void moveCreature();

    public boolean findNewPath() {
        return this.random.nextInt(70) == 0 || this.onLand() && this.random.nextInt(10) == 0;
    }

    protected double addPathY() {
        return this.getEyeHeight();
    }

    public boolean setRandomPath() {
        double x = this.getX() + (3.0D + (double)this.random.nextFloat() * 3.0D) * (double)(this.random.nextBoolean() ? 1 : -1);
        double y = this.getY() + ((double)this.random.nextFloat() - 0.5D) * 6.0D + this.addPathY();
        double z = this.getZ() + (3.0D + (double)this.random.nextFloat() * 3.0D) * (double)(this.random.nextBoolean() ? 1 : -1);

        if (this.onLand()) {
            x = this.getX() + (2.0D + (double)this.random.nextFloat() * 8.0D) * (double)(this.random.nextBoolean() ? 1 : -1);
            z = this.getZ() + (2.0D + (double)this.random.nextFloat() * 8.0D) * (double)(this.random.nextBoolean() ? 1 : -1);
        }

        if (this.isClearPath(x, y, z)) {
            this.targetVec = new Vec3(x, y, z);
            return true;
        }
        return false;
    }

    public boolean isClearPath(double x, double y, double z) {
        boolean water = this.getPathingFluid(this.level().getFluidState(BlockPos.containing((int)x, (int)y, (int)z)));
        boolean seen = this.level().clip(new ClipContext(
                this.getEyePosition(1.0F),
                new Vec3(x, y, z),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        )).getType() == HitResult.Type.MISS;
        return water && seen;
    }

    public boolean isClearPathWaterBelow(double x, double y, double z) {
        boolean water = this.getPathingFluid(this.level().getFluidState(BlockPos.containing((int)x, (int)y, (int)z)));
        boolean waterBelow = this.getPathingFluid(this.level().getFluidState(BlockPos.containing((int)x, (int)y - 1, (int)z)));
        boolean seen = this.level().clip(new ClipContext(
                this.getEyePosition(1.0F),
                new Vec3(x, y, z),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        )).getType() == HitResult.Type.MISS;

        return (water || waterBelow) && seen;
    }

    public boolean getPathingFluid(FluidState state) {
        return state.is(FluidTags.WATER);
    }

    @Override
    public void travel(Vec3 travelVector) {
        this.travelOld((float)travelVector.x, (float)travelVector.y, (float)travelVector.z);
    }

    public void travelOld(float strafe, float vertical, float forward) {
        double posY;

        if (this.isInWater()) {
            posY = this.getY();

            this.moveRelative(0.04F, new Vec3(strafe, vertical, forward));
            this.move(MoverType.SELF, this.getDeltaMovement());

            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.84D,
                    this.getDeltaMovement().y * 0.84D,
                    this.getDeltaMovement().z * 0.84D
            );

            if (this.horizontalCollision && this.isOffsetPositionInLiquid(
                    this.getDeltaMovement().x,
                    this.getDeltaMovement().y + 0.6000000238418579D - this.getY() + posY,
                    this.getDeltaMovement().z
            )) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.30000001192092896D, this.getDeltaMovement().z);
            }
        } else if (this.isInLava()) {
            posY = this.getY();

            this.moveRelative(0.02F, new Vec3(strafe, vertical, forward));
            this.move(MoverType.SELF, this.getDeltaMovement());

            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.5D,
                    this.getDeltaMovement().y * 0.5D,
                    this.getDeltaMovement().z * 0.5D
            );

            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().x, this.getDeltaMovement().y - 0.02D, this.getDeltaMovement().z);
            }

            if (this.horizontalCollision && this.isOffsetPositionInLiquid(
                    this.getDeltaMovement().x,
                    this.getDeltaMovement().y + 0.6000000238418579D - this.getY() + posY,
                    this.getDeltaMovement().z
            )) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.30000001192092896D, this.getDeltaMovement().z);
            }
        } else {
            float f2 = 0.91F;
            BlockPos.MutableBlockPos blockpos = new BlockPos.MutableBlockPos(this.getX(), this.getBoundingBox().minY - 1.0D, this.getZ());

            if (this.onGround()) {
                BlockState underState = this.level().getBlockState(blockpos);
                f2 = underState.getBlock().getFriction(underState, this.level(), blockpos, this) * 0.91F;
            }

            float f3 = 0.16277136F / (f2 * f2 * f2);
            float friction;

            if (this.onGround()) {
                friction = this.getSpeed() * f3;
            } else {
                friction = this.getFlyingSpeed();
            }

            this.moveRelative(friction, new Vec3(strafe, vertical, forward));

            f2 = 0.91F;

            if (this.onGround()) {
                BlockState underState = this.level().getBlockState(blockpos);
                f2 = underState.getBlock().getFriction(underState, this.level(), blockpos, this) * 0.91F;
                f2 *= 0.1F;
            }

            if (this.onClimbable()) {
                this.setDeltaMovement(
                        Mth.clamp(this.getDeltaMovement().x, -0.15000000596046448D, 0.15000000596046448D),
                        this.getDeltaMovement().y,
                        Mth.clamp(this.getDeltaMovement().z, -0.15000000596046448D, 0.15000000596046448D)
                );

                this.fallDistance = 0.0F;

                if (this.getDeltaMovement().y < -0.15D) {
                    this.setDeltaMovement(this.getDeltaMovement().x, -0.15D, this.getDeltaMovement().z);
                }
            }

            this.move(MoverType.SELF, this.getDeltaMovement());

            if (this.horizontalCollision && this.onClimbable()) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.2D, this.getDeltaMovement().z);
            }

            if (this.hasEffect(MobEffects.LEVITATION)) {
                this.setDeltaMovement(
                        this.getDeltaMovement().x,
                        this.getDeltaMovement().y + (0.05D * (double)(Objects.requireNonNull(this.getEffect(MobEffects.LEVITATION)).getAmplifier() + 1) - this.getDeltaMovement().y) * 0.2D,
                        this.getDeltaMovement().z
                );
            } else {
                blockpos.set(this.getX(), 0.0D, this.getZ());

                if (!this.level().isClientSide || this.level().hasChunkAt(blockpos)) {
                    if (!this.isNoGravity()) {
                        this.setDeltaMovement(this.getDeltaMovement().x, this.getDeltaMovement().y - 0.08D, this.getDeltaMovement().z);
                    }
                } else if (this.getY() > 0.0D) {
                    this.setDeltaMovement(this.getDeltaMovement().x, -0.1D, this.getDeltaMovement().z);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().x, 0.0D, this.getDeltaMovement().z);
                }
            }

            f2 *= 0.4F;

            this.setDeltaMovement(
                    this.getDeltaMovement().x * (double)f2,
                    this.getDeltaMovement().y * 0.9800000190734863D,
                    this.getDeltaMovement().z * (double)f2
            );
        }

        this.handleLimbSwing();
    }

    protected boolean isOffsetPositionInLiquid(double x, double y, double z) {
        return this.isFree(x, y, z);
    }

    protected void handleLimbSwing() {
        this.prevLimbSwingAmount = this.limbSwingAmount;

        double x = this.getX() - this.xo;
        double y = this.getY() - this.yo;
        double z = this.getZ() - this.zo;

        if (!this.level().isClientSide) {
            x = this.dPosX;
            y = this.dPosY;
            z = this.dPosZ;
        }

        float f6 = Mth.sqrt((float)(x * x + y * y + z * z)) * 4.0F;

        if (f6 > 1.0F) {
            f6 = 1.0F;
        }

        if (!this.isInWater() && !this.onGround()) {
            this.limbSwingAmount *= 0.4F;
        } else {
            float delta = f6 - this.limbSwingAmount;

            if (delta >= 0.0F) {
                this.limbSwingAmount += delta * 0.4F;
            } else {
                this.limbSwingAmount += delta * 0.1F;
            }
        }

        this.limbSwing += this.limbSwingAmount;
    }

    @Override
    public void knockback(double strength, double xRatio, double zRatio) {
        double xx = this.getDeltaMovement().x;
        double yy = this.getDeltaMovement().y;
        double zz = this.getDeltaMovement().z;

        super.knockback(strength, xRatio, zRatio);

        if (this.isInWater()) {
            this.setDeltaMovement(
                    this.getDeltaMovement().x + (xx - this.getDeltaMovement().x) * 0.5D,
                    this.getDeltaMovement().y + (yy - this.getDeltaMovement().y) * 0.800000011920929D,
                    this.getDeltaMovement().z + (zz - this.getDeltaMovement().z) * 0.5D
            );
        } else {
            this.setDeltaMovement(
                    this.getDeltaMovement().x,
                    this.getDeltaMovement().y + (yy - this.getDeltaMovement().y) * 0.4000000059604645D,
                    this.getDeltaMovement().z
            );
        }
    }

    @Override
    public void makeStuckInBlock(@NotNull BlockState state, @NotNull Vec3 motionMultiplier) {
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.randNumTick);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.randNumTick = additionalData.readInt();
    }

    @Override
    protected @NotNull ResourceLocation getDefaultLootTable() {
        ResourceLocation entityId = EntityType.getKey(this.getType());
        return new ResourceLocation(entityId.getNamespace(), "entities/" + entityId.getPath());
    }

    protected static float wrapAngleAround(float value, float target) {
        return target + Mth.wrapDegrees(value - target);
    }
}