package net.amurdza.examplemod.entity.sea_serpent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;

public class BodyPart extends PartEntity<SeaSerpentEntity> {
    private int inWaterTick;
    private boolean wasInWater;

    private final EntityDimensions size;

    public BodyPart(SeaSerpentEntity parent, float size) {
        super(parent);
        this.size = EntityDimensions.scalable(size, size);
        this.blocksBuilding = true;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.size;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {

    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return this.getParent().getPickedResult(target);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    public void tick() {
        boolean wasInWater = this.level()
                .getFluidState(BlockPos.containing((int)this.getX(), (int)this.getBoundingBox().minY, (int)this.getZ()))
                .is(FluidTags.WATER);

        super.tick();

        this.collideWithNearbyEntities();
        --this.inWaterTick;

        if (this.level()
                .getFluidState(BlockPos.containing((int)this.getX(), (int)this.getBoundingBox().minY, (int)this.getZ()))
                .is(FluidTags.WATER)) {

            this.inWaterTick = 20;

            if (!this.wasInWater) {
                for (int i = 0; i < 12; ++i) {
                    float x = (this.getBbWidth() * 0.4F + this.random.nextFloat() * 0.6F) * (float)(this.random.nextBoolean() ? 1 : -1);
                    float z = (this.getBbWidth() * 0.4F + this.random.nextFloat() * 0.6F) * (float)(this.random.nextBoolean() ? 1 : -1);

                    this.level().addParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WATER.defaultBlockState()),
                            this.getX() + (double)x,
                            this.getY() + 0.5D + 0.5D * (double)this.getBbWidth() * (double)this.random.nextFloat(),
                            this.getZ() + (double)z,
                            x * 0.1F,
                            0.1F + 0.3F * this.getBbWidth() * this.random.nextFloat(),
                            z * 0.1F
                    );
                }
            }
        } else if (this.inWaterTick > 0) {
            for (int i = 0; i < 4; ++i) {
                float x = this.getBbWidth() * 1.2F * (this.random.nextFloat() - 0.5F);
                float y = this.getBbWidth() * 0.4F * this.random.nextFloat();
                float z = this.getBbWidth() * 1.2F * (this.random.nextFloat() - 0.5F);

                this.level().addParticle(
                        ParticleTypes.SPLASH,
                        this.getX() + (double)x,
                        this.getBoundingBox().minY + (double)y,
                        this.getZ() + (double)z,
                        0.0D,
                        0.0D,
                        0.0D
                );
            }
        }

        this.wasInWater = wasInWater;
    }

    private void collideWithNearbyEntities() {
        AABB box = this.getBoundingBox().inflate(0.2D, 0.0D, 0.2D);

        for (Entity entity : this.level().getEntities(this, box, EntitySelector.NO_SPECTATORS)) {
            if (entity.isPickable() && !this.is(entity)) {
                if (entity.isPushable()) {
                    entity.push(this);
                }

                this.getParent().collideWithEntity(this, entity);
            }
        }
    }

    @Override
    public boolean is(@NotNull Entity entity) {
        return super.is(entity)
                || entity instanceof BodyPart bodyPart && bodyPart.getParent() == this.getParent();
    }
}
