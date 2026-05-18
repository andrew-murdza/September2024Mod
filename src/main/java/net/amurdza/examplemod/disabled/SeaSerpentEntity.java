//package net.amurdza.examplemod.entity;
//
//import net.amurdza.examplemod.entity.util.Bone;
//import net.amurdza.examplemod.entity.util.Euler;
//import net.minecraft.core.BlockPos;
//import net.minecraft.network.syncher.EntityDataAccessor;
//import net.minecraft.network.syncher.EntityDataSerializers;
//import net.minecraft.network.syncher.SynchedEntityData;
//import net.minecraft.tags.FluidTags;
//import net.minecraft.util.Mth;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.MobType;
//import net.minecraft.world.entity.MoverType;
//import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
//import net.minecraft.world.entity.ai.attributes.Attributes;
//import net.minecraft.world.entity.animal.WaterAnimal;
//import net.minecraft.world.level.ClipContext;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.HitResult;
//import net.minecraft.world.phys.Vec3;
//import org.jetbrains.annotations.NotNull;
//
//public class SeaSerpentEntity extends WaterAnimal {
//    public static final int[] PART_HEIGHT = new int[] {8, 14, 12, 10, 8, 6, 5, 4};
//    public static final int[] PART_LENGTH = new int[] {18, 24, 24, 20, 14, 16, 18, 22};
//
//    private final Bone baseBone = new Bone();
//    private final Bone[] boneList = new Bone[8];
//    private final Euler[] targetAngles = new Euler[8];
//
//    private float currentYaw;
//    private float currentPitch;
//    private float prevCurrentYaw;
//    private float prevCurrentPitch;
//
//    private Vec3 targetVec;
//    private double netSpeed;
//    private int moveTick;
//    private int idleTime;
//    private double dPosX;
//    private double dPosY;
//    private double dPosZ;
//    private float serpentLimbSwing;
//    private float serpentLimbSwingAmount;
//    private float prevSerpentLimbSwingAmount;
//    private final SeaSerpentPart[] partList = new SeaSerpentPart[8];
//
//    private static final EntityDataAccessor<Integer> SYNC_TICKS_EXISTED =
//            SynchedEntityData.defineId(SeaSerpentEntity.class, EntityDataSerializers.INT);
//
//    private static final EntityDataAccessor<Float> SYNC_SERPENT_LIMB_SWING =
//            SynchedEntityData.defineId(SeaSerpentEntity.class, EntityDataSerializers.FLOAT);
//
//    public SeaSerpentEntity(EntityType<? extends WaterAnimal> type, Level level) {
//        super(type, level);
//
//        this.baseBone.setLength(0.0F);
//
//        this.boneList[0] = new Bone(this.baseBone);
//        this.boneList[0].setLength((float) PART_LENGTH[0] / 16.0F);
//
//        for (int i = 1; i < this.boneList.length; i++) {
//            this.boneList[i] = new Bone(i == 1 ? this.baseBone : this.boneList[i - 1]);
//            this.boneList[i].setLength((float) (-PART_LENGTH[i]) / 16.0F);
//        }
//
//        for (int i = 0; i < this.partList.length; i++) {
//            float width = (float) PART_LENGTH[i] / 16.0F;
//            float height = (float) PART_HEIGHT[i] / 16.0F;
//            this.partList[i] = new SeaSerpentPart(this, width, height);
//        }
//
//        for (int i = 0; i < this.targetAngles.length; i++) {
//            this.targetAngles[i] = new Euler();
//        }
//
//        this.setNoGravity(false);
//    }
//
//    @Override
//    public boolean isMultipartEntity() {
//        return true;
//    }
//
//    @Override
//    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
//        return this.partList;
//    }
//
//    public static AttributeSupplier.Builder createAttributes() {
//        return WaterAnimal.createLivingAttributes()
//                .add(Attributes.MAX_HEALTH, 90.0D)
//                .add(Attributes.MOVEMENT_SPEED, 0.3D)
//                .add(Attributes.ATTACK_DAMAGE, 12.0D)
//                .add(Attributes.FOLLOW_RANGE, 48.0D);
//    }
//
//    @Override
//    protected void defineSynchedData() {
//        super.defineSynchedData();
//        this.entityData.define(SYNC_TICKS_EXISTED, 0);
//        this.entityData.define(SYNC_SERPENT_LIMB_SWING, 0.0F);
//    }
//
//    @Override
//    public void tick() {
//        double oldX = this.getX();
//        double oldY = this.getY();
//        double oldZ = this.getZ();
//
//        super.tick();
//
//        if (!this.level().isClientSide) {
//            this.entityData.set(SYNC_TICKS_EXISTED, this.tickCount);
//            this.entityData.set(SYNC_SERPENT_LIMB_SWING, this.serpentLimbSwing);
//        } else {
//            this.tickCount = this.entityData.get(SYNC_TICKS_EXISTED);
//
//            double movementSq = this.dPosX * this.dPosX + this.dPosY * this.dPosY + this.dPosZ * this.dPosZ;
//            if (movementSq < 1.6000001778593287E-5D || this.tickCount % 100 == 0) {
//                this.serpentLimbSwing = this.entityData.get(SYNC_SERPENT_LIMB_SWING);
//            }
//        }
//
//        if (this.targetVec == null) {
//            this.idleTime++;
//        } else {
//            this.idleTime = 0;
//        }
//
//        if (!this.level().isClientSide) {
//            this.moveCreature();
//        }
//
//        this.dPosX = this.getX() - oldX;
//        this.dPosY = this.getY() - oldY;
//        this.dPosZ = this.getZ() - oldZ;
//
//        this.handleSerpentLimbSwing();
//
//        this.prevCurrentYaw = this.currentYaw;
//        this.prevCurrentPitch = this.currentPitch;
//
//        this.currentYaw = wrapAngleAround(this.currentYaw, this.getYRot());
//        this.currentPitch = wrapAngleAround(this.currentPitch, this.getXRot());
//
//        this.prevCurrentYaw = wrapAngleAround(this.prevCurrentYaw, this.currentYaw);
//        this.prevCurrentPitch = wrapAngleAround(this.prevCurrentPitch, this.currentPitch);
//
//        this.currentYaw += (this.getYRot() - this.currentYaw) * 0.6F;
//
//        if (this.isInWater()) {
//            this.currentPitch += (this.getXRot() - this.currentPitch) * 0.1F;
//        } else if (this.onGround()) {
//            this.currentPitch *= 0.6F;
//        }
//
//        this.updateParts();
//        this.pushEntitiesFromParts();
//    }
//
//    public void setupAnimationBones(float partialTick) {
//        this.resetBoneAngles();
//        this.updatePitchRotations(partialTick);
//        this.updateYawRotations(partialTick);
//    }
//
//    private void updateBones(float partialTick) {
//        this.baseBone.setRotation(this.currentPitch, this.getYRot(), 0.0F);
//        this.resetBoneAngles();
//        this.updatePitchRotations(partialTick);
//        this.updateYawRotations(partialTick);
//    }
//
//    public void resetBoneAngles() {
//        for (Bone bone : this.boneList) {
//            bone.setRotation(0.0F, 0.0F, 0.0F);
//        }
//    }
//
//    private void updateParts() {
//        this.baseBone.setRotation(this.currentPitch, this.getYRot(), 0.0F);
//        this.resetBoneAngles();
//        this.updatePitchRotations(1.0F);
//        this.updateYawRotations(1.0F);
//        this.setBodyPartPositions();
//    }
//
//    private void setBodyPartPositions() {
//        Vec3 vec = this.boneList[0].getRotation()
//                .getRotated(this.baseBone.getRotation())
//                .rotateVector(this.boneList[0].getLength() / 2.0F);
//
//        this.partList[0].setPos(this.getX() + vec.x, this.getY() + vec.y, this.getZ() + vec.z);
//
//        vec = this.baseBone.getRotatedVector();
//        Euler angle = this.baseBone.getRotation();
//
//        for (int i = 1; i < this.partList.length; i++) {
//            angle = angle.getRotated(this.boneList[i].getRotation());
//
//            float length = this.boneList[i].getLength();
//            Vec3 midVec = angle.rotateVector(length / 2.0F);
//
//            this.partList[i].setPos(
//                    this.getX() + vec.x + midVec.x,
//                    this.getY() + vec.y + midVec.y,
//                    this.getZ() + vec.z + midVec.z
//            );
//
//            Vec3 target = angle.rotateVector(length);
//            vec = vec.add(target.x, target.y, target.z);
//        }
//    }
//
//    public void updatePitchRotations(float partialTick) {
//        for (int i = this.boneList.length - 1; i > 1; i--) {
//            if (partialTick == 1.0F) {
//                this.boneList[i].getRotation().x += this.targetAngles[i].x = this.targetAngles[i - 1].x;
//            } else {
//                this.boneList[i].getRotation().x += this.targetAngles[i].x
//                        + (this.targetAngles[i - 1].x - this.targetAngles[i].x) * partialTick;
//            }
//        }
//
//        this.targetAngles[1].x = -(this.currentPitch - this.prevCurrentPitch) * 2.4F;
//
//        float moveScale = this.prevSerpentLimbSwingAmount
//                + (this.serpentLimbSwingAmount - this.prevSerpentLimbSwingAmount) * partialTick;
//
//        float moveTick = this.serpentLimbSwing
//                - this.serpentLimbSwingAmount * (1.0F - partialTick);
//
//        if (moveScale > 1.0F) {
//            moveScale = 1.0F;
//        }
//
//        for (int i = 0; i < this.boneList.length; i++) {
//            float breatheAnim = Mth.sin(0.1F * ((float) this.tickCount + partialTick - (float) i * 6.0F));
//            float moveAnim = Mth.sin(0.2F * (moveTick - (float) i * 2.0F)) * moveScale;
//
//            this.boneList[i].getRotation().x += breatheAnim * 1.1F;
//            this.boneList[i].getRotation().x += moveAnim * 6.0F;
//        }
//    }
//
//    public void updateYawRotations(float partialTick) {
//        for (int i = this.boneList.length - 1; i > 1; i--) {
//            if (partialTick == 1.0F) {
//                this.boneList[i].getRotation().y += this.targetAngles[i].y = this.targetAngles[i - 1].y;
//            } else {
//                this.boneList[i].getRotation().y += this.targetAngles[i].y
//                        + (this.targetAngles[i - 1].y - this.targetAngles[i].y) * partialTick;
//            }
//        }
//
//        float yawDelta = this.currentYaw - this.prevCurrentYaw;
//
//        this.targetAngles[1].y = -yawDelta * 3.5F;
//        this.targetAngles[0].y = yawDelta * 2.0F;
//    }
//
//    @Override
//    public void travel(@NotNull Vec3 travelVector) {
//        if (this.isInWater()) {
//            double oldY = this.getY();
//
//            this.moveRelative(0.04F, travelVector);
//            this.move(MoverType.SELF, this.getDeltaMovement());
//
//            Vec3 motion = this.getDeltaMovement();
//            this.setDeltaMovement(
//                    motion.x * 0.84D,
//                    motion.y * 0.84D,
//                    motion.z * 0.84D
//            );
//
//            if (this.horizontalCollision && this.isFree(
//                    motion.x,
//                    motion.y + 0.6000000238418579D - this.getY() + oldY,
//                    motion.z
//            )) {
//                this.setDeltaMovement(this.getDeltaMovement().x, 0.30000001192092896D, this.getDeltaMovement().z);
//            }
//        } else {
//            super.travel(travelVector);
//        }
//
//        this.handleSerpentLimbSwing();
//    }
//
//    @Override
//    public @NotNull MobType getMobType() {
//        return MobType.WATER;
//    }
//
//    public Bone getBaseBone() {
//        return this.baseBone;
//    }
//
//    public Bone[] getBoneList() {
//        return this.boneList;
//    }
//
//    public float getCurrentPitch(float partialTicks) {
//        return this.prevCurrentPitch + (this.currentPitch - this.prevCurrentPitch) * partialTicks;
//    }
//
//    private static float wrapAngleAround(float angle, float target) {
//        while (target - angle >= 180.0F) {
//            angle += 360.0F;
//        }
//
//        while (target - angle < -180.0F) {
//            angle -= 360.0F;
//        }
//
//        return angle;
//    }
//
//    private void moveCreature() {
//        double targetSpeed = 0.0D;
//
//        if (this.targetVec != null) {
//            ++this.moveTick;
//            targetSpeed = this.moveByPathing();
//        } else if (this.idleTime < 100 && this.findNewPath() && this.tickCount > 20) {
//            this.setRandomPath();
//        }
//
//        Vec3 vec = this.getLookAngle();
//
//        this.netSpeed += (targetSpeed - this.netSpeed) * 0.1D;
//
//        float dYaw = Math.abs(this.currentYaw - this.prevCurrentYaw);
//        if (dYaw > 0.02F && this.netSpeed > targetSpeed * 0.6D) {
//            this.netSpeed += (targetSpeed * 0.6D - this.netSpeed) * 0.4D;
//        }
//
//        if (!this.isInWater()) {
//            this.netSpeed = 0.0D;
//        }
//
//        this.move(MoverType.SELF, new Vec3(vec.x * this.netSpeed, vec.y * this.netSpeed, vec.z * this.netSpeed));
//    }
//
//    private double moveByPathing() {
//        this.getLookControl().setLookAt(this.targetVec.x, this.targetVec.y, this.targetVec.z, 3.0F, 85.0F);
//
//        if (this.horizontalCollision && this.getDeltaMovement().y < 0.2D) {
//            this.setDeltaMovement(this.getDeltaMovement().x, 0.2D, this.getDeltaMovement().z);
//        }
//
//        if (this.targetVec.distanceTo(this.position()) < 3.0D || this.moveTick > 120) {
//            this.moveTick = 0;
//
//            if (this.random.nextInt(5) < 3) {
//                this.setRandomPath();
//            } else {
//                this.targetVec = null;
//            }
//        }
//
//        return this.getMovementSpeedValue();
//    }
//
//    private double getMovementSpeedValue() {
//        return this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.8D;
//    }
//
//    private boolean findNewPath() {
//        return this.random.nextInt(40) == 0 || (!this.isInWater() && this.onGround() && this.random.nextInt(4) == 0);
//    }
//
//    private void setRandomPath() {
//        for (int tries = 0; tries < 8; tries++) {
//            double x = this.getX() + (6.0D + this.random.nextFloat() * 12.0D) * (this.random.nextBoolean() ? 1.0D : -1.0D);
//            double y = this.getY() + this.random.nextFloat() * 6.0D - 2.0D;
//            double z = this.getZ() + (6.0D + this.random.nextFloat() * 12.0D) * (this.random.nextBoolean() ? 1.0D : -1.0D);
//
//            if (this.isClearPath(x, y, z)) {
//                this.targetVec = new Vec3(x, y, z);
//                return;
//            }
//        }
//
//    }
//
//
//    private void handleSerpentLimbSwing() {
//        this.prevSerpentLimbSwingAmount = this.serpentLimbSwingAmount;
//
//        float movement = Mth.sqrt((float)(this.dPosX * this.dPosX + this.dPosY * this.dPosY + this.dPosZ * this.dPosZ)) * 4.0F;
//
//        if (movement > 1.0F) {
//            movement = 1.0F;
//        }
//
//        if (!this.isInWater() && !this.onGround()) {
//            this.serpentLimbSwingAmount *= 0.4F;
//        } else {
//            float delta = movement - this.serpentLimbSwingAmount;
//
//            if (delta >= 0.0F) {
//                this.serpentLimbSwingAmount += delta * 0.4F;
//            } else {
//                this.serpentLimbSwingAmount += delta * 0.1F;
//            }
//        }
//
//        this.serpentLimbSwing += this.serpentLimbSwingAmount;
//    }
//
//    private boolean isClearPath(double x, double y, double z) {
//        BlockPos pos = BlockPos.containing(x, y, z);
//
//        boolean water = this.level().getBlockState(pos).getFluidState().is(FluidTags.WATER);
//
//        boolean seen = this.level().clip(new ClipContext(
//                this.getEyePosition(),
//                new Vec3(x, y, z),
//                ClipContext.Block.COLLIDER,
//                ClipContext.Fluid.NONE,
//                this
//        )).getType() == HitResult.Type.MISS;
//
//        return water && seen;
//    }
//
//    private void pushEntitiesFromParts() {
//        if (this.level().isClientSide) {
//            return;
//        }
//
//        for (SeaSerpentPart part : this.partList) {
//            for (Entity entity : this.level().getEntities(this, part.getBoundingBox(), e -> e.isPushable() && e.isAlive())) {
//                if (entity == this || entity instanceof SeaSerpentPart) {
//                    continue;
//                }
//
//                Vec3 away = entity.position().subtract(part.position());
//
//                if (away.lengthSqr() < 0.0001D) {
//                    away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
//                }
//
//                away = away.normalize().scale(0.08D);
//                entity.push(away.x, Math.max(away.y, 0.01D), away.z);
//                entity.hurtMarked = true;
//            }
//        }
//    }
//}