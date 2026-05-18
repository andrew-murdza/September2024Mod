package net.amurdza.examplemod.entity.sea_serpent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public abstract class ComplexCreature extends AquaticCreature {
    private static final EntityDataAccessor<Integer> TICKS_EXISTED =
            SynchedEntityData.defineId(ComplexCreature.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> LIMB_SWING =
            SynchedEntityData.defineId(ComplexCreature.class, EntityDataSerializers.FLOAT);

    protected int moveTick;

    protected ComplexCreature(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICKS_EXISTED, 0);
        this.entityData.define(LIMB_SWING, 0.0F);
    }

    @Override
    public boolean isInWater() {
        AABB box = this.getBoundingBox().inflate(0.0D, -0.3D, 0.0D);

        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    pos.set(x, y, z);
                    if (this.level().getFluidState(pos).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void tick() {
        double prevX = this.getX();
        double prevY = this.getY();
        double prevZ = this.getZ();

        super.tick();

        if (!this.level().isClientSide) {
            this.entityData.set(TICKS_EXISTED, this.tickCount);
            this.entityData.set(LIMB_SWING, this.walkAnimation.position());
        } else {
            this.tickCount = this.entityData.get(TICKS_EXISTED);
        }

        this.dPosX = this.getX() - prevX;
        this.dPosY = this.getY() - prevY;
        this.dPosZ = this.getZ() - prevZ;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.updateSwingTime();

        if (!this.isInWater()) {
            if (this.onGround()) {
                this.currentPitch *= 0.6F;
            } else {
                double d = Mth.sqrt((float)(this.dPosX * this.dPosX + this.dPosZ * this.dPosZ));
                float pitch = -((float)Math.atan2(this.dPosY, d)) * 57.295776F;
                this.currentPitch += (pitch - this.currentPitch) * 0.25F;
            }
        } else {
            this.currentPitch += (this.getXRot() - this.currentPitch) * 0.1F;
        }

        this.updateParts();
    }

    @Override
    protected void moveCreature() {
        double targetSpeed = 0.0D;

        if (this.targetVec != null) {
            ++this.moveTick;
            targetSpeed = this.moveByPathing();
        } else if (this.findNewPath() && this.tickCount > 20) {
            this.setRandomPath();
        }

        Vec3 vec = this.getLookAngle();

        this.netSpeed += (targetSpeed - this.netSpeed) * 0.1D;

        float dYaw = Math.abs(this.currentYaw - this.prevCurrentYaw);
        if (dYaw > 0.02F && this.netSpeed > targetSpeed * 0.6D) {
            this.netSpeed += (targetSpeed * 0.6D - this.netSpeed) * 0.4D;
        }

        if (this.onLand()) {
            this.netSpeed = 0.0D;

            if (this.targetVec != null && this.random.nextInt(12) == 0) {
                this.setDeltaMovement(
                        vec.x * 0.25D,
                        0.20000000298023224D,
                        vec.z * 0.25D
                );
            }
        }

        this.move(
                MoverType.SELF,
                new Vec3(vec.x * this.netSpeed, vec.y * this.netSpeed, vec.z * this.netSpeed)
        );
    }

    public double moveByPathing() {
        this.getLookControl().setLookAt(this.targetVec.x, this.targetVec.y, this.targetVec.z, 3.0F, 85.0F);

        if (this.horizontalCollision && this.getDeltaMovement().y < 0.20000000298023224D) {
            this.setDeltaMovement(
                    this.getDeltaMovement().x,
                    0.20000000298023224D,
                    this.getDeltaMovement().z
            );
        }

        if (this.targetVec.distanceTo(this.position()) < 3.0D || this.moveTick > 120) {
            this.moveTick = 0;

            if (this.random.nextInt(5) < 3) {
                this.setRandomPath();
            } else {
                this.targetVec = null;
            }
        }

        return this.getMovementSpeed();
    }

    public final double getMovementSpeed() {
        return this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.8D;
    }

    public abstract BodyPart[] getParts();

    public abstract Bone getBaseBone();

    public abstract Bone[] getBoneList();

    protected void updateParts() {
        this.getBaseBone().setRotation(this.currentPitch, this.getYRot(), 0.0F);
        this.resetBoneAngles();
        this.updatePitchRotations(1.0F);
        this.updateYawRotations(1.0F);
        this.setBodyPartPositions();
    }

    public void updatePitchRotations(float partialTick) {
    }

    public void updateYawRotations(float partialTick) {
    }

    private void setBodyPartPositions() {
        BodyPart[] partList = this.getParts();
        Bone baseBone = this.getBaseBone();
        Bone[] boneList = this.getBoneList();

        Vec3 vec = boneList[0]
                .getRotation()
                .getRotated(baseBone.getRotation())
                .rotateVector(boneList[0].getLength() / 2.0F);

        partList[0].setPos(this.getX() + vec.x, this.getY() + vec.y, this.getZ() + vec.z);
        partList[0].setYRot(this.getYRot());
        partList[0].setXRot(this.getXRot());

        vec = baseBone.getRotatedVector();
        Euler angle = baseBone.getRotation();

        for (int i = 1; i < partList.length; ++i) {
            angle = angle.getRotated(boneList[i].getRotation());

            float length = boneList[i].getLength();
            Vec3 midVec = angle.rotateVector(length / 2.0F);

            partList[i].setPos(this.getX() + vec.x + midVec.x, this.getY() + vec.y + midVec.y, this.getZ() + vec.z + midVec.z);
            partList[i].setYRot(this.getYRot());
            partList[i].setXRot(this.getXRot());

            Vec3 target = angle.rotateVector(length);
            vec = vec.add(target.x, target.y, target.z);
        }
    }

    public void resetBoneAngles() {
        for (Bone bone : this.getBoneList()) {
            bone.setRotation(0.0F, 0.0F, 0.0F);
        }
    }

    public void collideWithEntity(BodyPart part, Entity entity) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if ("arrow".equals(source.getMsgId())) {
            amount *= 0.25F;
        }

        return super.hurt(source, amount);
    }

    public boolean hurtPart(BodyPart part, DamageSource source, float damage) {
        return this.hurt(source, damage * 0.85F);
    }

    @Override
    public void remove(Entity.@NotNull RemovalReason reason) {
        super.remove(reason);

        for (BodyPart bodyPart : this.getParts()) {
            bodyPart.remove(reason);
        }
    }
}