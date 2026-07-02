package net.amurdza.examplemod.entity.future;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BreezeWindChargeEntity extends AbstractHurtingProjectile {
    public BreezeWindChargeEntity(EntityType<? extends BreezeWindChargeEntity> type, Level level) {
        super(type, level);
    }

    public BreezeWindChargeEntity(
            EntityType<? extends BreezeWindChargeEntity> type,
            LivingEntity owner,
            double xPower,
            double yPower,
            double zPower,
            Level level
    ) {
        super(type, owner, xPower, yPower, zPower, level);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.CLOUD;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        result.getEntity().hurt(this.damageSources().mobProjectile(this, livingOwner), 4.0F);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        AABB area = this.getBoundingBox().inflate(4.0D);
        for (Entity entity : level.getEntities(this, area, Entity::isAlive)) {
            Vec3 push = entity.position().subtract(this.position());
            double distance = Math.max(0.5D, push.length());
            double strength = Math.max(0.0D, 1.5D - distance * 0.2D);
            entity.push(push.x / distance * strength, 0.35D, push.z / distance * strength);
            entity.hurtMarked = true;
        }
        level.explode(this, this.getX(), this.getY(), this.getZ(), 1.2F, Level.ExplosionInteraction.NONE);
        this.discard();
    }
}
