package net.amurdza.examplemod.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Parrot.class)
public abstract class BreedableParrots extends Animal {
    protected BreedableParrots(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel world, @NotNull AgeableMob otherParent) {
        Parrot parrot= EntityType.PARROT.create(world);
        assert parrot != null;
        parrot.setVariant(Parrot.Variant.byId(random.nextInt(5)));
        return  parrot;
    }
    @Override
    public boolean canMate(@NotNull Animal other){
        return other!=this&&other.getClass()==Parrot.class&&isInLove()&&other.isInLove();
    }
}
