package net.amurdza.examplemod.mixins.add_ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Parrot.class)
public abstract class BreedableParrots extends ShoulderRidingEntity {
    protected BreedableParrots(EntityType<? extends ShoulderRidingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(@NotNull ServerLevel world, @NotNull AgeableMob otherParent) {
        Parrot parrot = EntityType.PARROT.create(world);
        if (parrot == null) {
            return null;
        }

        parrot.setVariant(Parrot.Variant.byId(random.nextInt(5)));
        Parrot parent = (Parrot) (Object) this;
        if (parent.isTame()) {
            parrot.setOwnerUUID(parent.getOwnerUUID());
            parrot.setTame(true);
        }
        return parrot;
    }
    @Override
    public boolean canMate(@NotNull Animal other){
        return other!=this&&other.getClass()==Parrot.class&&isInLove()&&other.isInLove();
    }

    @Inject(method = "isBaby", at = @At("HEAD"), cancellable = true)
    private void aoe$useInheritedBabyAge(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(this.getAge() < 0);
    }

    @Override
    public boolean canSitOnShoulder() {
        return this.getAge() >= 0 && super.canSitOnShoulder();
    }
}
