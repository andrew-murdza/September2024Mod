package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Explosion.class)
public abstract class WitherProjectileExplosionBehavior {
    @Shadow
    @Final
    @Nullable
    private Entity source;

    @Redirect(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
            )
    )
    private List<Entity> aoemod$excludeMonsters(Level level, Entity excludedEntity, AABB bounds) {
        List<Entity> affectedEntities = level.getEntities(excludedEntity, bounds);
        if (this.source instanceof WitherSkull) {
            affectedEntities.removeIf(entity -> entity instanceof Monster);
        }
        return affectedEntities;
    }
}
