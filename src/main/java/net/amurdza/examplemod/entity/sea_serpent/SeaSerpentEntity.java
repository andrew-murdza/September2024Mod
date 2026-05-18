package net.amurdza.examplemod.entity.sea_serpent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SeaSerpentEntity extends WaterAnimal implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> TICKS_EXISTED =
            SynchedEntityData.defineId(SeaSerpentEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> LIMB_SWING =
            SynchedEntityData.defineId(SeaSerpentEntity.class, EntityDataSerializers.FLOAT);

    public static final int[] PART_HEIGHT = new int[] {8, 14, 12, 10, 8, 6, 5, 4};
    public static final int[] PART_LENGTH = new int[] {18, 24, 24, 20, 14, 16, 18, 22};

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
    protected int moveTick;

    public float limbSwing;
    public float limbSwingAmount;
    public float prevLimbSwingAmount;

    private final BodyPart[] partList = new BodyPart[8];
    private final Bone baseBone = new Bone();
    private final Bone[] boneList = new Bone[8];
    private final Euler[] targetAngles = new Euler[8];

    public SeaSerpentEntity(EntityType<? extends SeaSerpentEntity> type, Level level) {
        super(type, level);

        this.randNumTick = this.random.nextInt(100);
        this.noCulling = true;

        this.baseBone.setLength(0.0F);

        this.boneList[0] = new Bone(this.baseBone);
        this.boneList[0].setLength((float) PART_LENGTH[0] / 16.0F);

        for (int i = 1; i < this.boneList.length; ++i) {
            this.boneList[i] = new Bone(i == 1 ? this.baseBone : this.boneList[i - 1]);
            this.boneList[i].setLength((float) (-PART_LENGTH[i]) / 16.0F);
        }

        for (int i = 0; i < this.partList.length; ++i) {
            this.targetAngles[i] = new Euler();
            this.partList[i] = new BodyPart(this, (float) PART_LENGTH[i] / 16.0F);
        }

        this.updateParts();
    }

    public static Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICKS_EXISTED, 0);
        this.entityData.define(LIMB_SWING, 0.0F);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    @Override
    public int getAirSupply() {
        return 300;
    }

    public BodyPart[] getParts() {
        return this.partList;
    }

    public Bone getBaseBone() {
        return this.baseBone;
    }

    public Bone[] getBoneList() {
        return this.boneList;
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

        this.updateSwingTime();

        if (!this.isInWater()) {
            if (this.onGround()) {
                this.currentPitch *= 0.6F;
            } else {
                double d = Mth.sqrt((float) (this.dPosX * this.dPosX + this.dPosZ * this.dPosZ));
                float pitch = -((float) Math.atan2(this.dPosY, d)) * 57.295776F;
                this.currentPitch += (pitch - this.currentPitch) * 0.25F;
            }
        } else {
            this.currentPitch += (this.getXRot() - this.currentPitch) * 0.1F;
        }

        this.updateParts();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.moveCreature();
    }

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
                this.setDeltaMovement(vec.x * 0.25D, 0.20000000298023224D, vec.z * 0.25D);
            }
        }

        this.move(MoverType.SELF, new Vec3(vec.x * this.netSpeed, vec.y * this.netSpeed, vec.z * this.netSpeed));
    }

    public double moveByPathing() {
        this.getLookControl().setLookAt(this.targetVec.x, this.targetVec.y, this.targetVec.z, 3.0F, 85.0F);

        if (this.horizontalCollision && this.getDeltaMovement().y < 0.20000000298023224D) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.20000000298023224D, this.getDeltaMovement().z);
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

    public double getMovementSpeed() {
        return this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.8D;
    }

    public boolean findNewPath() {
        return this.random.nextInt(40) == 0 || this.onLand() && this.random.nextInt(4) == 0;
    }

    protected double addPathY() {
        return this.getEyeHeight();
    }

    public boolean setRandomPath() {
        double x = this.getX() + (10.0D + (double)this.random.nextFloat() * 12.0D) * (double)(this.random.nextBoolean() ? 1 : -1);
        double y = this.getY() + ((double)this.random.nextFloat() - 0.5D) * 12.0D;
        double z = this.getZ() + (10.0D + (double)this.random.nextFloat() * 12.0D) * (double)(this.random.nextBoolean() ? 1 : -1);

        if (this.onLand()) {
            x = this.getX() + (4.0D + this.random.nextFloat() * 16.0D) * (this.random.nextBoolean() ? 1 : -1);
            z = this.getZ() + (4.0D + this.random.nextFloat() * 16.0D) * (this.random.nextBoolean() ? 1 : -1);
        }

        if (this.isClearPath(x, y, z)) {
            this.targetVec = new Vec3(x, y, z);
            return true;
        }

        return false;
    }

    public boolean isClearPath(double x, double y, double z) {
        boolean water = this.getPathingFluid(this.level().getFluidState(BlockPos.containing((int) x, (int) y, (int) z)));
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
        boolean water = this.getPathingFluid(this.level().getFluidState(BlockPos.containing((int) x, (int) y, (int) z)));
        boolean waterBelow = this.getPathingFluid(this.level().getFluidState(BlockPos.containing((int) x, (int) y - 1, (int) z)));
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
        this.travelOld((float) travelVector.x, (float) travelVector.y, (float) travelVector.z);
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
            float friction = this.onGround() ? this.getSpeed() * f3 : this.getFlyingSpeed();

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
                        this.getDeltaMovement().y + (0.05D * (Objects.requireNonNull(this.getEffect(MobEffects.LEVITATION)).getAmplifier() + 1) - this.getDeltaMovement().y) * 0.2D,
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
                    this.getDeltaMovement().x * f2,
                    this.getDeltaMovement().y * 0.9800000190734863D,
                    this.getDeltaMovement().z * f2
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

        float f6 = Mth.sqrt((float) (x * x + y * y + z * z)) * 4.0F;

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

    protected void updateParts() {
        this.getBaseBone().setRotation(this.currentPitch, this.getYRot(), 0.0F);
        this.resetBoneAngles();
        this.updatePitchRotations(1.0F);
        this.updateYawRotations(1.0F);
        this.setBodyPartPositions();
    }

    public void updatePitchRotations(float partialTick) {
        for (int i = this.boneList.length - 1; i > 1; --i) {
            if (partialTick == 1.0F) {
                this.boneList[i].getRotation().x += this.targetAngles[i].x = this.targetAngles[i - 1].x;
            } else {
                this.boneList[i].getRotation().x += this.targetAngles[i].x
                        + (this.targetAngles[i - 1].x - this.targetAngles[i].x) * partialTick;
            }
        }

        this.targetAngles[1].x = -(this.currentPitch - this.prevCurrentPitch) * 2.4F;

        float moveScale = this.prevLimbSwingAmount + (this.limbSwingAmount - this.prevLimbSwingAmount) * partialTick;
        float moveTick = this.limbSwing - this.limbSwingAmount * (1.0F - partialTick);

        if (moveScale > 1.0F) {
            moveScale = 1.0F;
        }

        for (int i = 0; i < this.boneList.length; ++i) {
            float breatheAnim = Mth.sin(0.1F * ((float) this.tickCount + partialTick - (float) i * 6.0F));
            float moveAnim = Mth.sin(0.2F * (moveTick - (float) i * 2.0F)) * moveScale;

            this.boneList[i].getRotation().x += breatheAnim * 1.1F;
            this.boneList[i].getRotation().x += moveAnim * 6.0F;

            if (i == 0 && partialTick == 1.0F) {
                Euler angle = new Euler(this.currentPitch + 90.0F, this.getYRot(), 0.0F);
                Vec3 vec = angle.rotateVector(moveAnim * 0.03F);

                this.setDeltaMovement(
                        this.getDeltaMovement().x + vec.x,
                        this.getDeltaMovement().y + vec.y,
                        this.getDeltaMovement().z + vec.z
                );
            }
        }
    }

    public void updateYawRotations(float partialTick) {
        for (int i = this.boneList.length - 1; i > 1; --i) {
            if (partialTick == 1.0F) {
                this.boneList[i].getRotation().y += this.targetAngles[i].y = this.targetAngles[i - 1].y;
            } else {
                this.boneList[i].getRotation().y += this.targetAngles[i].y
                        + (this.targetAngles[i - 1].y - this.targetAngles[i].y) * partialTick;
            }
        }

        this.targetAngles[1].y = -(this.currentYaw - this.prevCurrentYaw) * 2.5F;
        this.targetAngles[0].y = (this.currentYaw - this.prevCurrentYaw) * 1.5F;
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

    @Override
    public void remove(Entity.@NotNull RemovalReason reason) {
        super.remove(reason);

        for (BodyPart bodyPart : this.getParts()) {
            bodyPart.remove(reason);
        }
    }

    @Override
    public boolean checkSpawnRules(@NotNull LevelAccessor level, @NotNull MobSpawnType spawnType) {
        return this.getY() < 48.0D && super.checkSpawnRules(level, spawnType);
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    protected static float wrapAngleAround(float value, float target) {
        return target + Mth.wrapDegrees(value - target);
    }
}