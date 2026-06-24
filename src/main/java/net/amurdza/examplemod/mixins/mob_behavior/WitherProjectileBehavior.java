package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkull.class)
public abstract class WitherProjectileBehavior {
    @Unique
    private Mob aoemod$potentialVictim;

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void aoemod$protectMonsters(EntityHitResult hitResult, CallbackInfo ci) {
        Entity target = hitResult.getEntity();
        if (target instanceof Monster) {
            ci.cancel();
            return;
        }

        WitherSkull skull = (WitherSkull) (Object) this;
        if (target instanceof Mob mob && target.isAlive() && skull.getOwner() instanceof WitherBoss) {
            this.aoemod$potentialVictim = mob;
        }
    }

    @Inject(method = "onHitEntity", at = @At("RETURN"))
    private void aoemod$spawnWitherSkeleton(EntityHitResult hitResult, CallbackInfo ci) {
        Mob victim = this.aoemod$potentialVictim;
        this.aoemod$potentialVictim = null;

        if (victim != null && !victim.isAlive() && victim.level() instanceof ServerLevel level) {
            EntityType.WITHER_SKELETON.spawn(level, victim.blockPosition(), MobSpawnType.TRIGGERED);
        }
    }

    @ModifyArg(
            method = "onHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"
            ),
            index = 6
    )
    private Level.ExplosionInteraction aoemod$preventBlockDamage(Level.ExplosionInteraction interaction) {
        return Level.ExplosionInteraction.NONE;
    }
}
