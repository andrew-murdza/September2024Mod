package net.amurdza.examplemod.entity.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * GeckoLib animatable used to replace only the rendered model of a vanilla baby entity.
 * The entity itself remains vanilla, so its AI, data, sounds, and interactions are unchanged.
 */
public final class BabyReplacedEntity<E extends Entity> implements GeoReplacedEntity {
    private final EntityType<? extends E> replacingEntityType;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private E currentEntity;

    public BabyReplacedEntity(EntityType<? extends E> replacingEntityType) {
        this.replacingEntityType = replacingEntityType;
    }

    @Override
    public EntityType<?> getReplacingEntityType() {
        return this.replacingEntityType;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The supplied baby models do not include animation files. Their movement is
        // applied procedurally by BabyDolphinModel and BabyParrotModel.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    public void setCurrentEntity(E entity) {
        this.currentEntity = entity;
    }

    @Nullable
    public E getCurrentEntity() {
        return this.currentEntity;
    }
}
